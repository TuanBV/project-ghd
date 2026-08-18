package guru.springframework.ghd.security;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.entities.SysParam;
import guru.springframework.ghd.repositories.SysParamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "/api/v1/payment/**" phải nằm trong SecurityConfig.PUBLIC_URLS - IPN (server-to-server)
 * và return URL (trình duyệt) không mang JWT/session nên sẽ bị 401 nếu wiring sai. Bảo
 * mật thật của IPN nằm ở verify chữ ký trong PaymentServiceImpl, không phải ở đây - test
 * này chỉ xác nhận route KHÔNG bị chặn bởi Spring Security trước khi chạm tới handler.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentPublicUrlsTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysParamRepository sysParamRepository;

    @BeforeEach
    void seedHotlineSysParam() {
        // Xem ghi chú tương tự trong ApiStatelessSessionTest - GlobalCommon.getHotline()
        // 500 nếu thiếu sys_param "hotline" (bug có sẵn, không liên quan tính năng này).
        if (sysParamRepository.findByParamKey("hotline").isEmpty()) {
            sysParamRepository.save(SysParam.builder()
                    .paramKey("hotline")
                    .paramValue("0123456789")
                    .paramName("Hotline")
                    .build());
        }
    }

    @Test
    void ipnEndpointIsPublicNotUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/payment/vnpay/ipn").param("vnp_TxnRef", "does-not-exist"))
                .andExpect(status().isOk()); // handler tự trả VnpayIpnResponse.orderNotFound(), không phải 401
    }

    @Test
    void returnUrlEndpointIsPublicNotUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/payment/vnpay/return").param("vnp_TxnRef", "does-not-exist"))
                .andExpect(status().isNotFound()); // 404 nghiệp vụ (không tìm thấy payment), không phải 401
    }

    @Test
    void statusEndpointIsPublicNotUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/payment/status").param("orderId", "does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
