package guru.springframework.ghd.dto.payment;

import guru.springframework.ghd.constants.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Dùng cho trang return-URL (chỉ đọc) poll trạng thái sau khi khách quay lại từ VNPay -
// xem PaymentController#getStatus. Không dùng để đổi trạng thái, chỉ hiển thị.
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentStatusResponse {
    private String orderId;
    private PaymentStatus paymentStatus;
    private String orderStatus;
}
