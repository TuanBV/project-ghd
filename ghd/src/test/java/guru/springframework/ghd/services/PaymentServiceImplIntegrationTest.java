package guru.springframework.ghd.services;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.constants.enums.OrderStatus;
import guru.springframework.ghd.constants.enums.PaymentStatus;
import guru.springframework.ghd.dto.payment.VnpayIpnResponse;
import guru.springframework.ghd.entities.OrderDetail;
import guru.springframework.ghd.entities.Orders;
import guru.springframework.ghd.entities.Payment;
import guru.springframework.ghd.entities.Product;
import guru.springframework.ghd.repositories.OrderDetailRepository;
import guru.springframework.ghd.repositories.OrdersRepository;
import guru.springframework.ghd.repositories.PaymentRepository;
import guru.springframework.ghd.repositories.ProductRepository;
import guru.springframework.ghd.utils.VnpayUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Xác nhận phần có thể test được KHÔNG CẦN sandbox VNPay thật: verify chữ ký, chống
 * trùng (idempotency), đối chiếu số tiền, trừ kho qua {@link StockService} dùng chung
 * với luồng COD/BANK_TRANSFER, và trường hợp hết hàng đúng lúc IPN xác nhận thành công.
 * Tên field vnp_* dùng trong test là convention phổ biến - xem
 * {@code .claude/skills/vnpay-payment/SKILL.md}.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "vnpay.hash-secret=integration-test-secret",
        "vnpay.tmn-code=TESTCODE",
        "vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
})
class PaymentServiceImplIntegrationTest extends AbstractIntegrationTest {

    private static final String HASH_SECRET = "integration-test-secret";

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ProductRepository productRepository;

    // Không có Kafka trong AbstractIntegrationTest (chỉ Testcontainers MySQL+Redis) -
    // mock để tránh gọi mạng thật ra localhost:9092 lúc afterCommit publish OrderCreatedEvent.
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

    private Orders saveAwaitingPaymentOrder(BigDecimal amount) {
        Orders order = new Orders();
        order.setCustomerName("Nguyen Van A");
        order.setCustomerPhone("0900000000");
        order.setShippingAddress("123 Test Street");
        order.setTotalAmount(amount);
        order.setPaymentMethod("CARD");
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        return ordersRepository.save(order);
    }

    private void saveOrderDetail(String orderId, String productId, int quantity, BigDecimal price) {
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(orderId);
        detail.setProductId(productId);
        detail.setQuantity(quantity);
        detail.setPrice(price);
        orderDetailRepository.save(detail);
    }

