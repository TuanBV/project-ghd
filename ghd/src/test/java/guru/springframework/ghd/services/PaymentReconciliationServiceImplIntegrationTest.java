package guru.springframework.ghd.services;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.constants.enums.OrderStatus;
import guru.springframework.ghd.constants.enums.PaymentStatus;
import guru.springframework.ghd.entities.Orders;
import guru.springframework.ghd.entities.Payment;
import guru.springframework.ghd.repositories.OrdersRepository;
import guru.springframework.ghd.repositories.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
    void reconcileDoesNotThrowEvenThoughQueryTransactionIsUnimplemented() {
        Orders order = saveOrder(OrderStatus.AWAITING_PAYMENT);
        savePayment(order.getId().toString(), PaymentStatus.PENDING);

        // graceWindow=0, ttl=1 ngày -> payment vừa tạo (age ~0) vẫn nằm trong khoảng
        // (now-ttl, now-graceWindow) vì graceWindow=0 nghĩa là windowEnd=now.
        reconciliationService.reconcilePendingPayments(Duration.ZERO, Duration.ofDays(1));
        // Không throw ra ngoài dù VnpayServiceImpl.queryTransaction throw
        // UnsupportedOperationException bên trong - đó chính là điều cần khẳng định.
    }
}
