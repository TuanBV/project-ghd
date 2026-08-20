package guru.springframework.ghd.services;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.constants.enums.OrderStatus;
import guru.springframework.ghd.constants.enums.PaymentStatus;
import guru.springframework.ghd.entities.OrderDetail;
import guru.springframework.ghd.entities.Orders;
import guru.springframework.ghd.entities.Payment;
import guru.springframework.ghd.entities.Product;
import guru.springframework.ghd.repositories.OrderDetailRepository;
import guru.springframework.ghd.repositories.OrdersRepository;
import guru.springframework.ghd.repositories.PaymentRepository;
import guru.springframework.ghd.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Dùng overload nhận {@link Duration} trực tiếp để KHÔNG phải chờ thời gian thật hay
 * backdate {@code createdDate} (cột {@code updatable = false} qua Hibernate
 * {@code @CreationTimestamp} nên không sửa được bằng setter sau khi save).
 */
@SpringBootTest
class PaymentReconciliationServiceImplIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentReconciliationService reconciliationService;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ProductRepository productRepository;

    // queryTransaction gọi HTTP thật ra VNPay - mock để kiểm soát kết quả trả về mà
    // không cần sandbox thật, giống tinh thần PaymentServiceImplIntegrationTest verify
    // chữ ký/idempotency mà không gọi mạng.
    @MockitoBean
    private VnpayService vnpayService;

    // afterCommit của applyGatewayResult publish OrderCreatedEvent thật khi thành công -
    // mock để tránh gọi mạng thật ra localhost:9092 (không có Kafka trong AbstractIntegrationTest).
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private Product saveTestProduct(int stockQty) {
        Product product = Product.builder()
                .categoryId("test-category")
                .brandId("test-brand")
                .policyId("test-policy")
                .title("Test Product")
                .sku("TEST-SKU-" + UUID.randomUUID())
                .price(BigDecimal.valueOf(1_000_000))
                .salePrice(BigDecimal.valueOf(1_000_000))
                .stockQty(stockQty)
                .build();
        return productRepository.save(product);
    }

    private void saveOrderDetail(String orderId, String productId, int quantity, BigDecimal price) {
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(orderId);
        detail.setProductId(productId);
        detail.setQuantity(quantity);
        detail.setPrice(price);
        orderDetailRepository.save(detail);
    }

    private Orders saveOrder(OrderStatus status) {
        Orders order = new Orders();
        order.setCustomerName("Nguyen Van A");
        order.setCustomerPhone("0900000000");
        order.setShippingAddress("123 Test Street");
        order.setTotalAmount(BigDecimal.valueOf(1_000_000));
        order.setPaymentMethod("CARD");
        order.setStatus(status);
        return ordersRepository.save(order);
    }

    private Payment savePayment(String orderId, PaymentStatus status) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(BigDecimal.valueOf(1_000_000))
                .paymentMethod("CARD")
                .txnRef(UUID.randomUUID().toString().replace("-", ""))
                .status(status)
                .build();
        return paymentRepository.save(payment);
    }

    @Test
    void cancelsAwaitingPaymentOrderPastTtl() {
        Orders order = saveOrder(OrderStatus.AWAITING_PAYMENT);
        Payment payment = savePayment(order.getId().toString(), PaymentStatus.PENDING);

        int cancelledCount = reconciliationService.cancelExpiredAwaitingPayments(Duration.ZERO);

        assertThat(cancelledCount).isGreaterThanOrEqualTo(1);
        Orders reloadedOrder = ordersRepository.findByOrderId(order.getId().toString()).orElseThrow();
        assertThat(reloadedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(reloadedOrder.getAdminNote()).contains("quá hạn");
        Payment reloadedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(reloadedPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void doesNotTouchOrdersNotInAwaitingPaymentStatus() {
        Orders confirmedOrder = saveOrder(OrderStatus.PENDING);
        savePayment(confirmedOrder.getId().toString(), PaymentStatus.SUCCESS);

        reconciliationService.cancelExpiredAwaitingPayments(Duration.ZERO);

        Orders reloaded = ordersRepository.findByOrderId(confirmedOrder.getId().toString()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PENDING); // không bị đụng
    }

    @Test
    void doesNotCancelWhenPaymentAlreadyResolved() {
        // Mô phỏng: IPN đã xử lý xong (payment SUCCESS) nhưng vì lý do nào đó order
        // vẫn còn AWAITING_PAYMENT trong 1 khoảnh khắc (race hiếm) - job KHÔNG được tự
        // ý huỷ khi payment không còn PENDING.
        Orders order = saveOrder(OrderStatus.AWAITING_PAYMENT);
        savePayment(order.getId().toString(), PaymentStatus.SUCCESS);

        int cancelledCount = reconciliationService.cancelExpiredAwaitingPayments(Duration.ZERO);

        assertThat(cancelledCount).isZero();
        Orders reloaded = ordersRepository.findByOrderId(order.getId().toString()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT); // giữ nguyên, không đè lên IPN
    }

    @Test
    void reconcileSkipsPaymentWhenQueryTransactionReturnsNull() {
        // null = VNPay chưa có kết quả cuối cùng (đang xử lý/gọi lỗi) - job phải bỏ
        // qua, không được suy diễn thành công/thất bại.
        Orders order = saveOrder(OrderStatus.AWAITING_PAYMENT);
        Payment payment = savePayment(order.getId().toString(), PaymentStatus.PENDING);
        when(vnpayService.queryTransaction(any(Payment.class))).thenReturn(null);

        // graceWindow=0, ttl=1 ngày -> payment vừa tạo (age ~0) vẫn nằm trong khoảng
        // (now-ttl, now-graceWindow) vì graceWindow=0 nghĩa là windowEnd=now.
        reconciliationService.reconcilePendingPayments(Duration.ZERO, Duration.ofDays(1));

        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PENDING); // không bị đụng
        assertThat(ordersRepository.findByOrderId(order.getId().toString()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.AWAITING_PAYMENT);
    }

    @Test
    void reconcileAppliesSuccessResultFromQueryTransaction() {
        // Query API xác nhận giao dịch đã thành công (IPN thật đã bị rớt trước đó) -
        // job phải áp dụng ĐÚNG logic với IPN: trừ kho, xác nhận đơn - qua
        // PaymentService.applyGatewayResult dùng chung, không viết trùng.
        Product product = saveTestProduct(5);
        Orders order = saveOrder(OrderStatus.AWAITING_PAYMENT);
        saveOrderDetail(order.getId().toString(), product.getId(), 2, BigDecimal.valueOf(500_000));
        Payment payment = savePayment(order.getId().toString(), PaymentStatus.PENDING);

        Map<String, String> gatewayResult = Map.of(
                "vnp_TxnRef", payment.getTxnRef(),
                "vnp_ResponseCode", "00",
                "vnp_Amount", "100000000", // 1_000_000 x100
                "vnp_TransactionNo", "VNP123",
                "vnp_BankCode", "NCB"
        );
        when(vnpayService.queryTransaction(any(Payment.class))).thenReturn(gatewayResult);

        reconciliationService.reconcilePendingPayments(Duration.ZERO, Duration.ofDays(1));

        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(ordersRepository.findByOrderId(order.getId().toString()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PENDING); // rejoin luồng xác nhận/giao hàng admin bình thường
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQty()).isEqualTo(3);
    }

    @Test
    void reconcileAppliesFailureResultFromQueryTransaction() {
        Orders order = saveOrder(OrderStatus.AWAITING_PAYMENT);
        Payment payment = savePayment(order.getId().toString(), PaymentStatus.PENDING);

        Map<String, String> gatewayResult = Map.of(
                "vnp_TxnRef", payment.getTxnRef(),
                "vnp_ResponseCode", "02", // vnp_TransactionStatus khác "00"/"01" - thất bại
                "vnp_Amount", "100000000"
        );
        when(vnpayService.queryTransaction(any(Payment.class))).thenReturn(gatewayResult);

        reconciliationService.reconcilePendingPayments(Duration.ZERO, Duration.ofDays(1));

        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.FAILED);
        assertThat(ordersRepository.findByOrderId(order.getId().toString()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PAYMENT_FAILED);
    }
}
