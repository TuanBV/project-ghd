package guru.springframework.ghd.utils;

import guru.springframework.ghd.constants.RequestHeaderNames;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CookiesUtil {

    public static String getAuthToken(HttpServletRequest httpReq) {
        // HEADER
        final String authHeader = httpReq.getHeader(RequestHeaderNames.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(RequestHeaderNames.BEARER)) {
            return authHeader.substring(7);
        }

        // Cookie
        return Optional.ofNullable(httpReq.getCookies()).stream().flatMap(Arrays::stream)
                .filter(c -> c.getName().equals(RequestHeaderNames.COOKIE_TOKEN_NAME)).findFirst()
                .map(Cookie::getValue)
                .orElse(StringUtils.EMPTY);
    }

    public static String getCookieValue(HttpServletRequest httpRequest, String name) {
        return Optional.ofNullable(httpRequest.getCookies()).stream().flatMap(Arrays::stream)
                .filter(c -> c.getName().equals(name)).findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    /** Xoá cookie access + refresh token (đúng 2 cookie auth, không đụng cookie khác như session). */
    public static void deleteCookies(HttpServletResponse httpResponse) {
        httpResponse.addCookie(expiredCookie(RequestHeaderNames.COOKIE_TOKEN_NAME, "/"));
        httpResponse.addCookie(expiredCookie(RequestHeaderNames.COOKIE_REFRESH_TOKEN_NAME, "/api/v1/auth"));
    }

    private static Cookie expiredCookie(String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath(path);
        cookie.setMaxAge(0);
        return cookie;
    }

    public static Cookie createCookieResponse(String name, String token, int maxAgeSeconds,
                                              boolean secure, String path) {
        Cookie cookie = new Cookie(name, token);
        cookie.setHttpOnly(true);
        cookie.setPath(path);
        cookie.setSecure(secure);
        cookie.setMaxAge(maxAgeSeconds);
        return cookie;
    }

}
