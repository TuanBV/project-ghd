---
name: implement-feature-end-to-end
description: "Task-template: quy trình thêm 1 feature xuyên suốt layer cho GHD (entity → migration → repo → service → dto/mapper → controller → view). Không chứa quy tắc kỹ thuật riêng — chỉ điều hướng tới skill kỹ thuật liên quan."
---

# Task-template: implement 1 feature end-to-end — GHD

Đây là **quy trình**, không phải nguồn quy tắc kỹ thuật — nếu có gì mâu thuẫn với skill
kỹ thuật bên dưới, skill kỹ thuật thắng (xem `CLAUDE.md` §1).

## Quy trình

1. **Xác định phạm vi thay đổi schema**: cần bảng/cột mới? →
   [.claude/skills/flyway-mysql/SKILL.md](../flyway-mysql/SKILL.md) (tạo migration
   mới, không sửa migration cũ).
2. **Layer**: entity → repository → service (interface + impl) → dto (theo feature) →
   mapper → controller. Bảng ranh giới đầy đủ + mẫu controller chuẩn:
   [.claude/skills/spring-boot-layering/SKILL.md](../spring-boot-layering/SKILL.md) +
   [checklist.md](../spring-boot-layering/checklist.md).
3. **Bảo mật**: endpoint mới vào chain nào, có cần public không? →
   [.claude/skills/spring-security-ghd/SKILL.md](../spring-security-ghd/SKILL.md).
4. **Cache**: dữ liệu đọc nhiều/ít đổi (danh mục, banner, sản phẩm theo slug...)? →
   [.claude/skills/redis-cache-session/SKILL.md](../redis-cache-session/SKILL.md).
5. **Sự kiện async** (thông báo, email, tracking...)? →
   [.claude/skills/kafka-events/SKILL.md](../kafka-events/SKILL.md).
6. **Giao diện** (admin và/hoặc client)? →
   [.claude/skills/thymeleaf-tailwind-alpine/SKILL.md](../thymeleaf-tailwind-alpine/SKILL.md).
7. **Test**: `.\mvnw.cmd test` sạch, thêm test mới nếu chạm DB/cache thật
   (`AbstractIntegrationTest`).
8. **Git**: branch mới, Conventional Commits, không attribution AI — xem `CLAUDE.md` §4
   (enforce bằng hook, không cần tự nhớ chi tiết).
9. **QA**: gọi [.claude/agents/ghd-qa.md](../../agents/ghd-qa.md) trước khi coi feature
   xong, theo vòng lặp ở `CLAUDE.md` §5.
10. Nếu feature gắn với 1 GitHub issue cụ thể, dùng
    [.claude/commands/work-issue.md](../../commands/work-issue.md) thay vì làm tay
    từng bước trên.
