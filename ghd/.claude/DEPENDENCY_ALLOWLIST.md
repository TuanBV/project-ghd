# Dependency allowlist — GHD

Lấy trực tiếp từ `pom.xml` (Maven) và `package.json` (frontend tooling) tại thời điểm
xây dựng bộ kit này. Đây là allowlist — **không tự thêm dependency mới ngoài danh sách
này mà không hỏi người dùng trước**, kể cả khi có vẻ tiện lợi hay "ai cũng dùng".

## Maven (`pom.xml`)

Parent: `org.springframework.boot:spring-boot-starter-parent:4.0.1`, Java 17.

| Dependency | Version | Ghi chú |
|---|---|---|
| `spring-boot-starter-web` | (theo BOM 4.0.1) | |
| `spring-boot-h2console` | (theo BOM) | H2 console — kiểm tra trước khi giả định H2 là DB runtime, DB thật là MySQL |
| `spring-boot-starter-data-jpa` (+ `-test`) | (theo BOM) | |
| `spring-boot-starter-webmvc-test` | (theo BOM, test scope) | |
| `spring-boot-starter-data-redis` | (theo BOM) | |
| `spring-boot-starter-cache` | (theo BOM) | |
| `spring-boot-starter-session-data-redis` | (theo BOM) | Boot 4 tách autoconfig Session theo store — dùng starter này, không dùng raw `spring-session-data-redis` (không có autoconfig, sẽ không tạo `SessionRepository` bean) |
| `spring-boot-starter-actuator` | (theo BOM) | Chỉ expose `health,info` — xem `SecurityConfig` |
| `spring-boot-testcontainers` (+ `testcontainers:junit-jupiter`, `testcontainers:mysql`) | (theo BOM, test scope) | `testcontainers-bom` pin cứng `1.21.4` — comment trong `pom.xml` giải thích: BOM Spring Boot 4.0.1 kéo theo testcontainers-core 2.0.x nhưng module `junit-jupiter`/`mysql` chưa có bản 2.x trên Maven Central, nên phải pin cả BOM về 1.21.4 để nhất quán. **Không tự bump chỉ 1 module lên 2.x** trong khi core vẫn 1.x |
| `spring-boot-starter-validation` | **`4.0.0`** (pin tường minh) | ⚠️ pin thấp hơn parent `4.0.1` — không rõ lý do ghi lại, hỏi trước khi đổi |
| `org.jsoup:jsoup` | `1.17.2` | |
| `org.apache.poi:poi-ooxml` | `5.2.3` | Export/import Excel |
| `spring-boot-starter-thymeleaf` | **`4.0.0`** (pin tường minh) | ⚠️ cùng bất thường như validation ở trên |
| `com.mysql:mysql-connector-j` | (theo BOM) | |
| `spring-boot-starter-security` | **`3.4.3`** (pin tường minh) | ⚠️ **bất thường rõ nhất**: pin về nhánh Spring Boot 3.x trong khi parent project là 4.0.1 — không có comment giải thích lý do trong `pom.xml`. Có thể là tương thích ngược cố ý, có thể là sai sót chưa dọn. **Không tự "sửa cho gọn" bằng cách bỏ version tường minh để BOM tự quản** mà không hỏi người dùng và test kỹ — đổi version Security có thể đổi hành vi `SecurityFilterChain`/API |
| `thymeleaf-extras-springsecurity6` | (theo BOM) | |
| `flyway-mysql` + `spring-boot-starter-flyway` | (theo BOM) | |
| `lombok` | `1.18.42` (provided) | |
| `mapstruct` + `mapstruct-processor` | `1.5.2.Final` | Xem thứ tự annotation processor trong `.claude/skills/spring-boot-layering/SKILL.md` |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | `0.11.5` | JWT cho `JwtAuthenticationFilter`/`JwtService` |
| `commons-lang3` | (theo BOM) | |
| `scrimage-core` + `scrimage-webp` | `4.1.1` | Xử lý ảnh (resize/convert webp cho upload) |
| `spring-boot-starter-mail` | (theo BOM) | SMTP Gmail, xem `EmailService` |
| `spring-boot-starter-kafka` | (theo BOM) | |

**Đã comment tắt, không tự bật lại**: `spring-boot-starter-webflux` (dòng 119-122 trong
`pom.xml`) — dự án dùng Servlet MVC (Spring MVC), không phải reactive stack. Nếu 1 yêu
cầu có vẻ cần WebFlux, hỏi người dùng trước khi bật lại, đừng tự thêm.

## npm (`package.json`, root `ghd/`)

Chỉ dùng cho build frontend lúc build-time (xem
`.claude/skills/thymeleaf-tailwind-alpine/SKILL.md`), không chạy lúc runtime.

| Dependency | Version | Ghi chú |
|---|---|---|
| `@tailwindcss/cli` | `^4.3.0` | |
| `tailwindcss` | `^4.3.0` | Tailwind v4 (cấu hình CSS-first, khác v3) |
| `alpinejs` | `^3.14.9` | Chỉ lấy file `dist/cdn.min.js`, không import theo module |

## Việc KHÔNG được làm

- Không thêm dependency ORM/DB khác ngoài Spring Data JPA + MySQL (vd không tự thêm
  jOOQ, MyBatis) mà không hỏi.
- Không thêm framework frontend khác (React/Vue/Angular) — client hiện là Thymeleaf +
  Alpine.js + Tailwind, đã là quyết định kiến trúc, không tự đổi.
- Không tự nâng major version của bất kỳ dependency nào ở bảng trên mà không chạy
  `.\mvnw.cmd test` xác nhận và không hỏi người dùng trước, đặc biệt 3 dòng đã đánh dấu
  ⚠️ ở trên.
