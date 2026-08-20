package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.constants.RequestHeaderNames;
import guru.springframework.ghd.constants.enums.WebEnvironment;
import guru.springframework.ghd.dto.auth.LoginRequest;
import guru.springframework.ghd.dto.auth.LoginResponse;
import guru.springframework.ghd.entities.User;
import guru.springframework.ghd.services.CustomerUserDetailsService;
import guru.springframework.ghd.services.JwtService;
import guru.springframework.ghd.services.TokenStoreService;
import guru.springframework.ghd.utils.CookiesUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController extends BaseController {

    private final CustomerUserDetailsService customerUserDetailsService;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final TokenStoreService tokenStoreService;

    private final Environment environment;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpirationSeconds;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest param,
                                HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            param.getUsername(),
                            param.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User data = (User) authentication.getPrincipal();
            String token = issueTokens(data.getUsername(), httpResponse);
            return ok(LoginResponse.builder()
                    .data(data)
                    .token(token)
                    .build());
        } catch (UsernameNotFoundException ex) {
            log.error("Lỗi: Không tìm thấy người dùng: {}", param.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Tài khoản không tồn tại");
        } catch (BadCredentialsException ex) {
            log.error("Lỗi: Sai mật khẩu cho user: {}", param.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Mật khẩu không chính xác");
        } catch (Exception ex) {
            log.error("Lỗi hệ thống: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi xác thực");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String refreshToken = CookiesUtil.getCookieValue(httpRequest, RequestHeaderNames.COOKIE_REFRESH_TOKEN_NAME);

        if (!StringUtils.hasText(refreshToken)
                || !Boolean.TRUE.equals(jwtService.validateToken(refreshToken))
                || !JwtService.TYPE_REFRESH.equals(jwtService.extractType(refreshToken))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token không hợp lệ");
        }

        String username = jwtService.extractUsername(refreshToken);
        String jti = jwtService.extractJti(refreshToken);

        if (!tokenStoreService.isRefreshTokenValid(username, jti)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token đã bị thu hồi");
        }

        UserDetails userDetails;
        try {
            userDetails = customerUserDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            tokenStoreService.revokeRefreshToken(username, jti);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Tài khoản không tồn tại");
        }

        // Rotate: refresh token cũ chỉ dùng được đúng 1 lần.
        tokenStoreService.revokeRefreshToken(username, jti);
        String newAccessToken = issueTokens(userDetails.getUsername(), httpResponse);

        return ok(LoginResponse.builder()
                .data((User) userDetails)
                .token(newAccessToken)
                .build());
    }

    @DeleteMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String accessToken = CookiesUtil.getCookieValue(httpRequest, RequestHeaderNames.COOKIE_TOKEN_NAME);
        if (StringUtils.hasText(accessToken) && Boolean.TRUE.equals(jwtService.validateToken(accessToken))) {
            tokenStoreService.blacklistAccessToken(jwtService.extractJti(accessToken),
                    jwtService.getRemainingSeconds(accessToken));
        }

        String refreshToken = CookiesUtil.getCookieValue(httpRequest, RequestHeaderNames.COOKIE_REFRESH_TOKEN_NAME);
        if (StringUtils.hasText(refreshToken) && Boolean.TRUE.equals(jwtService.validateToken(refreshToken))) {
            tokenStoreService.revokeRefreshToken(jwtService.extractUsername(refreshToken), jwtService.extractJti(refreshToken));
        }

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        CookiesUtil.deleteCookies(httpResponse);
        return ok(null);
    }

    /** Phát access + refresh token mới, đăng ký refresh token vào whitelist, set cả 2 cookie. */
    private String issueTokens(String username, HttpServletResponse httpResponse) {
        String accessToken = jwtService.generateAccessToken(username);
        String refreshToken = jwtService.generateRefreshToken(username);

        tokenStoreService.registerRefreshToken(username, jwtService.extractJti(refreshToken),
                refreshTokenExpirationSeconds);

        boolean secure = !Arrays.asList(environment.getActiveProfiles())
                .contains(WebEnvironment.DEVELOPMENT);

        httpResponse.addCookie(CookiesUtil.createCookieResponse(
                RequestHeaderNames.COOKIE_TOKEN_NAME, accessToken,
                (int) jwtService.getRemainingSeconds(accessToken), secure, "/"));
        httpResponse.addCookie(CookiesUtil.createCookieResponse(
                RequestHeaderNames.COOKIE_REFRESH_TOKEN_NAME, refreshToken,
                (int) refreshTokenExpirationSeconds, secure, "/api/v1/auth"));

        return accessToken;
    }
}
