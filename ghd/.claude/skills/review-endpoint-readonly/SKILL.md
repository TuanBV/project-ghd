---
name: review-endpoint-readonly
description: "Task-template: review read-only 1 endpoint/API GHD (không sửa code) đối chiếu ranh giới layer + bảo mật + validation. Dùng subagent QA, điều hướng tới skill layering/security."
---

# Task-template: review read-only 1 endpoint — GHD

Review-only: **không Edit/Write code**. Nếu phát hiện vấn đề cần sửa, báo cáo lại cho
người dùng/agent chính thay vì tự sửa trong lúc review.

## Checklist review

- [ ] **Layer**: controller có gọi thẳng `repositories/*` không (vi phạm ranh giới) —
      xem [.claude/skills/spring-boot-layering/SKILL.md](../spring-boot-layering/SKILL.md).
      Nếu vi phạm, hook `.claude/hooks/layer-boundary.cjs` lẽ ra đã chặn lúc code được
      ghi — nếu review thấy vi phạm mà hook không bắt được, đó tự nó là 1 finding
      (hook có lỗ hổng, cần báo lại).
- [ ] **Bảo mật**: endpoint có nằm trong `PUBLIC_URLS` không, và nếu có, dữ liệu trả về
      có thực sự nên public không? Chain đúng loại (JWT stateless vs session) chưa?
      Xem [.claude/skills/spring-security-ghd/SKILL.md](../spring-security-ghd/SKILL.md).
- [ ] **Validation**: request DTO có `@Valid` ở param controller chưa (`@Valid
      @ModelAttribute`/`@Valid @RequestBody`)? Thiếu `@Valid` từng là bug thật gặp ở
      project liên quan (xem ghi chú trong commit lịch sử về `RequestValidationTest`) —
      luôn kiểm tra kỹ điểm này.
- [ ] **Response**: dùng `BaseController.ok()/created()/ng()/noContent()` và
      `ApiResponse<T>` nhất quán, không tự trả `ResponseEntity` tay không lý do.
- [ ] **Cache**: endpoint đọc dữ liệu ít đổi có nên `@Cacheable` chưa, và nếu đã có,
      TTL/cache name có đúng quy ước không? Xem
      [.claude/skills/redis-cache-session/SKILL.md](../redis-cache-session/SKILL.md).
- [ ] **N+1/query chậm**: nếu nghi ngờ, không tự sửa trong review-only, chuyển sang
      [.claude/skills/optimize-slow-query/SKILL.md](../optimize-slow-query/SKILL.md)
      như 1 task riêng.

## Cách chạy

Gọi subagent [.claude/agents/ghd-qa.md](../../agents/ghd-qa.md) với đúng đường dẫn
endpoint/controller cần review — subagent chỉ đọc/chạy lệnh kiểm tra, không sửa code,
trả về `VERDICT`/`FINDINGS`/`SKIPPED` theo format cố định.
