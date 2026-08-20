package guru.springframework.ghd.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

// HMAC-SHA512 signing/verification cho VNPay - convention phổ biến của VNPay là ký trên
// chuỗi query string đã sort theo key. Chỉ dùng javax.crypto có sẵn trong JDK, không
// thêm dependency mới (xem .claude/DEPENDENCY_ALLOWLIST.md).
//
// LƯU Ý: cách encode từng value (URL-encode hay không), ký tự nối ("&"/"="), và việc có
// bao gồm param rỗng hay không đều là chi tiết CỤ THỂ của VNPay - cần xác nhận lại với
// tài liệu merchant thật khi có sandbox trước khi tin tưởng hoàn toàn cách build ở đây.
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VnpayUtil {

    /**
     * Sort params theo key (thứ tự tự nhiên của String), URL-encode từng value rồi nối
     * thành {@code key1=value1&key2=value2&...}, bỏ qua key có value null/rỗng và bỏ qua
     * chính field chữ ký nếu lỡ có mặt trong map truyền vào.
     * <p>
     * PHẢI encode value trước khi hash (khớp thuật toán chuẩn của VNPay) - nếu hash trên
     * giá trị thô rồi mới encode riêng lúc build URL (bug cũ), chuỗi bị ký và chuỗi thật
     * sự gửi đi sẽ khác nhau ngay khi 1 value có ký tự cần encode (vd dấu cách trong
     * vnp_OrderInfo), khiến VNPay luôn báo sai chữ ký bất kể secret đúng hay không.
     */
    public static String buildSortedQueryString(Map<String, String> params, String signatureFieldName) {
        TreeMap<String, String> sorted = new TreeMap<>();
        params.forEach((key, value) -> {
            if (value != null && !value.isEmpty() && !key.equals(signatureFieldName)) {
                sorted.put(key, value);
            }
        });

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    /**
     * Nối các value theo ĐÚNG thứ tự tham số truyền vào (không sort, không encode) bằng
     * dấu {@code |} - thuật toán ký RIÊNG của Query/Refund API (querydr), KHÁC hẳn
     * {@link #buildSortedQueryString}. Value {@code null} được coi là chuỗi rỗng (VNPay
     * quy ước field vắng mặt vẫn có vị trí trong chuỗi ký, không bỏ qua như payment
     * URL/IPN). CHƯA verify với tài liệu merchant thật - xem VnpayServiceImpl.queryTransaction.
     */
    public static String buildPipeDelimitedHash(String... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(values[i] == null ? "" : values[i]);
        }
        return sb.toString();
    }

    public static String hmacSHA512(String secret, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKeySpec);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được chữ ký VNPay", e);
        }
    }

    /** So sánh constant-time - không dùng String#equals để tránh timing attack. */
    public static boolean isSignatureValid(String computedSignature, String receivedSignature) {
        if (computedSignature == null || receivedSignature == null) {
            return false;
        }
        return MessageDigest.isEqual(
                computedSignature.getBytes(StandardCharsets.UTF_8),
                receivedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
}
