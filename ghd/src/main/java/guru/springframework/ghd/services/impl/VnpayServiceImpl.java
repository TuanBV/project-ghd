package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.entities.Payment;
import guru.springframework.ghd.services.VnpayService;
import guru.springframework.ghd.utils.VnpayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

// Tích hợp VNPay theo convention phổ biến (Redirect để thanh toán + IPN + Query API) -
// CHƯA có sandbox VNPay thật lúc viết class này. Mọi tên field/format cụ thể dưới đây
// (vnp_*, nhân 100 cho amount, format ngày...) đánh dấu rõ "cần xác nhận lại với tài
// liệu merchant thật" thay vì khẳng định như đã chắc chắn đúng - xem
// .claude/skills/vnpay-payment/SKILL.md và plan gốc của tính năng này.
@Slf4j
@Service
public class VnpayServiceImpl implements VnpayService {

    private static final String SIGNATURE_FIELD = "vnp_SecureHash";
    private static final DateTimeFormatter CREATE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Value("${vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${vnpay.pay-url:}")
    private String payUrl;

    @Value("${vnpay.api-url:}")
    private String apiUrl;

    @Value("${app.base-url:http://localhost:18080}")
    private String appBaseUrl;

    @Override
    public String buildPaymentUrl(Payment payment, String clientIp) {
        if (tmnCode.isEmpty() || hashSecret.isEmpty() || payUrl.isEmpty()) {
            // Chưa cấu hình VNPAY_TMN_CODE/VNPAY_HASH_SECRET/VNPAY_PAY_URL thật (.env) -
            // fail rõ ràng thay vì âm thầm build 1 URL không dùng được.
            throw new IllegalStateException(
                    "VNPay chưa được cấu hình (VNPAY_TMN_CODE/VNPAY_HASH_SECRET/VNPAY_PAY_URL trong .env)");
        }

        // vnp_Amount theo convention phổ biến của VNPay là số tiền x100 (không có phần
        // thập phân) - XÁC NHẬN LẠI với tài liệu merchant thật trước khi go-live.
        long amountX100 = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(amountX100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getTxnRef());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + payment.getOrderId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", appBaseUrl + "/api/v1/payment/vnpay/return");
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", LocalDateTime.now().format(CREATE_DATE_FORMAT));
        // Không set vnp_BankCode - để trống cho VNPay tự hiển thị đầy đủ danh sách thẻ/
        // ngân hàng/đối tác trả góp trên trang của họ, theo quyết định đã xác nhận (GHD
        // không tự chọn hộ khách phương thức cụ thể trong nhóm CARD/INSTALLMENT).

        String queryToSign = VnpayUtil.buildSortedQueryString(params, SIGNATURE_FIELD);
        String secureHash = VnpayUtil.hmacSHA512(hashSecret, queryToSign);

        StringBuilder redirectUrl = new StringBuilder(payUrl).append('?');
        params.forEach((key, value) -> {
            redirectUrl.append(key).append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8))
                    .append('&');
        });
        redirectUrl.append(SIGNATURE_FIELD).append('=').append(secureHash);

        return redirectUrl.toString();
    }

    @Override
    public boolean verifySignature(Map<String, String> params) {
        String receivedSignature = params.get(SIGNATURE_FIELD);
        if (receivedSignature == null || hashSecret.isEmpty()) {
            return false;
        }
        String queryToSign = VnpayUtil.buildSortedQueryString(params, SIGNATURE_FIELD);
        String computedSignature = VnpayUtil.hmacSHA512(hashSecret, queryToSign);
        return VnpayUtil.isSignatureValid(computedSignature, receivedSignature);
    }

    @Override
    public void queryTransaction(String txnRef) {
        // TODO: gọi Query API thật của VNPay để đối soát khi IPN bị rớt (phase 2, xem
        // plan gốc). Chưa hiện thực ở v1 - chỉ khai interface.
        log.warn("VnpayService.queryTransaction chưa được hiện thực (phase 2) - txnRef={}", txnRef);
        throw new UnsupportedOperationException("VNPay Query API chưa được hiện thực ở v1");
    }
}
