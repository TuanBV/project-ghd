package guru.springframework.ghd.config;

import guru.springframework.ghd.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import java.util.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    // JwtAuthenticationFilter is a bare @Component, so Spring Boot's servlet-filter
    // auto-registration would otherwise also install it as a generic filter for "/*",
    // in addition to the explicit addFilterBefore() below that wires it into
    // adminApiSecurityFilterChain. That meant it also ran for clientSecurityFilterChain
    // requests (session policy IF_REQUIRED, not stateless) - so an admin's JWT cookie
    // got authenticated there too, and Spring Security's default session-based
    // SecurityContextRepository for that chain then persisted the resulting
    // Authentication into the Redis-backed HttpSession. Jackson 3 (see SessionConfig)
    // has no compatible creator for UsernamePasswordAuthenticationToken, so every
    // following request carrying that session cookie 500'd trying to read it back.
    // Disabling the auto-registration keeps this filter scoped to exactly the chain
    // it's added to below.
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    public static final String[] PUBLIC_URLS = {
            // --- 1. Hệ thống & Xác thực ---
            "/api/v1/auth/**",
            "/admin/v1/sign-in",
            "/api/v1/product/sync",
            "/api/v1/analytics/track",
            "/common/**",

            // --- 2. Các trang Web Client (Giao diện) ---
            "/",
            "/ve-chung-toi",
            "/lien-he",
            "/gio-hang/**",
            "/san-pham/**",
            "/tin-tuc/**",
            "/chinh-sach/**",

            // --- 3. API Công khai cho AJAX (KHÔNG ĐƯỢC THIẾU) ---
            "/api/v1/order/**",
            // VNPay IPN (server-to-server, không JWT/session) + return URL (trình
            // duyệt) + status (poll đọc) - bảo mật của IPN nằm ở verify chữ ký trong
            // PaymentServiceImpl.handleIpn, không phải ở đây.
            "/api/v1/payment/**",
            "/api/v1/product/search/**",
            "/api/v1/news/list/**",
            "/api/v1/categories/**",     // <--- THÊM DÒNG NÀY ĐỂ HIỆN DANH MỤC
            "/api/v1/brands/**",         // <--- THÊM DÒNG NÀY ĐỂ HIỆN THƯƠNG HIỆU
            "/api/v1/review/**",

            // --- 4. Tài nguyên tĩnh (Static) ---
            "/static/**",
            "/images/**",
            "/admin/images/**",
            "/uploads/**",
            "/client/**",
            "/webjars/**",
            "/css/**",
            "/js/**",
            "/favicon.ico",
            "/fonts/**",
            "/robots.txt",
            "/sitemap.xml"
    };

    // --- 1. API & ADMIN ---
    @Bean
    @Order(1)
    public SecurityFilterChain adminApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/**", "/admin/v1/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable) // Giữ disable nếu bạn dùng JWT stateless hoàn toàn
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin) // Chống Clickjacking
                        .xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)) // Chống XSS
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // Nếu là API trả về 401, nếu là trang admin thì redirect
                            if (request.getRequestURI().startsWith("/api/v1/")) {
                                response.sendError(401, "Unauthorized");
                            } else {
                                response.sendRedirect("/admin/v1/sign-in");
                            }
                        })
                );
        return http.build();
    }

    // --- 2. CLIENT (WEBSITE) ---
    @Bean
    @Order(2)
    public SecurityFilterChain clientSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(true);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // Trong thực tế nên để domain cụ thể
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(List.of("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(false);
        return filter;
    }
}