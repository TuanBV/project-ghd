package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.dto.payment.PaymentStatusResponse;
import guru.springframework.ghd.dto.payment.VnpayIpnResponse;
import guru.springframework.ghd.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Route "/api/v1/payment/**" - đã thêm vào SecurityConfig.PUBLIC_URLS (server-to-server
// IPN không có JWT/session, return URL do trình duyệt gọi không auth).
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment")
public class PaymentController extends BaseController {

    private final PaymentService paymentService;

    // Method HTTP thật VNPay dùng để gọi IPN (GET hay POST) CẦN xác nhận với tài liệu
    // merchant khi có sandbox - chấp nhận cả 2 tạm thời cho tới lúc đó.
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<VnpayIpnResponse> vnpayIpnGet(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentService.handleIpn(params));
    }

    @PostMapping("/vnpay/ipn")
    public ResponseEntity<VnpayIpnResponse> vnpayIpnPost(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentService.handleIpn(params));
    }

    // CHỈ đọc - không bao giờ đổi trạng thái Payment/Orders ở đây. Trình duyệt gọi URL
    // này nên tham số có thể bị replay/tự chế bởi khách; IPN (ở trên) mới là nguồn sự
    // thật duy nhất cho việc đổi trạng thái. Tra theo vnp_TxnRef (VNPay redirect về kèm
    // param này, không phải orderId của GHD - typical convention, xác nhận lại tên field
    // thật khi có sandbox) để trả trạng thái hiện tại cho trang "kết quả thanh toán".
    @GetMapping("/vnpay/return")
    public ResponseEntity<?> vnpayReturn(@RequestParam(name = "vnp_TxnRef") String txnRef) {
        PaymentStatusResponse status = paymentService.getStatusByTxnRef(txnRef);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ok(status);
    }

    // Endpoint đọc nhẹ để frontend poll trạng thái (return-URL hoặc trang xem lại đơn).
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam String orderId) {
        PaymentStatusResponse status = paymentService.getStatus(orderId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ok(status);
    }

    // Tạo lần thử thanh toán mới cho đơn CARD/INSTALLMENT đang PAYMENT_FAILED/
    // AWAITING_PAYMENT (vd khách rớt mạng giữa chừng muốn thử lại) - cùng mô hình tin
    // cậy với GET /api/v1/order/{orderId} đã có (orderId UUID khó đoán, project không
    // có tài khoản gắn với đơn hàng để yêu cầu thêm auth ở đây).
    @PostMapping("/vnpay/retry")
    public ResponseEntity<?> retryPayment(@RequestParam String orderId, HttpServletRequest servletRequest) {
        try {
            return ok(paymentService.retryPayment(orderId, servletRequest.getRemoteAddr()));
        } catch (Exception ex) {
            return ng(ex.getMessage());
        }
    }
}
