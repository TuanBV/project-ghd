package guru.springframework.ghd.security;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.entities.User;
import guru.springframework.ghd.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm chứng phần vừa thêm vào luồng login/logout: access token bị blacklist ngay khi
 * logout (không còn xài được dù chưa hết hạn), và refresh token rotate mỗi lần dùng
 * (bản cũ bị từ chối sau khi đã dùng, hoặc sau khi đã logout).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFlowTest extends AbstractIntegrationTest {

    private static final String USERNAME = "auth_flow_test_user";
    private static final String PASSWORD = "Password123!";
    // Path bất kỳ dưới /api/v1/** không nằm trong PUBLIC_URLS và không phải GET - chỉ
    // dùng để phân biệt 401 (chưa xác thực, authenticationEntryPoint chặn) và không-401
    // (đã xác thực, qua được authorizeHttpRequests) mà không cần một business endpoint
    // thật. Không giả định mã trạng thái cụ thể khi đã xác thực (không có handler thật
    // khớp path này) - chỉ cần khác 401.
    private static final String PROTECTED_PROBE_PATH = "/api/v1/__auth_probe__";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedTestUser() {
        userRepository.findByUsername(USERNAME).orElseGet(() -> userRepository.save(User.builder()
                .username(USERNAME)
                .fullName("Auth Flow Test")
                .email(USERNAME + "@example.com")
                .password(passwordEncoder.encode(PASSWORD))
                .role(1)
                .build()));
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("ghd"))
                .andExpect(cookie().exists("ghd_rt"))
                .andReturn();
    }

    private Cookie cookieFrom(MvcResult result, String name) {
        Cookie cookie = result.getResponse().getCookie(name);
        assertThat(cookie).as("cookie " + name).isNotNull();
        return cookie;
    }

    @Test
    void loginSetsHttpOnlyAccessAndRefreshCookies() throws Exception {
        MvcResult result = login();

        assertThat(result.getResponse().getCookie("ghd").isHttpOnly()).isTrue();
        assertThat(result.getResponse().getCookie("ghd_rt").isHttpOnly()).isTrue();
    }

    @Test
    void validAccessTokenAuthenticatesUntilLogoutThenBlacklistRejectsIt() throws Exception {
        Cookie accessCookie = cookieFrom(login(), "ghd");

        // Token còn hợp lệ -> phải qua được authorizeHttpRequests (không bị 401).
        mockMvc.perform(put(PROTECTED_PROBE_PATH).cookie(accessCookie))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));

        mockMvc.perform(delete("/api/v1/auth/logout").cookie(accessCookie))
                .andExpect(status().isOk());

        // Cùng token cũ, sau logout phải bị từ chối ở tầng filter (blacklist) -> 401.
        mockMvc.perform(put(PROTECTED_PROBE_PATH).cookie(accessCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenRotatesSoThePreviousOneIsRejected() throws Exception {
        Cookie firstRefreshCookie = cookieFrom(login(), "ghd_rt");

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("ghd"))
                .andExpect(cookie().exists("ghd_rt"))
                .andReturn();

        // Refresh token mới (rotate) phải khác refresh token vừa dùng.
        Cookie rotatedRefreshCookie = cookieFrom(refreshResult, "ghd_rt");
        assertThat(rotatedRefreshCookie.getValue()).isNotEqualTo(firstRefreshCookie.getValue());

        // Dùng lại refresh token CŨ (đã rotate) -> phải bị từ chối.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstRefreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshTokenSoItCanNoLongerMintNewAccessTokens() throws Exception {
        MvcResult loginResult = login();
        Cookie accessCookie = cookieFrom(loginResult, "ghd");
        Cookie refreshCookie = cookieFrom(loginResult, "ghd_rt");

        mockMvc.perform(delete("/api/v1/auth/logout").cookie(accessCookie, refreshCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }
}
