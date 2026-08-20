package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.entities.Payment;
import guru.springframework.ghd.services.VnpayService;
import guru.springframework.ghd.utils.VnpayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// Tích hợp VNPay theo convention phổ biến (Redirect để thanh toán + IPN + Query API) -
// CHƯA có sandbox VNPay thật lúc viết class này. Mọi tên field/format cụ thể dưới đây
// (vnp_*, nhân 100 cho amount, format ngày...) đánh dấu rõ "cần xác nhận lại với tài
// liệu merchant thật" thay vì khẳng định như đã chắc chắn đúng - xem
// .claude/skills/vnpay-payment/SKILL.md và docs/VNPAY_INTEGRATION.md.
@Slf4j
@Service
public class VnpayServiceImpl implements VnpayService {

    private static final String SIGNATURE_FIELD = "vnp_SecureHash";
    private static final String TRANSACTION_STATUS_FIELD = "vnp_TransactionStatus";
    private static final String TRANSACTION_STATUS_SUCCESS = "00";
    private static final String TRANSACTION_STATUS_PROCESSING = "01";
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

    // Cùng pattern với TelegramService - field đơn giản, không cần thêm dependency.
    private final RestTemplate restTemplate = new RestTemplate();

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

        // Lưu lại ĐÚNG giá trị này lên payment - Query API đối soát sau này
        // (queryTransaction) cần vnp_TransactionDate khớp chính xác giá trị gốc, không
        // được suy ra từ payment.createdDate (có thể lệch vài trăm ms - xem entity).
        String createDate = LocalDateTime.now().format(CREATE_DATE_FORMAT);
        payment.setVnpCreateDate(createDate);

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
        params.put("vnp_CreateDate", createDate);
        // Không set vnp_BankCode - để trống cho VNPay tự hiển thị đầy đủ danh sách thẻ/
        // ngân hàng/đối tác trả góp trên trang của họ, theo quyết định đã xác nhận (GHD
        // không tự chọn hộ khách phương thức cụ thể trong nhóm CARD/INSTALLMENT).

        // Ký và build URL từ ĐÚNG 1 chuỗi đã sort + URL-encode (VnpayUtil.buildSortedQueryString)
        // - trước đây hash trên giá trị thô rồi encode riêng lúc build URL, 2 chuỗi lệch
        // nhau ngay khi có value chứa ký tự cần encode (vnp_OrderInfo có dấu cách), khiến
        // VNPay luôn trả lỗi sai chữ ký. Giữ nguyên 1 nguồn duy nhất để không tái diễn.
        String signedQuery = VnpayUtil.buildSortedQueryString(params, SIGNATURE_FIELD);
        String secureHash = VnpayUtil.hmacSHA512(hashSecret, signedQuery);

        return payUrl + "?" + signedQuery + "&" + SIGNATURE_FIELD + "=" + secureHash;
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
    public Map<String, String> queryTransaction(Payment payment) {
        if (tmnCode.isEmpty() || hashSecret.isEmpty() || apiUrl.isEmpty()) {
            log.warn("VNPay Query API chưa được cấu hình (VNPAY_TMN_CODE/VNPAY_HASH_SECRET/VNPAY_API_URL) - "
                    + "bỏ qua đối soát txnRef={}", payment.getTxnRef());
            return null;
        }
        if (payment.getVnpCreateDate() == null) {
            log.warn("Payment txnRef={} không có vnpCreateDate (tạo trước khi field này tồn tại) - "
                    + "không đối soát được qua Query API", payment.getTxnRef());
            return null;
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        String createDate = LocalDateTime.now().format(CREATE_DATE_FORMAT);
        String orderInfo = "Kiem tra ket qua giao dich " + payment.getOrderId();

        // Thuật toán ký RIÊNG của Query/Refund API (querydr) - nối theo thứ tự CỐ ĐỊNH
        // bằng dấu "|" (KHÁC buildSortedQueryString dùng cho payment URL/IPN) -
        // convention phổ biến, CHƯA verify với tài liệu merchant thật.
        String hashData = VnpayUtil.buildPipeDelimitedHash(
                requestId, "2.1.0", "querydr", tmnCode, payment.getTxnRef(),
                payment.getVnpCreateDate(), createDate, "127.0.0.1", orderInfo);
        String secureHash = VnpayUtil.hmacSHA512(hashSecret, hashData);

        Map<String, String> request = new LinkedHashMap<>();
        request.put("vnp_RequestId", requestId);
        request.put("vnp_Version", "2.1.0");
        request.put("vnp_Command", "querydr");
        request.put("vnp_TmnCode", tmnCode);
        request.put("vnp_TxnRef", payment.getTxnRef());
        request.put("vnp_OrderInfo", orderInfo);
        request.put("vnp_TransactionDate", payment.getVnpCreateDate());
        request.put("vnp_CreateDate", createDate);
        request.put("vnp_IpAddr", "127.0.0.1");
        request.put(SIGNATURE_FIELD, secureHash);

        Map<?, ?> response;
        try {
            response = restTemplate.postForObject(apiUrl, request, Map.class);
        } catch (Exception e) {
            log.warn("VNPay Query API lỗi khi gọi cho txnRef={}: {}", payment.getTxnRef(), e.getMessage());
            return null;
        }
        if (response == null) {
            return null;
        }

        String transactionStatus = asString(response.get(TRANSACTION_STATUS_FIELD));
        if (transactionStatus == null || TRANSACTION_STATUS_PROCESSING.equals(transactionStatus)) {
            // Vẫn đang xử lý hoặc response không có field này (lỗi/không xác định) -
            // chưa có kết quả cuối cùng, để lần đối soát sau thử lại.
            return null;
        }

        // KHÔNG verify chữ ký response ở bản này - quyết định có chủ đích (xem
        // docs/VNPAY_INTEGRATION.md): đây là cuộc gọi HTTPS do chính GHD chủ động gọi
        // thẳng domain thật của VNPay (TLS đã xác thực server), khác IPN - nơi bất kỳ ai
        // cũng POST được tới endpoint public của GHD. Thuật toán ký cho RESPONSE của
        // Query API càng chưa chắc chắn hơn cả request - verify sai có thể tự loại bỏ
        // kết quả đúng, rủi ro cao hơn lợi ích ở bước này.
        Map<String, String> normalized = new LinkedHashMap<>();
        normalized.put("vnp_TxnRef", payment.getTxnRef());
        // vnp_TransactionStatus "00" = thành công, khác "00" (đã loại "01" ở trên) =
        // thất bại - map sang vnp_ResponseCode để tái dùng PaymentService.applyGatewayResult
        // (vốn key theo vnp_ResponseCode, giống IPN).
        normalized.put("vnp_ResponseCode",
                TRANSACTION_STATUS_SUCCESS.equals(transactionStatus) ? "00" : transactionStatus);
        normalized.put("vnp_Amount", asString(response.get("vnp_Amount")));
        normalized.put("vnp_TransactionNo", asString(response.get("vnp_TransactionNo")));
        normalized.put("vnp_BankCode", asString(response.get("vnp_BankCode")));
        return normalized;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
