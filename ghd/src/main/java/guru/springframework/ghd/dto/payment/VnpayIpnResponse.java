package guru.springframework.ghd.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Response riêng cho IPN VNPay - KHÔNG dùng BaseController.ok()/ApiResponse<T>, vì VNPay
// mong đợi đúng format {"RspCode": "...", "Message": "..."} của họ (tên field/giá trị
// rspCode chính xác cần xác nhận lại với tài liệu merchant thật khi có sandbox).
@Getter
@AllArgsConstructor
public class VnpayIpnResponse {
    private String rspCode;
    private String message;

    public static VnpayIpnResponse ok() {
        return new VnpayIpnResponse("00", "Confirm Success");
    }

    public static VnpayIpnResponse invalidSignature() {
        return new VnpayIpnResponse("97", "Invalid Signature");
    }

    public static VnpayIpnResponse orderNotFound() {
        return new VnpayIpnResponse("01", "Order not found");
    }

    public static VnpayIpnResponse amountMismatch() {
        return new VnpayIpnResponse("04", "Invalid amount");
    }

    public static VnpayIpnResponse alreadyConfirmed() {
        return new VnpayIpnResponse("02", "Order already confirmed");
    }
}
