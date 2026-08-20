package guru.springframework.ghd.services;

import guru.springframework.ghd.entities.Payment;

import java.util.Map;

public interface VnpayService {

    /** Build URL redirect sang VNPay để khách thanh toán payment này. */
    String buildPaymentUrl(Payment payment, String clientIp);

    /** Verify chữ ký HMAC-SHA512 của 1 request IPN/return - true nếu hợp lệ. */
    boolean verifySignature(Map<String, String> params);

    /**
     * Đối soát 1 giao dịch qua Query API (querydr) của VNPay khi IPN bị rớt/không tới.
     * Trả về map các field đã chuẩn hoá theo đúng tên field IPN dùng
     * ({@code vnp_ResponseCode}, {@code vnp_TransactionNo}, {@code vnp_BankCode},
     * {@code vnp_Amount}, {@code vnp_TxnRef}) để tái dùng
     * {@link PaymentService#applyGatewayResult} - CHỈ KHI đã có kết quả CUỐI CÙNG
     * (thành công/thất bại). Trả {@code null} nếu giao dịch vẫn đang xử lý
     * (VNPay trả {@code vnp_TransactionStatus="01"}), gọi API lỗi, thiếu
     * {@link Payment#getVnpCreateDate()} (payment tạo trước khi field này tồn tại),
     * hoặc không xác định được kết quả. Caller PHẢI coi {@code null} là "chưa biết, thử
     * lại ở lần đối soát sau" - không được suy diễn thành công/thất bại.
     */
    Map<String, String> queryTransaction(Payment payment);
}
