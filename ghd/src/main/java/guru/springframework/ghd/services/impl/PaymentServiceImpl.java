package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.constants.enums.OrderStatus;
import guru.springframework.ghd.constants.enums.PaymentEnum;
import guru.springframework.ghd.constants.enums.PaymentStatus;
import guru.springframework.ghd.dto.order.OrderCreationResult;
import guru.springframework.ghd.dto.payment.PaymentStatusResponse;
import guru.springframework.ghd.dto.payment.VnpayIpnResponse;
import guru.springframework.ghd.entities.Orders;
import guru.springframework.ghd.entities.Payment;
import guru.springframework.ghd.events.OrderCreatedEvent;
import guru.springframework.ghd.exceptions.InsufficientStockException;
import guru.springframework.ghd.repositories.OrdersRepository;
import guru.springframework.ghd.repositories.PaymentRepository;
import guru.springframework.ghd.services.PaymentService;
import guru.springframework.ghd.services.StockService;
import guru.springframework.ghd.services.VnpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static guru.springframework.ghd.config.KafkaTopicConfig.ORDER_EVENTS_TOPIC;

// PaymentServiceImpl KHÔNG phụ thuộc OrdersService (interface) để tránh circular bean
// dependency (OrdersServiceImpl -> PaymentService -> OrdersService -> ... ). Thay vào đó
// dùng thẳng OrdersRepository (Service -> Repository luôn hợp lệ theo layering rules) và
// StockService (thấp hơn, không phụ thuộc ngược lại Payment/Orders service nào).
//
// Mọi tên field VNPay cụ thể dưới đây (vnp_TxnRef, vnp_Amount x100, vnp_ResponseCode,
// vnp_TransactionNo, vnp_BankCode, mã "00"...) là convention PHỔ BIẾN của VNPay - CẦN
// XÁC NHẬN LẠI với tài liệu merchant thật khi có sandbox, xem
// .claude/skills/vnpay-payment/SKILL.md.
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String VNP_SUCCESS_CODE = "00";

    private final PaymentRepository paymentRepository;
    private final OrdersRepository ordersRepository;
    private final StockService stockService;
    private final VnpayService vnpayService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public String initiatePayment(Orders order, PaymentEnum paymentMethod, String clientIp) {
        // Bất biến bắt buộc: tối đa 1 Payment PENDING cho 1 order tại 1 thời điểm - nếu
        // không, job cancelExpiredAwaitingPayments/handleIpn có thể xử lý nhầm payment
        // cũ đã bị "mồ côi" (retryPayment gọi lại method này trên đơn đang
        // AWAITING_PAYMENT sẽ luôn còn 1 payment PENDING trước đó nếu không dọn ở đây).
        cancelAnyExistingPendingPayments(order.getId().toString());

        String txnRef = UUID.randomUUID().toString().replace("-", "");

        Payment payment = Payment.builder()
                .orderId(order.getId().toString())
                .amount(order.getTotalAmount())
                .paymentMethod(paymentMethod.name())
                .txnRef(txnRef)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        String paymentUrl = vnpayService.buildPaymentUrl(payment, clientIp);
        // buildPaymentUrl set payment.vnpCreateDate (cần cho Query API đối soát sau này,
        // xem VnpayServiceImpl.queryTransaction) - save lại để persist field này.
        paymentRepository.save(payment);

        return paymentUrl;
    }

    // Khoá Payment MỚI NHẤT của order này (cùng row/cơ chế/THỨ TỰ mà handleIpn khoá:
    // Payment trước, Orders sau nếu có) - CỐ Ý dùng bản khoá thay vì đọc "tất cả PENDING"
    // không khoá: nếu 2 lần gọi initiatePayment/retryPayment chạy đồng thời cho CÙNG 1
    // order, lần chạy sau phải đợi lần chạy trước commit rồi mới thấy (và huỷ) đúng
    // payment PENDING mà lần trước vừa tạo, thay vì cả 2 cùng tạo payment PENDING song
    // song (bug đã bị QA subagent phát hiện). Giữ nguyên thứ tự khoá Payment-trước ở
    // MỌI nơi trong class này để không bao giờ đảo ngược thứ tự khoá với handleIpn -
    // đảo ngược thứ tự khoá giữa 2 transaction là nguyên nhân kinh điển gây deadlock.
    //
    // CHỈ xử lý payment MỚI NHẤT (không phải "tất cả PENDING"): đủ vì payment chỉ được
    // tạo ở duy nhất 1 chỗ (Payment.builder() trong initiatePayment, ngay dưới), và
    // method này luôn được gọi TRƯỚC khi tạo payment mới - nên tại mọi thời điểm hợp lệ,
    // nếu có payment PENDING thì đó luôn là payment mới nhất. Nếu sau này có thêm chỗ
    // khác tạo Payment mà không đi qua initiatePayment, bất biến này cần được xem lại.
    private void cancelAnyExistingPendingPayments(String orderId) {
        Payment latest = paymentRepository.findFirstByOrderIdForUpdateOrderByCreatedDateDesc(orderId).orElse(null);
        if (latest != null && latest.getStatus() == PaymentStatus.PENDING) {
            latest.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(latest);
        }
    }

    @Override
    @Transactional
    public VnpayIpnResponse handleIpn(Map<String, String> params) {
        if (!vnpayService.verifySignature(params)) {
            log.warn("VNPay IPN: chữ ký không hợp lệ, bỏ qua. params={}", params);
            return VnpayIpnResponse.invalidSignature();
        }

        return applyGatewayResult(params.get("vnp_TxnRef"), params);
    }

    @Override
    @Transactional
    public VnpayIpnResponse applyGatewayResult(String txnRef, Map<String, String> params) {
        // typical convention - xác nhận lại tên field thật với tài liệu VNPay
        Payment payment = txnRef == null ? null : paymentRepository.findByTxnRefForUpdate(txnRef).orElse(null);
        if (payment == null) {
            log.warn("VNPay IPN: không tìm thấy payment cho txnRef={}", txnRef);
            return VnpayIpnResponse.orderNotFound();
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            // Idempotency: IPN đã được xử lý trước đó (VNPay được biết là có thể gọi lại
            // nhiều lần cho cùng giao dịch) - không lặp lại bất kỳ side effect nào.
            //
            // NHƯNG: nếu payment đã CANCELLED (do quá hạn hoặc bị 1 lần retryPayment
            // thay thế) hoặc FAILED, mà IPN lần này lại báo THÀNH CÔNG, đây KHÔNG phải
            // trùng lặp bình thường - khả năng khách đã bị VNPay thu tiền cho 1 link đã
            // bị GHD coi là "xong" (vd bấm link cũ sau khi đã retry, hoặc thanh toán
            // đúng lúc job tự huỷ chạy). Không tự động xử lý (ngoài phạm vi Phase 2,
            // giống quyết định không tự động hoàn tiền) - chỉ log ERROR + ghi chú vào
            // đơn để admin đối soát thủ công, KHÔNG được âm thầm bỏ qua như finding của
            // QA subagent đã chỉ ra.
            boolean gatewayReportsSuccess = VNP_SUCCESS_CODE.equals(params.get("vnp_ResponseCode"));
            if (payment.getStatus() != PaymentStatus.SUCCESS && gatewayReportsSuccess) {
                log.error("VNPay IPN: txnRef={} (orderId={}) báo THÀNH CÔNG nhưng payment đã ở trạng thái {} từ trước - "
                                + "CẦN ĐỐI SOÁT THỦ CÔNG, khả năng khách đã bị thu tiền ngoài dự kiến.",
                        txnRef, payment.getOrderId(), payment.getStatus());
                flagOrderForManualReconciliation(payment.getOrderId(), txnRef, payment.getStatus());
            } else {
                log.info("VNPay IPN: txnRef={} đã được xử lý trước đó (status={}), bỏ qua.", txnRef, payment.getStatus());
            }
            return VnpayIpnResponse.ok();
        }

        Orders order = ordersRepository.findByOrderId(payment.getOrderId()).orElse(null);
        if (order == null) {
            log.error("VNPay IPN: payment txnRef={} trỏ tới orderId={} không tồn tại.", txnRef, payment.getOrderId());
            return VnpayIpnResponse.orderNotFound();
        }

        payment.setResponseCode(params.get("vnp_ResponseCode"));
        payment.setGatewayTransactionId(params.get("vnp_TransactionNo"));
        payment.setBankCode(params.get("vnp_BankCode"));
        payment.setIpnReceivedAt(LocalDateTime.now());
        payment.setRawIpnPayload(params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&")));

        if (!isAmountMatching(params.get("vnp_Amount"), payment.getAmount())) {
            log.warn("VNPay IPN: sai số tiền cho txnRef={} - từ chối, không đụng kho.", txnRef);
            payment.setStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            paymentRepository.save(payment);
            ordersRepository.save(order);
            return VnpayIpnResponse.amountMismatch();
        }

        boolean gatewaySuccess = VNP_SUCCESS_CODE.equals(params.get("vnp_ResponseCode"));

        if (!gatewaySuccess) {
            payment.setStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            paymentRepository.save(payment);
            ordersRepository.save(order);
            // rspCode "00" ở response CHO VNPAY nghĩa là "GHD đã nhận và xử lý xong IPN
            // này" - khác với vnp_ResponseCode (kết quả thanh toán thật). Trả ok() ở đây
            // là đúng dù kết quả nghiệp vụ là thất bại, để VNPay không retry vô ích.
            return VnpayIpnResponse.ok();
        }

        try {
            List<OrderCreatedEvent.OrderItemInfo> items = stockService.decrementStockForOrder(order.getId().toString());

            payment.setStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.PENDING); // rejoin luồng xác nhận/giao hàng admin bình thường
            paymentRepository.save(payment);
            Orders savedOrder = ordersRepository.save(order);

            OrderCreatedEvent event = new OrderCreatedEvent(
                    savedOrder.getId().toString(),
                    savedOrder.getCustomerName(),
                    savedOrder.getCustomerPhone(),
                    savedOrder.getCustomerEmail(),
                    savedOrder.getShippingAddress(),
                    payment.getPaymentMethod(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getNote(),
                    items
            );
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    kafkaTemplate.send(ORDER_EVENTS_TOPIC, savedOrder.getId().toString(), event);
                }
            });

            return VnpayIpnResponse.ok();
        } catch (InsufficientStockException ex) {
            // Tiền ĐÃ được VNPay xác nhận thu thành công - không được ghi Payment là
            // FAILED (sai sự thật). Chỉ đơn hàng chuyển PAYMENT_FAILED, admin hoàn tiền
            // thủ công qua merchant portal VNPay (v1 - xem plan, TODO tự động hoá phase 2).
            log.error("VNPay IPN: thanh toán thành công nhưng hết hàng lúc xác nhận, orderId={} - CẦN HOÀN TIỀN THỦ CÔNG. {}",
                    order.getId(), ex.getMessage());
            payment.setStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            order.setAdminNote("Thanh toán VNPay đã thành công nhưng hết hàng lúc xác nhận - CẦN HOÀN TIỀN THỦ CÔNG qua merchant portal VNPay. " + ex.getMessage());
            paymentRepository.save(payment);
            ordersRepository.save(order);
            return VnpayIpnResponse.ok();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(String orderId) {
        Orders order = ordersRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            return null;
        }
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedDateDesc(orderId).orElse(null);
        return PaymentStatusResponse.builder()
                .orderId(orderId)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .orderStatus(order.getStatus().name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatusByTxnRef(String txnRef) {
        Payment payment = paymentRepository.findByTxnRef(txnRef).orElse(null);
        if (payment == null) {
            return null;
        }
        return getStatus(payment.getOrderId());
    }

    @Override
    @Transactional
    public OrderCreationResult retryPayment(String orderId, String clientIp) {
        Orders order = ordersRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        PaymentEnum paymentMethod;
        try {
            paymentMethod = PaymentEnum.valueOf(order.getPaymentMethod());
        } catch (IllegalArgumentException | NullPointerException e) {
            paymentMethod = null;
        }
        if (paymentMethod != PaymentEnum.CARD && paymentMethod != PaymentEnum.INSTALLMENT) {
            throw new RuntimeException("Đơn hàng này không dùng thanh toán online (VNPay), không thể thử lại qua endpoint này");
        }

        // BƯỚC 1: khoá + huỷ payment PENDING cũ (nếu có) TRƯỚC KHI đụng tới Orders - giữ
        // đúng thứ tự khoá Payment-trước-Orders-sau giống hệt handleIpn, để 2 transaction
        // không bao giờ khoá theo thứ tự ngược nhau (nguyên nhân kinh điển gây deadlock -
        // finding của QA subagent). Đồng thời đây cũng là mutex chống 2 lần retry đồng
        // thời cho cùng 1 order: lần chạy sau phải đợi lần chạy trước commit xong.
        cancelAnyExistingPendingPayments(orderId);

        // BƯỚC 2: khoá lại Orders SAU (không dùng snapshot repeatable-read cũ ở trên) để
        // đọc trạng thái THẬT MỚI trước khi quyết định retry - tránh trường hợp đơn vừa
        // được 1 IPN xác nhận thành công đúng lúc giữa bước đọc ban đầu và bước này.
        Orders lockedOrder = ordersRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        if (lockedOrder.getStatus() != OrderStatus.PAYMENT_FAILED && lockedOrder.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new RuntimeException("Đơn hàng đang ở trạng thái " + lockedOrder.getStatus() + ", không thể thử lại thanh toán");
        }

        lockedOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
        Orders savedOrder = ordersRepository.save(lockedOrder);

        // cancelAnyExistingPendingPayments bên trong initiatePayment giờ là no-op (đã
        // huỷ ở BƯỚC 1) - giữ lại làm safety net chung cho mọi caller khác.
        String paymentUrl = initiatePayment(savedOrder, paymentMethod, clientIp);
        return new OrderCreationResult(savedOrder.getId().toString(), paymentUrl);
    }

    // Ghi cảnh báo vào adminNote của đơn khi IPN báo thành công cho 1 payment đã bị coi
    // là "xong" (CANCELLED/FAILED) từ trước - CHỈ ghi nếu đơn CHƯA được xác nhận thành
    // công qua 1 payment khác (tránh ghi đè/gây nhiễu cho 1 đơn đã xử lý xong bình
    // thường qua đường khác).
    private void flagOrderForManualReconciliation(String orderId, String txnRef, PaymentStatus previousStatus) {
        Orders order = ordersRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            return;
        }
        boolean alreadyResolvedElsewhere = order.getStatus() == OrderStatus.PENDING
                || order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.SHIPPING
                || order.getStatus() == OrderStatus.COMPLETED;
        if (alreadyResolvedElsewhere) {
            return;
        }
        String note = "CẢNH BÁO: IPN VNPay báo thành công cho txnRef=" + txnRef + " nhưng payment này đã "
                + previousStatus + " từ trước (đơn hết hạn hoặc đã thử lại bằng link khác) - CẦN ĐỐI SOÁT THỦ CÔNG, "
                + "khả năng khách đã bị thu tiền ngoài dự kiến.";
        order.setAdminNote(order.getAdminNote() == null ? note : order.getAdminNote() + " | " + note);
        ordersRepository.save(order);
    }

    // typical convention: vnp_Amount = số tiền thật x100. Xác nhận lại khi có sandbox.
    private boolean isAmountMatching(String vnpAmountParam, BigDecimal expectedAmount) {
        if (vnpAmountParam == null) {
            return false;
        }
        try {
            BigDecimal actual = new BigDecimal(vnpAmountParam).divide(BigDecimal.valueOf(100));
            return actual.compareTo(expectedAmount) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