    private void savePendingPayment(String orderId, BigDecimal amount, String txnRef) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .paymentMethod("CARD")
                .txnRef(txnRef)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);
    }

    private Map<String, String> signedIpnParams(String txnRef, BigDecimal amount, String responseCode) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TransactionNo", "VNP" + System.nanoTime());
        params.put("vnp_BankCode", "NCB");

        String queryToSign = VnpayUtil.buildSortedQueryString(params, "vnp_SecureHash");
        params.put("vnp_SecureHash", VnpayUtil.hmacSHA512(HASH_SECRET, queryToSign));
        return params;
    }

    @Test
    void ipnReportingSuccessForAlreadyCancelledPaymentFlagsOrderInsteadOfSilentlyIgnoring() {
        // Tái hiện đúng finding [major] của QA subagent: payment đã bị CANCELLED (do
        // job hết hạn hoặc bị 1 lần retry thay thế) trước khi IPN cho txnRef CŨ này tới
        // và báo thành công - đây KHÔNG phải trùng lặp bình thường (khác payment SUCCESS
        // gọi lại lần 2), phải được cảnh báo rõ để đối soát thủ công, không được âm thầm
        // trả ok() như không có chuyện gì.
        Orders order = saveAwaitingPaymentOrder(BigDecimal.valueOf(1_000_000));
        String txnRef = UUID.randomUUID().toString().replace("-", "");
        savePendingPayment(order.getId().toString(), order.getTotalAmount(), txnRef);
        Payment payment = paymentRepository.findByTxnRef(txnRef).orElseThrow();
        payment.setStatus(PaymentStatus.CANCELLED); // giả lập đã bị huỷ (hết hạn/bị retry thay thế)
        paymentRepository.save(payment);

        VnpayIpnResponse response = paymentService.handleIpn(signedIpnParams(txnRef, order.getTotalAmount(), "00"));

        assertThat(response.getRspCode()).isEqualTo("00"); // vẫn ok với VNPay (đã nhận IPN)
        assertThat(paymentRepository.findByTxnRef(txnRef).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELLED); // KHÔNG bị đổi ngược lại SUCCESS
        Orders reloadedOrder = ordersRepository.findByOrderId(order.getId().toString()).orElseThrow();
        assertThat(reloadedOrder.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT); // không tự ý xác nhận đơn
        assertThat(reloadedOrder.getAdminNote()).contains("CẢNH BÁO").contains("ĐỐI SOÁT THỦ CÔNG");
    }

    @Test
    void tamperedSignatureIsRejectedWithoutTouchingDb() {
        Orders order = saveAwaitingPaymentOrder(BigDecimal.valueOf(1_000_000));
        String txnRef = UUID.randomUUID().toString().replace("-", "");
        savePendingPayment(order.getId().toString(), order.getTotalAmount(), txnRef);

        Map<String, String> params = signedIpnParams(txnRef, order.getTotalAmount(), "00");
        params.put("vnp_Amount", "999999900"); // đổi sau khi đã ký -> chữ ký không còn khớp

        VnpayIpnResponse response = paymentService.handleIpn(params);

        assertThat(response.getRspCode()).isEqualTo("97");
        Payment reloaded = paymentRepository.findByTxnRef(txnRef).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void successfulIpnDecrementsStockAndConfirmsOrder() {
        Product product = saveTestProduct(5);
        Orders order = saveAwaitingPaymentOrder(BigDecimal.valueOf(1_000_000));
        saveOrderDetail(order.getId().toString(), product.getId(), 2, BigDecimal.valueOf(500_000));
        String txnRef = UUID.randomUUID().toString().replace("-", "");
        savePendingPayment(order.getId().toString(), order.getTotalAmount(), txnRef);

        VnpayIpnResponse response = paymentService.handleIpn(signedIpnParams(txnRef, order.getTotalAmount(), "00"));

        assertThat(response.getRspCode()).isEqualTo("00");
        assertThat(ordersRepository.findByOrderId(order.getId().toString()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PENDING);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQty()).isEqualTo(3);
        assertThat(paymentRepository.findByTxnRef(txnRef).orElseThrow().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void duplicateIpnDeliveryIsANoOp() {
        Product product = saveTestProduct(5);
        Orders order = saveAwaitingPaymentOrder(BigDecimal.valueOf(500_000));
        saveOrderDetail(order.getId().toString(), product.getId(), 1, BigDecimal.valueOf(500_000));
        String txnRef = UUID.randomUUID().toString().replace("-", "");
        savePendingPayment(order.getId().toString(), order.getTotalAmount(), txnRef);
        Map<String, String> params = signedIpnParams(txnRef, order.getTotalAmount(), "00");

        paymentService.handleIpn(params);
        VnpayIpnResponse secondResponse = paymentService.handleIpn(params); // VNPay gọi lại lần 2

        assertThat(secondResponse.getRspCode()).isEqualTo("00");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQty()).isEqualTo(4);
    }

    @Test
    void amountMismatchFailsWithoutTouchingStock() {
        Product product = saveTestProduct(5);
        Orders order = saveAwaitingPaymentOrder(BigDecimal.valueOf(1_000_000));
        saveOrderDetail(order.getId().toString(), product.getId(), 1, BigDecimal.valueOf(1_000_000));
        String txnRef = UUID.randomUUID().toString().replace("-", "");
        savePendingPayment(order.getId().toString(), order.getTotalAmount(), txnRef);

        // Ký đúng nhưng với số tiền khác số tiền thật của payment (giả lập kẻ tấn công
        // tự ký lại với số tiền thấp hơn bằng secret bị lộ, hoặc lỗi tích hợp phía VNPay).
        VnpayIpnResponse response = paymentService.handleIpn(signedIpnParams(txnRef, BigDecimal.valueOf(500_000), "00"));

        assertThat(response.getRspCode()).isEqualTo("04");
        assertThat(ordersRepository.findByOrderId(order.getId().toString()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQty()).isEqualTo(5);
    }

    @Test
    void insufficientStockAtConfirmationMarksOrderPaymentFailedButPaymentSuccess() {
        Product product = saveTestProduct(1); // chỉ còn 1 trong kho
        Orders order = saveAwaitingPaymentOrder(BigDecimal.valueOf(1_000_000));
        saveOrderDetail(order.getId().toString(), product.getId(), 2, BigDecimal.valueOf(500_000)); // đặt 2
        String txnRef = UUID.randomUUID().toString().replace("-", "");
        savePendingPayment(order.getId().toString(), order.getTotalAmount(), txnRef);

        VnpayIpnResponse response = paymentService.handleIpn(signedIpnParams(txnRef, order.getTotalAmount(), "00"));

        assertThat(response.getRspCode()).isEqualTo("00"); // GHD đã nhận/xử lý xong IPN
        assertThat(paymentRepository.findByTxnRef(txnRef).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCESS); // tiền đã thu - không được ghi FAILED
        Orders reloadedOrder = ordersRepository.findByOrderId(order.getId().toString()).orElseThrow();
        assertThat(reloadedOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(reloadedOrder.getAdminNote()).contains("HOÀN TIỀN THỦ CÔNG");
    }

    @Test
    void retryPaymentOnFailedOrderCreatesNewPendingPaymentAndReturnsUrl() {
        Orders order = saveAwaitingPaymentOrder(BigDecimal.valueOf(1_000_000));
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        ordersRepository.save(order);

        var result = paymentService.retryPayment(order.getId().toString(), "127.0.0.1");

        assertThat(result.paymentUrl()).isNotBlank();
        assertThat(ordersRepository.findByOrderId(order.getId().toString()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.AWAITING_PAYMENT);
        Payment newPayment = paymentRepository.findFirstByOrderIdOrderByCreatedDateDesc(order.getId().toString())
                .orElseThrow();
        assertThat(newPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void retryPaymentOnAwaitingPaymentOrderCancelsOldPendingPaymentInsteadOfLeavingItOrphaned() {
        // Tái hiện đúng bug đã bị QA subagent phát hiện: retry trên đơn ĐANG
        // AWAITING_PAYMENT (chưa từng thất bại, khách chỉ rớt mạng/muốn thử lại) từng
        // để lại payment PENDING cũ "mồ côi" -> vi phạm bất biến tối đa 1 payment PENDING
        // /order, khiến job huỷ hạn dùng nhầm tuổi Orders (bất biến) và có thể huỷ nhầm
        // lần retry vừa tạo, hoặc để lộ nguy cơ double-decrement nếu cả 2 link đều được
        // thanh toán.
        Product product = saveTestProduct(5);
        Orders order = saveAwaitingPaymentOrder(BigDecimal.valueOf(1_000_000));
        saveOrderDetail(order.getId().toString(), product.getId(), 1, BigDecimal.valueOf(1_000_000));
        String oldTxnRef = UUID.randomUUID().toString().replace("-", "");
        savePendingPayment(order.getId().toString(), order.getTotalAmount(), oldTxnRef);

        var result = paymentService.retryPayment(order.getId().toString(), "127.0.0.1");

        Payment oldPayment = paymentRepository.findByTxnRef(oldTxnRef).orElseThrow();
        assertThat(oldPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED); // không còn "mồ côi" PENDING

        Payment newPayment = paymentRepository.findFirstByOrderIdOrderByCreatedDateDesc(order.getId().toString())
                .orElseThrow();
        assertThat(newPayment.getTxnRef()).isNotEqualTo(oldTxnRef);
        assertThat(newPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.paymentUrl()).contains(newPayment.getTxnRef());

        // Chỉ đúng 1 payment PENDING tồn tại cho order này tại mọi thời điểm.
        long pendingCount = paymentRepository.findAllByOrderIdAndStatus(order.getId().toString(), PaymentStatus.PENDING).size();
        assertThat(pendingCount).isEqualTo(1);
    }

    @Test
    void retryPaymentRejectsCodOrder() {
        Orders order = saveAwaitingPaymentOrder(BigDecimal.valueOf(1_000_000));
        order.setPaymentMethod("COD");
        order.setStatus(OrderStatus.PENDING);
        ordersRepository.save(order);

        assertThatThrownBy(() -> paymentService.retryPayment(order.getId().toString(), "127.0.0.1"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void retryPaymentRejectsAlreadySucceededOrder() {
        Orders order = saveAwaitingPaymentOrder(BigDecimal.valueOf(1_000_000));
        order.setStatus(OrderStatus.PENDING); // đã thanh toán xong (rejoin luồng admin bình thường)
        ordersRepository.save(order);

        assertThatThrownBy(() -> paymentService.retryPayment(order.getId().toString(), "127.0.0.1"))
                .isInstanceOf(RuntimeException.class);
    }
}
