package guru.springframework.ghd.security;

import guru.springframework.ghd.constants.RequestHeaderNames;
import guru.springframework.ghd.services.CustomerUserDetailsService;
import guru.springframework.ghd.services.JwtService;
import guru.springframework.ghd.services.TokenStoreService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomerUserDetailsService customerUserDetailsService;
    private final TokenStoreService tokenStoreService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        int maxPayloadLength = 10 * 1024; // 10KB
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, maxPayloadLength);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            String token = getJwtFromCookie(requestWrapper);

            if (StringUtils.hasText(token) && jwtService.validateToken(token)
                    && JwtService.TYPE_ACCESS.equals(jwtService.extractType(token))
                    && !tokenStoreService.isAccessTokenBlacklisted(jwtService.extractJti(token))) {
                String username = jwtService.getUsernameFromJWT(token);
                UserDetails userDetails = customerUserDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(requestWrapper));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                new RequestAttributeSecurityContextRepository().saveContext(SecurityContextHolder.getContext(), requestWrapper, responseWrapper);
            }

            filterChain.doFilter(requestWrapper, responseWrapper);

        } finally {
            logRequestResponse(requestWrapper, responseWrapper);

            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequestResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
        String contentType = response.getContentType();
        boolean isJson = contentType != null && contentType.contains("application/json");

        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format("\n[HTTP %s] %s -> Status: %d",
                request.getMethod(), request.getRequestURI(), response.getStatus()));

        // Chỉ log Request Body nếu có dữ liệu
        byte[] requestBody = request.getContentAsByteArray();
        if (requestBody.length > 0) {
            logMessage.append("\n[Req Body]: ").append(new String(requestBody));
        }

        // XỬ LÝ RESPONSE BODY GỌN GÀNG:
        if (isJson) {
            // Nếu là JSON thì in ra để debug
            logMessage.append("\n[Res Body]: ").append(new String(response.getContentAsByteArray()));
        } else {
            // Nếu là HTML/Template thì chỉ thông báo là trả về Template
            logMessage.append("\n[Res Type]: HTML/Template (Content hidden to keep log clean)");
        }

        log.info(logMessage.toString());
    }

    private String getJwtFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> RequestHeaderNames.COOKIE_TOKEN_NAME.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}