package guru.springframework.ghd.utils;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VnpayUtilTest {

    @Test
    void buildSortedQueryStringSortsByKeyAndSkipsSignatureAndBlankValues() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", "abc123");
        params.put("vnp_Amount", "100000");
        params.put("vnp_SecureHash", "should-be-excluded");
        params.put("vnp_OrderInfo", "");
        params.put("vnp_Command", "pay");

        String result = VnpayUtil.buildSortedQueryString(params, "vnp_SecureHash");

        assertThat(result).isEqualTo("vnp_Amount=100000&vnp_Command=pay&vnp_TxnRef=abc123");
    }

    // Bug thật đã tái hiện: buildSortedQueryString từng nối giá trị THÔ, trong khi
    // VnpayServiceImpl.buildPaymentUrl build URL redirect bằng giá trị ĐÃ URL-encode
    // (URLEncoder.encode) - 2 chuỗi lệch nhau ngay khi 1 value có ký tự cần encode
    // (vnp_OrderInfo luôn có dấu cách), khiến VNPay luôn báo sai chữ ký. Value phải được
    // encode NGAY TẠI ĐÂY để chuỗi dùng để ký và chuỗi thật sự gửi đi luôn khớp nhau.
    @Test
    void buildSortedQueryStringUrlEncodesValuesContainingSpaces() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_OrderInfo", "Thanh toan don hang 123");
        params.put("vnp_TxnRef", "abc123");

        String result = VnpayUtil.buildSortedQueryString(params, "vnp_SecureHash");

        assertThat(result).isEqualTo("vnp_OrderInfo=Thanh+toan+don+hang+123&vnp_TxnRef=abc123");
    }

    // Thuật toán ký RIÊNG của Query/Refund API (querydr) - khác hẳn buildSortedQueryString
    // (không sort, không encode, nối theo đúng thứ tự truyền vào bằng dấu "|").
    @Test
    void buildPipeDelimitedHashJoinsInGivenOrderWithoutSortingOrEncoding() {
        String result = VnpayUtil.buildPipeDelimitedHash("req1", "2.1.0", "querydr", "TMNCODE", "txn123");

        assertThat(result).isEqualTo("req1|2.1.0|querydr|TMNCODE|txn123");
    }

    @Test
    void buildPipeDelimitedHashTreatsNullAsEmptySegment() {
        String result = VnpayUtil.buildPipeDelimitedHash("a", null, "c");

        assertThat(result).isEqualTo("a||c");
    }

    @Test
    void hmacSHA512IsDeterministicForSameInput() {
        String signature1 = VnpayUtil.hmacSHA512("secret", "data=value");
        String signature2 = VnpayUtil.hmacSHA512("secret", "data=value");

        assertThat(signature1).isEqualTo(signature2);
        assertThat(signature1).hasSize(128); // SHA-512 = 64 byte = 128 hex char
    }

    @Test
    void hmacSHA512DiffersWhenSecretDiffers() {
        String signature1 = VnpayUtil.hmacSHA512("secret-a", "data=value");
        String signature2 = VnpayUtil.hmacSHA512("secret-b", "data=value");

        assertThat(signature1).isNotEqualTo(signature2);
    }

    @Test
    void isSignatureValidComparesCorrectly() {
        String computed = VnpayUtil.hmacSHA512("secret", "data=value");

        assertThat(VnpayUtil.isSignatureValid(computed, computed)).isTrue();
        assertThat(VnpayUtil.isSignatureValid(computed, "tampered")).isFalse();
        assertThat(VnpayUtil.isSignatureValid(computed, null)).isFalse();
        assertThat(VnpayUtil.isSignatureValid(null, computed)).isFalse();
    }
}
