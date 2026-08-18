package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.constants.enums.OrderStatus;
import guru.springframework.ghd.constants.enums.PaymentStatus;
import guru.springframework.ghd.entities.Orders;
import guru.springframework.ghd.entities.Payment;
import guru.springframework.ghd.repositories.OrdersRepository;
import guru.springframework.ghd.repositories.PaymentRepository;
import guru.springframework.ghd.services.PaymentReconciliationService;
import guru.springframework.ghd.services.VnpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationServiceImpl implements PaymentReconciliationService {

    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final VnpayService vnpayService;

    @Value("${app.order.payment-ttl-minutes:15}")
    private long paymentTtlMinutes;

    @Value("${app.payment.reconcile-grace-minutes:3}")
    private long reconcileGraceMinutes;

    @Override
    @Scheduled(fixedDelayString = "${app.scheduling.cancel-stale-orders-fixed-delay-ms:60000}")
    public int cancelExpiredAwaitingPayments() {
        return cancelExpiredAwaitingPayments(Duration.ofMinutes(paymentTtlMinutes));
    }

    @Override
    @Transactional
    public int cancelExpiredAwaitingPayments(Duration ttl) {
        LocalDateTime cutoff = LocalDateTime.now().minus(ttl);
        // Candidate lấy theo tuổi của PAYMENT (lần thử hiện tại), KHÔNG phải tuổi của
        // Orders - nếu dùng Orders.createdDate (bất biến, không đổi khi retryPayment
        // tạo payment mới), 1 đơn cũ vừa được thử lại thành công vẫn sẽ bị coi là "quá
        // hạn" ngay lượt job kế tiếp dù payment vừa tạo còn rất mới (bug thật đã bị QA
        // subagent phát hiện - xem .claude/skills/vnpay-payment/SKILL.md).
        List<Payment> candidates = paymentRepository.findAllByStatusAndCreatedDateBefore(PaymentStatus.PENDING, cutoff);

        int cancelledCount = 0;
        for (Payment candidate : candidates) {
            if (cancelOneIfStillStale(candidate.getOrderId(), candidate.getId(), cutoff, ttl)) {
                cancelledCount++;
            }
        }
        if (cancelledCount > 0) {
            log.info("Đã tự động huỷ {} đơn AWAITING_PAYMENT quá hạn ({} phút).", cancelledCount, ttl.toMinutes());
        }
        return cancelledCount;
    }

    // Khoá payment MỚI NHẤT của order này trước (cùng row/cơ chế mà
    // PaymentServiceImpl.handleIpn khoá) rồi mới quyết định huỷ hay bỏ qua:
    // - Nếu 1 IPN đang xử lý đồng thời, job đợi IPN commit xong, đọc lại thấy status đã
    //   đổi (không còn PENDING) và tự bỏ qua, không bao giờ đè lên kết quả IPN.
    // - Nếu 1 lần retry vừa tạo payment MỚI cho order này giữa lúc quét candidate và
    //   lúc xử lý (id payment mới nhất khác candidateId đã quét được), job cũng tự bỏ
    //   qua - không huỷ nhầm payment vừa tạo, không huỷ payment cũ nữa vì initiatePayment
    //   đã tự huỷ nó rồi (xem PaymentServiceImpl.cancelAnyExistingPendingPayments).
    private boolean cancelOneIfStillStale(String orderId, UUID candidatePaymentId, LocalDateTime cutoff, Duration ttl) {
        Payment payment = paymentRepository.findFirstByOrderIdForUpdateOrderByCreatedDateDesc(orderId).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            return false;
        }
        if (!payment.getId().equals(candidatePaymentId)) {
            return false; // đã bị thay bằng 1 lần thử mới hơn (retry) - không còn là payment "quá hạn" ban đầu
        }
        if (payment.getCreatedDate() != null && payment.getCreatedDate().isAfter(cutoff)) {
            return false; // an toàn: xác nhận lại vẫn thật sự quá hạn sau khi khoá
        }

        Orders order = ordersRepository.findByOrderId(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            return false;
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setAdminNote("Tự động huỷ do quá hạn chờ thanh toán (quá " + ttl.toMinutes()
                + " phút không có xác nhận thanh toán).");
        paymentRepository.save(payment);
        ordersRepository.save(order);
        return true;
    }

    @Override
    @Scheduled(fixedDelayString = "${app.scheduling.reconcile-pending-fixed-delay-ms:300000}")
    public void reconcilePendingPayments() {
        reconcilePendingPayments(Duration.ofMinutes(reconcileGraceMinutes), Duration.ofMinutes(paymentTtlMinutes));
    }

    @Override
    public void reconcilePendingPayments(Duration graceWindow, Duration ttl) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minus(ttl); // chưa tới hạn bị job huỷ
        LocalDateTime windowEnd = now.minus(graceWindow); // đủ cũ để nghi ngờ IPN bị rớt

        List<Payment> candidates = paymentRepository.findAllByStatusAndCreatedDateBetween(
                PaymentStatus.PENDING, windowStart, windowEnd);

        for (Payment payment : candidates) {
            try {
                // VnpayService.queryTransaction CHƯA hiện thực thật ở v1 (throw
                // UnsupportedOperationException theo thiết kế) - method này chỉ là
                // KHUNG, tự động có tác dụng khi queryTransaction được hiện thực thật
                // (có sandbox) mà không cần sửa gì ở đây.
                // TODO: khi queryTransaction trả kết quả thật, xử lý ở đây: nếu VNPay
                // xác nhận thành công/thất bại, áp dụng cùng logic với
                // PaymentServiceImpl.handleIpn (KHÔNG viết trùng - trích xuất phần xử
                // lý kết quả IPN thành phương thức dùng chung nếu cần).
                vnpayService.queryTransaction(payment.getTxnRef());
            } catch (Exception e) {
                log.warn("Đối soát VNPay cho txnRef={} chưa thực hiện được: {}", payment.getTxnRef(), e.getMessage());
            }
        }
    }
}
