package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.constants.enums.OrderStatus;
import guru.springframework.ghd.dto.order.*;
import guru.springframework.ghd.entities.OrderDetail;
import guru.springframework.ghd.entities.Orders;
import guru.springframework.ghd.entities.Product;
import guru.springframework.ghd.events.OrderCreatedEvent;
import guru.springframework.ghd.mappers.OrderMapper;
import guru.springframework.ghd.repositories.OrderDetailRepository;
import guru.springframework.ghd.repositories.OrdersRepository;
import guru.springframework.ghd.repositories.ProductRepository;
import guru.springframework.ghd.services.OrdersService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static guru.springframework.ghd.config.KafkaTopicConfig.ORDER_EVENTS_TOPIC;

@Service
@RequiredArgsConstructor
public class OrdersServiceImpl implements OrdersService {

    private final OrdersRepository ordersRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public void createOrder(OrderRequest request) {
        Orders order = new Orders();
        order.setCustomerName(request.getCustName());
        order.setCustomerPhone(request.getCustPhone());
        order.setCustomerEmail(request.getCustEmail());
        order.setShippingAddress(request.getCustAddress());
        order.setNote(request.getOrderNote());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setTotalAmount(request.getTotalAmount());
        order.setStatus(OrderStatus.PENDING);

        Orders savedOrder = ordersRepository.save(order);

        List<OrderDetail> orderDetails = new ArrayList<>();
        List<OrderCreatedEvent.OrderItemInfo> eventItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findByIdForUpdate(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + itemReq.getProductId()));

            if (product.getStockQty() < itemReq.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + product.getTitle() + " không đủ hàng trong kho!");
            }

            OrderDetail detail = new OrderDetail();
            detail.setOrderId(savedOrder.getId().toString());
            detail.setQuantity(itemReq.getQuantity());
            detail.setPrice(itemReq.getPrice());
            detail.setProductId(product.getId());
            orderDetails.add(detail);

            product.setStockQty(product.getStockQty() - itemReq.getQuantity());

            Integer soldCount = product.getSoldCount() == null ? 0 : product.getSoldCount();
            product.setSoldCount(soldCount + itemReq.getQuantity());

            productRepository.save(product);

            eventItems.add(new OrderCreatedEvent.OrderItemInfo(product.getTitle(), itemReq.getQuantity(), itemReq.getPrice()));
        }

        orderDetailRepository.saveAll(orderDetails);

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
    }
    @Override
    public OrderDetailResponse findByOrderId(String orderId) {
        Orders order = ordersRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        OrderDetailResponse res = orderMapper.toOrderDetailResponse(order);
        // Get list product
        List<OrderDetailProjection> items = orderDetailRepository.findByOrderId(orderId);
        res.setOrderItems(items);
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
        String dbSortField = sortField.equals(DefaultPage.CREATED_DATE) ? DefaultPage.CREATED_DATE : sortField;
        Sort sort = sortDir.equalsIgnoreCase(DefaultPage.ASC) ? Sort.by(dbSortField).ascending() : Sort.by(dbSortField).descending();
        Pageable pageable = PageRequest.of(pageIndex, sizeNumber, sort);
        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;
        String statusStr = (status != null) ? status.name() : null;
        Page<OrderProjection> projectionPage = ordersRepository.findAllNative(
                customerName, phone, statusStr, start, end, pageable);
        return projectionPage.map(p -> {
            return OrderResponse.builder()
                    .id(p.getId())
                    .customerName(p.getCustomerName())
                    .customerPhone(p.getCustomerPhone())
                    .totalAmount(p.getTotalAmount())
                    .paymentMethod(p.getPaymentMethod())
                    .status(p.getStatus())
                    .createdDate(p.getCreatedDate())
                    .shippingAddress(p.getShippingAddress())
                    .note(p.getNote())
                    .build();
        });
    }
}