# Checklist — sửa endpoint/auth/security

- [ ] Endpoint mới nằm đúng chain mong muốn (`/api/v1/**`/`/admin/v1/**` = JWT stateless,
      còn lại = session)?
- [ ] Nếu cần public: đã thêm đúng 1 chỗ trong `PUBLIC_URLS`, đã cân nhắc dữ liệu trả về
      có nhạy cảm không, đã hỏi người dùng nếu không chắc?
- [ ] Không thêm `Filter`/`OncePerRequestFilter` mới mà quên tắt auto-registration nếu
      filter đó chỉ nên chạy trên 1 chain (xem bẫy JwtAuthenticationFilter ở SKILL.md)?
- [ ] Không đọc/ghi `HttpSession` trong code chạy dưới `/api/v1/**`?
- [ ] Không đổi `PasswordEncoder`, `hideUserNotFoundExceptions`, hay gộp 2 filter chain
      mà không hỏi trước?
- [ ] Đã chạy `ApiStatelessSessionTest`, `SpringSessionRedisTest` (nếu đổi session/cache
      config liên quan) — `.\mvnw.cmd test -Dtest=ApiStatelessSessionTest,SpringSessionRedisTest`?
- [ ] Đã gọi subagent QA ([.claude/agents/ghd-qa.md](../../agents/ghd-qa.md)) trước khi
      coi thay đổi bảo mật là xong?
