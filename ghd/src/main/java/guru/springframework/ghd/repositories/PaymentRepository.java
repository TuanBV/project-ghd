package guru.springframework.ghd.repositories;

import guru.springframework.ghd.constants.enums.PaymentStatus;
import guru.springframework.ghd.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Row-locks the payment for the duration of the caller's transaction so a duplicate
    // IPN delivery for the same txnRef serializes instead of racing - cùng pattern với
    // ProductRepository.findByIdForUpdate. Đây là chốt chống trùng (idempotency) chính,
    // kết hợp với check payment.status != PENDING trong PaymentServiceImpl.handleIpn.
    @Query(value = "SELECT p.* FROM payment p WHERE p.txn_ref = :txnRef AND p.del_flag = 0 FOR UPDATE", nativeQuery = true)
    Optional<Payment> findByTxnRefForUpdate(@Param("txnRef") String txnRef);

    Optional<Payment> findFirstByOrderIdOrderByCreatedDateDesc(String orderId);

    // Không khoá - chỉ dùng cho lookup đọc (vd trang return-URL hiển thị trạng thái).
    Optional<Payment> findByTxnRef(String txnRef);

    // Khoá đúng row Payment mà PaymentServiceImpl.handleIpn cũng khoá (cùng bảng, cùng
    // cơ chế FOR UPDATE) - dùng bởi PaymentReconciliationServiceImpl.cancelExpiredAwaitingPayments
    // để job tự huỷ đơn quá hạn KHÔNG đè lên 1 IPN đang xử lý đồng thời cho cùng đơn đó:
    // job này phải đợi transaction IPN commit xong mới đọc được payment, và lúc đó thấy
    // status đã đổi (không còn PENDING) nên tự bỏ qua.
    @Query(value = """
            SELECT p.* FROM payment p
            WHERE p.order_id = :orderId AND p.del_flag = 0
            ORDER BY p.created_date DESC LIMIT 1 FOR UPDATE
            """, nativeQuery = true)
    Optional<Payment> findFirstByOrderIdForUpdateOrderByCreatedDateDesc(@Param("orderId") String orderId);

    // Dùng bởi PaymentReconciliationServiceImpl.reconcilePendingPayments để tìm các
    // payment "đủ cũ để nghi ngờ IPN bị rớt nhưng chưa tới hạn bị job huỷ".
    List<Payment> findAllByStatusAndCreatedDateBetween(PaymentStatus status, LocalDateTime start, LocalDateTime end);

    // Candidate cho PaymentReconciliationServiceImpl.cancelExpiredAwaitingPayments - CỐ
    // Ý dựa trên tuổi của PAYMENT (lần thử thanh toán hiện tại), KHÔNG phải tuổi của
    // Orders: nếu dùng Orders.createdDate (bất biến, không đổi khi retryPayment tạo
    // payment mới), 1 đơn cũ vừa được thử lại thành công sẽ vẫn bị coi là "quá hạn" và
    // bị job huỷ ngay lượt chạy kế tiếp dù payment vừa tạo còn rất mới - đây là bug thật
    // đã bị QA subagent phát hiện, xem .claude/skills/vnpay-payment/SKILL.md.
    List<Payment> findAllByStatusAndCreatedDateBefore(PaymentStatus status, LocalDateTime cutoff);

    // KHÔNG dùng trong logic nghiệp vụ (PaymentServiceImpl.cancelAnyExistingPendingPayments
    // dùng findFirstByOrderIdForUpdateOrderByCreatedDateDesc có khoá, vì bất biến "tối đa
    // 1 Payment PENDING/order" nghĩa là "mới nhất" luôn trùng với "PENDING duy nhất nếu
    // có") - chỉ dùng để assert trong test (vd xác nhận đúng 1 payment PENDING sau khi
    // retry), không khoá nên không dùng để quyết định nghiệp vụ.
    List<Payment> findAllByOrderIdAndStatus(String orderId, PaymentStatus status);
}
