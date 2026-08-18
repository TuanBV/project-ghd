package guru.springframework.ghd.services;

import guru.springframework.ghd.entities.Payment;

import java.util.Map;

public interface VnpayService {

    /** Build URL redirect sang VNPay để khách thanh toán payment này. */
    String buildPaymentUrl(Payment payment, String clientIp);

    /** Verify chữ ký HMAC-SHA512 của 1 request IPN/return - true nếu hợp lệ. */
    boolean verifySignature(Map<String, String> params);

    /**
     * Đối soát 1 giao dịch qua Query API của VNPay khi IPN bị rớt/không tới.
     * Chưa hiện thực thật ở v1 (phase 2, xem plan) - chỉ khai interface để chỗ gọi
     * (nếu có) không phải đổi lại sau này.
     */
    void queryTransaction(String txnRef);
}
