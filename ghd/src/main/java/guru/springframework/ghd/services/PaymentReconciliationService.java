package guru.springframework.ghd.services;

import java.time.Duration;

public interface PaymentReconciliationService {

    /**
     * Huỷ mọi đơn {@code AWAITING_PAYMENT} đã quá hạn (dùng
     * {@code app.order.payment-ttl-minutes} cấu hình) - gọi bởi {@code @Scheduled}.
     */
    int cancelExpiredAwaitingPayments();

    /**
     * Như {@link #cancelExpiredAwaitingPayments()} nhưng nhận thẳng TTL - dùng để test
     * không cần chờ thời gian thật (vd {@code Duration.ZERO} coi mọi
     * {@code AWAITING_PAYMENT} hiện có là quá hạn).
     *
     * @return số đơn đã huỷ
     */
    int cancelExpiredAwaitingPayments(Duration ttl);

    /**
     * Với các {@code Payment} đang PENDING quá lâu (nghi ngờ IPN bị rớt) nhưng chưa tới
     * hạn bị huỷ, thử hỏi lại VNPay qua {@code VnpayService.queryTransaction}. Ở v1
     * method đó chưa hiện thực thật (throw {@code UnsupportedOperationException}) - job
     * này chỉ là KHUNG, tự động hoạt động khi method đó được hiện thực thật sau này,
     * không cần sửa gì ở đây. Không bao giờ throw ra ngoài (mỗi item lỗi được log và bỏ
     * qua riêng, không làm hỏng cả job).
     */
    void reconcilePendingPayments();

    /** Như {@link #reconcilePendingPayments()} nhưng nhận thẳng grace-window/TTL - dùng để test. */
    void reconcilePendingPayments(Duration graceWindow, Duration ttl);
}
