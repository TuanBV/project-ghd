package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.constants.enums.WebEnvironment;
import guru.springframework.ghd.dto.auth.LoginRequest;
import guru.springframework.ghd.dto.auth.LoginResponse;
import guru.springframework.ghd.dto.auth.TokenInfoResponse;
import guru.springframework.ghd.entities.User;
import guru.springframework.ghd.services.AuthenticationService;
import guru.springframework.ghd.services.JwtService;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController extends BaseController {

    private final AuthenticationService authService;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final Environment environment;

    @Value("${cookie.expiration.default}")
    private int expirationTimeDefault;

    @Value("${cookie.domain}")
    private String domain;

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
            String token = setCookie(data, httpResponse);
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

    @DeleteMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication, HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // SecurityContext
        SecurityContext context = SecurityContextHolder.getContext();
        SecurityContextHolder.clearContext();
        context.setAuthentication(null);

        // Delete Cookie
        CookiesUtil.deleteCookies(httpRequest, httpResponse);
        return ok(null);
    }


    private String setCookie(User userResponse, HttpServletResponse httpResponse) {
        TokenInfoResponse tokenInfoResponse = new TokenInfoResponse();
        tokenInfoResponse.setUsername(userResponse.getUsername());
        String token = jwtService.generateToken(tokenInfoResponse);


        boolean secure = !Arrays.asList(environment.getActiveProfiles())
                .contains(WebEnvironment.DEVELOPMENT);
        // Set cookie
        httpResponse.addCookie(
                CookiesUtil.createCookieResponse(token, expirationTimeDefault, domain, secure));

        return token;
    }
}
