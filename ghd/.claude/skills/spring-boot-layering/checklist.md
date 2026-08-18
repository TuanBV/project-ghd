# Checklist — thêm/sửa 1 feature theo layer GHD

Dùng khi thêm entity/feature mới hoặc sửa 1 feature hiện có xuyên nhiều layer.

- [ ] Entity (`entities/`) extends `BaseEntity` nếu cần id/audit/soft-delete chung
- [ ] Migration Flyway mới nếu đổi schema — xem
      [.claude/skills/flyway-mysql/SKILL.md](../flyway-mysql/SKILL.md), **không** sửa
      migration cũ đã áp dụng
- [ ] Repository (`repositories/`) — chỉ được gọi từ `services/impl`, không phải
      controller
- [ ] DTO Request/Response trong `dto/<feature>/` (feature-folder, không gộp chung 1
      file lớn)
- [ ] Mapper (`mappers/`) map entity ↔ DTO — chạy `.\mvnw.cmd compile` để MapStruct
      generate lại sau khi đổi field
- [ ] Service interface (`services/`) + impl (`services/impl/`) — logic nghiệp vụ và
      gọi repository nằm ở đây, không nằm trong controller
- [ ] Controller API (`controllers/api/`) extends `BaseController`, dùng
      `ok()/created()/ng()/noContent()`, trả `ApiResponse<T>`
- [ ] Nếu endpoint cần public (không login) → có chủ đích thêm vào `PUBLIC_URLS` trong
      `SecurityConfig` và đã cân nhắc rủi ro — xem
      [.claude/skills/spring-security-ghd/SKILL.md](../spring-security-ghd/SKILL.md)
- [ ] Nếu list hiển thị public/ít đổi (category, brand, banner, slider, policy,
      sys-param, product theo slug...) → cân nhắc `@Cacheable` theo
      [.claude/skills/redis-cache-session/SKILL.md](../redis-cache-session/SKILL.md),
      nhớ `@CacheEvict` đúng cache name khi ghi
- [ ] View Thymeleaf (`controllers/views/`, `templates/admin|client/`) nếu feature có
      giao diện quản trị/công khai
- [ ] Test: ít nhất 1 test dùng `AbstractIntegrationTest` (Testcontainers MySQL+Redis)
      nếu feature chạm DB/cache thật; xem test hiện có
      (`CategoryServiceCacheTest`, `CacheConfigTest`) làm mẫu
- [ ] `.\mvnw.cmd test` chạy sạch trước khi coi feature hoàn thành
- [ ] Nếu thay đổi không tầm thường → gọi subagent QA
      ([.claude/agents/ghd-qa.md](../../agents/ghd-qa.md)) trước khi commit, theo vòng
      lặp QA ở `CLAUDE.md` §5
