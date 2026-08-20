package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.constants.enums.OrderStatus;
import guru.springframework.ghd.constants.enums.PaymentEnum;
import guru.springframework.ghd.dto.order.*;
import guru.springframework.ghd.dto.payment.PaymentStatusResponse;
import guru.springframework.ghd.entities.OrderDetail;
import guru.springframework.ghd.entities.Orders;
import guru.springframework.ghd.events.OrderCreatedEvent;
import guru.springframework.ghd.mappers.OrderMapper;
import guru.springframework.ghd.repositories.OrderDetailRepository;
import guru.springframework.ghd.repositories.OrdersRepository;
import guru.springframework.ghd.repositories.PaymentRepository;
import guru.springframework.ghd.repositories.ProductRepository;
import guru.springframework.ghd.services.OrdersService;
import guru.springframework.ghd.services.PaymentService;
import guru.springframework.ghd.services.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static guru.springframework.ghd.config.KafkaTopicConfig.ORDER_EVENTS_TOPIC;

@Service
@RequiredArgsConstructor
public class OrdersServiceImpl implements OrdersService {

    private final OrdersRepository ordersRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StockService stockService;
    private final PaymentService paymentService;

    // "thuoc-tinh" từ client hiện chỉ có 1 giá trị khả dĩ (created_date, xem
    // admin/order.html DEFAULTS.SORT_FIELD) - Sort.by() cần đúng tên property Java của
    // entity Orders (createdDate), không phải tên cột DB. Map an toàn qua allowlist,
    // mặc định về createdDate cho giá trị lạ thay vì để Hibernate ném lỗi property
    // không tồn tại.
    private static final Map<String, String> ORDER_SORT_PROPERTIES = Map.of(
            DefaultPage.CREATED_DATE, "createdDate"
    );

    @Override
    @Transactional
    public OrderCreationResult createOrder(OrderRequest request, String clientIp) {
        PaymentEnum paymentMethod = parsePaymentMethod(request.getPaymentMethod());
        boolean onlineGateway = paymentMethod == PaymentEnum.CARD || paymentMethod == PaymentEnum.INSTALLMENT;

        Orders order = new Orders();
        order.setCustomerName(request.getCustName());
        order.setCustomerPhone(request.getCustPhone());
        order.setCustomerEmail(request.getCustEmail());
        order.setShippingAddress(request.getCustAddress());
        order.setNote(request.getOrderNote());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setTotalAmount(request.getTotalAmount());
        // CARD/INSTALLMENT: chờ IPN xác nhận, CHƯA trừ kho (xem StockService/PaymentServiceImpl).
        // COD/BANK_TRANSFER: giữ nguyên hành vi cũ - trừ kho ngay bên dưới.
        order.setStatus(onlineGateway ? OrderStatus.AWAITING_PAYMENT : OrderStatus.PENDING);

        Orders savedOrder = ordersRepository.save(order);

        List<OrderDetail> orderDetails = request.getItems().stream().map(itemReq -> {
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(savedOrder.getId().toString());
            detail.setProductId(itemReq.getProductId());
            detail.setQuantity(itemReq.getQuantity());
            detail.setPrice(itemReq.getPrice());
            return detail;
        }).toList();

        if (onlineGateway) {
            // Chỉ xác nhận sản phẩm tồn tại - KHÔNG khoá, KHÔNG trừ kho ở bước này
            // (tồn kho chỉ bị trừ sau khi VNPay xác nhận thanh toán thành công qua IPN).
            for (OrderDetail detail : orderDetails) {
                productRepository.findById(detail.getProductId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + detail.getProductId()));
            }
            orderDetailRepository.saveAll(orderDetails);

            String paymentUrl = paymentService.initiatePayment(savedOrder, paymentMethod, clientIp);
            return new OrderCreationResult(savedOrder.getId().toString(), paymentUrl);
        }

        orderDetailRepository.saveAll(orderDetails);
        List<OrderCreatedEvent.OrderItemInfo> eventItems =
                stockService.decrementStockForOrder(savedOrder.getId().toString());

        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId().toString(),
                request.getCustName(),
                request.getCustPhone(),
                request.getCustEmail(),
                request.getCustAddress(),
                request.getPaymentMethod(),
                request.getTotalAmount(),
                request.getOrderNote(),
                eventItems
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send(ORDER_EVENTS_TOPIC, savedOrder.getId().toString(), event);
            }
        });

        return new OrderCreationResult(savedOrder.getId().toString(), null);
    }

    private PaymentEnum parsePaymentMethod(String rawPaymentMethod) {
        try {
            return PaymentEnum.valueOf(rawPaymentMethod);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ: " + rawPaymentMethod);
        }
    }
    @Override
    public OrderDetailResponse findByOrderId(String orderId) {
        Orders order = ordersRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        OrderDetailResponse res = orderMapper.toOrderDetailResponse(order);
        // Get list product
        List<OrderDetailProjection> items = orderDetailRepository.findByOrderId(orderId);
        res.setOrderItems(items);
        // paymentStatus không có trên entity Orders nên OrderMapper không tự map được -
        // null cho COD/BANK_TRANSFER (không có Payment), set thủ công ở đây.
        PaymentStatusResponse paymentStatus = paymentService.getStatus(orderId);
        if (paymentStatus != null && paymentStatus.getPaymentStatus() != null) {
            res.setPaymentStatus(paymentStatus.getPaymentStatus().name());
        }
        return res;
    }

    @Transactional
    @Override
    public void updateOrderStatus(String orderId, OrderUpdateRequest request) {
        Orders order = ordersRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }
        if (request.getAdminNote() != null) {
            order.setAdminNote(request.getAdminNote());
        }
        ordersRepository.save(order);
    }

    @Override
    public Page<OrderResponse> searchOrders(String orderId, String customerName, String phone,
                                            OrderStatus status, LocalDate startDate, LocalDate endDate,
                                            String sortField, String sortDir, int pageNumber, int sizeNumber) {
        int pageIndex = pageNumber > DefaultPage.PAGE ? pageNumber - 1 : DefaultPage.PAGE;
        Pageable pageable = PageRequest.of(pageIndex, sizeNumber, resolveSort(sortField, sortDir));
        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        Page<Orders> orderPage = ordersRepository.search(
                parseOrderId(orderId), customerName, phone, status, start, end, pageable);

        return orderPage.map(order -> {
            OrderResponse response = orderMapper.toResponse(order);
            // paymentStatus không có trên entity Orders nên OrderMapper không tự map
            // được - null cho COD/BANK_TRANSFER (không có Payment), giống findByOrderId.
            response.setPaymentStatus(latestPaymentStatus(order.getId().toString()));
            return response;
        });
    }

    private Sort resolveSort(String sortField, String sortDir) {
        String property = ORDER_SORT_PROPERTIES.getOrDefault(sortField, "createdDate");
        return DefaultPage.ASC.equalsIgnoreCase(sortDir) ? Sort.by(property).ascending() : Sort.by(property).descending();
    }

    private UUID parseOrderId(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return null;
        }
        try {
            return UUID.fromString(orderId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String latestPaymentStatus(String orderId) {
        return paymentRepository.findFirstByOrderIdOrderByCreatedDateDesc(orderId)
                .map(payment -> payment.getStatus().name())
                .orElse(null);
    }
}