package guru.springframework.ghd.utils;

import guru.springframework.ghd.constants.RequestHeaderNames;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

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

    public static void deleteCookies(HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse) {
        // Cookie
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                Cookie deleteCookie = new Cookie(cookie.getName(), "");
                deleteCookie.setPath("/");
                deleteCookie.setMaxAge(0);
                httpResponse.addCookie(deleteCookie);
            }
        }
    }

    public static Cookie createCookieResponse(String token, int time, String domain, boolean secure) {
        Cookie cookie = new Cookie(RequestHeaderNames.COOKIE_TOKEN_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        //cookie.setDomain(domain);
        cookie.setSecure(false);
        cookie.setMaxAge(time);
        return cookie;
    }

}
