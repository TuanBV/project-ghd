---
name: debug-failed-request
description: "Task-template: quy trình debug 1 request lỗi trong GHD, đi theo đúng luồng thật (filter → controller advice → controller → service → repository → DB). Không chứa quy tắc kỹ thuật riêng — điều hướng tới docs/PROCESSING-FLOW.md và skill liên quan."
---

# Task-template: debug 1 request lỗi — GHD

Bản đồ luồng xử lý thật (bắt buộc đọc trước, đừng đoán): [docs/PROCESSING-FLOW.md](../../../docs/PROCESSING-FLOW.md).

## Quy trình

1. **Xác định request đi qua chain nào**: `/api/v1/**`/`/admin/v1/**` (JWT stateless)
   hay `/**` (session)? → nếu nghi ngờ do auth/session, đọc
   [.claude/skills/spring-security-ghd/SKILL.md](../spring-security-ghd/SKILL.md)
   trước (đặc biệt bẫy JwtAuthenticationFilter double-registration — lỗi 500 kỳ lạ
   "request sau khi login bị lỗi đọc session" gần như chắc chắn liên quan tới đây).
2. **Bật log chi tiết tạm thời nếu cần** — `application.properties` đang set
   `logging.level.root=ERROR` và hạ mức 1 số package cụ thể xuống `INFO`/`WARN`. Có 2
   dòng `#Show SQL`/`#Show bind values` đã comment sẵn, chỉ cần bỏ comment tạm thời để
   xem SQL/bind values thật (nhớ comment lại trước khi commit).
   `CommonsRequestLoggingFilter` (`SecurityConfig.logFilter()`) log path/query/payload
   (giới hạn 10000 ký tự) cho mọi request — kiểm tra log app trước khi thêm log mới.
3. **Nếu lỗi ở tầng advice/model chung**: `GlobalCommon` (chỉ áp dụng
   `controllers.views`) hoặc `ApiExceptionHandler`/`GlobalExceptionHandler` — xem
   [.claude/skills/thymeleaf-tailwind-alpine/SKILL.md](../thymeleaf-tailwind-alpine/SKILL.md)
   để biết phạm vi `GlobalCommon`.
4. **Nếu lỗi liên quan cache trả dữ liệu cũ/rỗng**: kiểm tra TTL/namespace/behaviour
   clear-on-startup ở
   [.claude/skills/redis-cache-session/SKILL.md](../redis-cache-session/SKILL.md).
5. **Nếu lỗi liên quan đơn hàng/tồn kho**: đọc kỹ mục 2 trong
   `docs/PROCESSING-FLOW.md` (pessimistic lock `findByIdForUpdate`, publish Kafka sau
   commit) trước khi sửa — đây là chỗ dễ gây oversell nếu sửa sai.
6. **Nếu lỗi ở luồng async** (Telegram/email không tới): xem retry/no-DLQ ở
   [.claude/skills/kafka-events/SKILL.md](../kafka-events/SKILL.md) — lỗi ở đây không
   nên rollback nghiệp vụ chính, đừng "sửa" bằng cách gọi đồng bộ lại trong request
   thread.
7. Viết test tái hiện lỗi trước khi fix nếu khả thi (dùng
   `AbstractIntegrationTest` nếu cần DB/Redis thật).
8. Sau khi fix, gọi [.claude/agents/ghd-qa.md](../../agents/ghd-qa.md) để xác nhận
   độc lập trước khi coi là xong (`CLAUDE.md` §5).
