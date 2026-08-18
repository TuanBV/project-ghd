---
name: spring-security-ghd
description: Hai security filter chain của GHD (admin/API JWT stateless vs client session), PUBLIC_URLS, và bẫy JwtAuthenticationFilter double-registration. Đọc trước khi đụng SecurityConfig, thêm endpoint, hoặc đổi auth.
---

# Spring Security — GHD

File nguồn: `src/main/java/guru/springframework/ghd/config/SecurityConfig.java`.
Xem thêm sequence diagram đăng nhập/request trong
[docs/PROCESSING-FLOW.md](../../../docs/PROCESSING-FLOW.md) mục 1 và 5.

## 2 filter chain — không được gộp

```java
@Bean @Order(1) adminApiSecurityFilterChain(...)   // securityMatcher("/api/v1/**", "/admin/v1/**")
@Bean @Order(2) clientSecurityFilterChain(...)     // securityMatcher("/**")
```

| | `adminApiSecurityFilterChain` | `clientSecurityFilterChain` |
|---|---|---|
| Phạm vi | `/api/v1/**`, `/admin/v1/**` | `/**` (mọi request còn lại) |
| Session | `STATELESS` (JWT trong cookie, không session) | `IF_REQUIRED` (session Redis, cookie `GHDSESSION`) |
| Auth | `PUBLIC_URLS` → `permitAll()`, còn lại `authenticated()` | `permitAll()` toàn bộ |
| CSRF | disabled (chấp nhận vì JWT stateless) | disabled |
| Filter JWT | `addFilterBefore(jwtAuthenticationFilter, ...)` add thủ công | không có |

**`PUBLIC_URLS`** (`SecurityConfig.PUBLIC_URLS`) là danh sách route được `permitAll()`
trong chain admin/API — bao gồm auth, static resources, và một số API AJAX công khai
(`/api/v1/order/**`, `/api/v1/product/search/**`, `/api/v1/review/**`...). Thêm route
mới vào đây = chủ động mở public. Trước khi thêm:

1. Xác nhận route thật sự không cần định danh người dùng.
2. Nếu route trả dữ liệu nhạy cảm (đơn hàng của người khác, thông tin user...) →
   **hỏi người dùng trước khi thêm**, đừng tự quyết.
3. Đề xuất mở rộng test `ApiStatelessSessionTest` để giữ danh sách này không phình ra
   ngoài kiểm soát (chưa bắt buộc viết ngay, nhưng phải nêu trong PR/báo cáo).

## Bẫy JwtAuthenticationFilter double-registration (đã xảy ra thật)

`JwtAuthenticationFilter` là `@Component` thường — nếu để Spring Boot tự động đăng ký
nó như servlet filter (`FilterRegistrationBean` mặc định, áp dụng cho `/*`), nó chạy
**cả trên** `clientSecurityFilterChain`, không chỉ chain admin/API nó được add thủ công
vào. Hậu quả từng gặp: JWT admin bị authenticate luôn ở chain client (session-based),
`HttpSessionSecurityContextRepository` cố lưu `Authentication` đó vào Redis session,
nhưng Jackson 3 (`SessionConfig`) không có creator tương thích cho
`UsernamePasswordAuthenticationToken` → mọi request sau đó mang session cookie đó bị
500 khi đọc lại.

Fix hiện tại: bean `jwtAuthenticationFilterRegistration` set
`registration.setEnabled(false)` để tắt auto-registration, giữ filter chỉ chạy đúng 1
lần qua `addFilterBefore` trong `adminApiSecurityFilterChain`.

**Quy tắc**: không xóa bean `jwtAuthenticationFilterRegistration`, không đổi
`setEnabled(false)` thành `true`, không thêm filter mới kiểu `@Component` implement
`Filter`/`OncePerRequestFilter` mà không tự tắt auto-registration tương tự nếu filter
đó chỉ nên chạy trên 1 trong 2 chain.

## Session vs JWT — không trộn

- `/api/v1/**`, `/admin/v1/**`: xác thực bằng **JWT cookie**, không dùng session
  (`SessionCreationPolicy.STATELESS`).
- Phần còn lại (`/**`, tức trang Thymeleaf client + `GlobalCommon`): dùng **session
  Redis** thật (cookie `GHDSESSION`, xem `application.properties` mục `spring.session.*`).
- Đừng đọc/ghi `HttpSession` trong code chạy dưới `/api/v1/**` — chain đó stateless,
  session sẽ không tồn tại đáng tin cậy.

## Khác

- `CorsConfigurationSource`: hiện `allowedOrigins = List.of("*")` — có comment trong
  code ghi nhận đây là tạm thời ("trong thực tế nên để domain cụ thể"). Đừng tự thắt
  chặt/nới lỏng CORS mà không hỏi, vì có thể ảnh hưởng frontend đang chạy.
- `PasswordEncoder` là `BCryptPasswordEncoder` mặc định — không đổi thuật toán hash mà
  không có kế hoạch migrate password cũ.
- `AuthenticationProvider` set `hideUserNotFoundExceptions(true)` — không đổi thành
  `false` (tránh lộ thông tin tài khoản tồn tại hay không qua thông báo lỗi).

## Checklist

Xem [checklist.md](checklist.md) khi sửa endpoint/auth/security config.
