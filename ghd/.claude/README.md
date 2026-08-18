# `.claude/` — mục lục GHD

Rule cấp cao nhất nằm ở [`CLAUDE.md`](../CLAUDE.md) (root, tự nạp khi mở project).
File này chỉ là mục lục — xem `CLAUDE.md` §1 để biết thứ tự ưu tiên khi có mâu thuẫn.

**Phạm vi**: git repo root thật nằm ở thư mục cha (`project-ghd/`), chứa cả project
`CPMAP/` không liên quan. Toàn bộ nội dung dưới đây chỉ áp dụng cho `ghd/`.

## Nạp tự động (không cần gọi tường minh)

| File | Vai trò |
|---|---|
| `CLAUDE.md` (root) | Rule chính, tự nạp mỗi session |
| `.claude/settings.json` | Đấu nối 2 hook bên dưới + `permissions.deny` (.env) + tắt AI attribution |
| `.claude/hooks/git-safety.cjs` | `PreToolUse` trên `Bash` — chặn force-push, commit thẳng `main`, push thẳng `main`, amend/rebase lịch sử đã publish, sai Conventional Commits, có `Co-Authored-By` AI |
| `.claude/hooks/layer-boundary.cjs` | `PostToolUse` trên `Edit`/`Write` — chặn `controllers/**` import `repositories.*` trực tiếp |

## Skill kỹ thuật (đọc trước khi code phần liên quan)

| Skill | Đọc trước khi... |
|---|---|
| [`spring-boot-layering`](skills/spring-boot-layering/SKILL.md) | Thêm/sửa entity, repository, service, controller, DTO, mapper |
| [`spring-security-ghd`](skills/spring-security-ghd/SKILL.md) | Đụng `SecurityConfig`, thêm endpoint, đổi auth/session/JWT |
| [`flyway-mysql`](skills/flyway-mysql/SKILL.md) | Thêm/sửa file trong `db/migration` |
| [`redis-cache-session`](skills/redis-cache-session/SKILL.md) | Thêm `@Cacheable`/`@CacheEvict`, đụng `CacheConfig`/`SessionConfig` |
| [`kafka-events`](skills/kafka-events/SKILL.md) | Thêm event/listener mới, đụng `KafkaTopicConfig`/`KafkaConsumerConfig` |
| [`thymeleaf-tailwind-alpine`](skills/thymeleaf-tailwind-alpine/SKILL.md) | Sửa template Thymeleaf, CSS Tailwind, Alpine.js |
| [`vnpay-payment`](skills/vnpay-payment/SKILL.md) | Đụng `PaymentController`/`PaymentServiceImpl`/`VnpayServiceImpl` (thanh toán thẻ/trả góp) |

## Task-template (điều hướng, gọi khi nhiệm vụ khớp loại này)

| Skill | Dùng khi... |
|---|---|
| [`implement-feature-end-to-end`](skills/implement-feature-end-to-end/SKILL.md) | Thêm 1 feature mới xuyên nhiều layer |
| [`debug-failed-request`](skills/debug-failed-request/SKILL.md) | Debug 1 request/luồng đang lỗi |
| [`optimize-slow-query`](skills/optimize-slow-query/SKILL.md) | Tối ưu 1 query/endpoint chậm |
| [`review-endpoint-readonly`](skills/review-endpoint-readonly/SKILL.md) | Review read-only 1 endpoint, không sửa code |

## Agent (gọi qua Agent tool)

| Agent | Vai trò | Quyền |
|---|---|---|
| [`ghd-qa`](agents/ghd-qa.md) | QA độc lập — chạy test/kiểm tra, báo `VERDICT/FINDINGS/SKIPPED` | Read, Grep, Glob, Bash — **không Edit/Write** |
| [`ghd-issue-worker`](agents/ghd-issue-worker.md) | Xử lý 1 GitHub issue trọn vẹn qua Agent tool | Trỏ lại `/work-issue`, không có quy trình riêng |

## Slash command

| Command | Vai trò |
|---|---|
| [`/work-issue <số-issue>`](commands/work-issue.md) | Đọc issue → branch → sửa → test → QA → comment → commit (Conventional Commits, không AI attribution) → push → PR |

## Tài liệu tham chiếu

| File | Nội dung |
|---|---|
| [`DEPENDENCY_ALLOWLIST.md`](DEPENDENCY_ALLOWLIST.md) | Dependency hợp lệ từ `pom.xml`/`package.json` thật + bất thường cần hỏi trước khi đổi |
| [`../docs/PROJECT_INDEX.md`](../docs/PROJECT_INDEX.md) | Cấu trúc source, route index, entity (đã có sẵn trước khi xây bộ kit này) |
| [`../docs/PROCESSING-FLOW.md`](../docs/PROCESSING-FLOW.md) | Sequence diagram luồng request/order/user/Kafka/login |
| [`../docs/DOCKER.md`](../docs/DOCKER.md) | Build/run/vận hành Docker Compose |

## Vòng lặp QA (tóm tắt, chi tiết ở `CLAUDE.md` §5)

Agent chính tự kiểm tra → gọi `ghd-qa` → nếu `FAIL` thì tự sửa rồi gọi lại → tối đa
**3 lần** → escalate cho người dùng nếu vẫn `FAIL`. Miễn vòng lặp: docs-only, rename,
config nhỏ không đổi hành vi.

## Phát hiện quan trọng khi khảo sát (không thuộc bộ kit, cần bạn quyết định)

`.env` đang **được track trong git** (`git ls-files` xác nhận), không nằm trong
`.gitignore`. `docs/DOCKER.md` chỉ ghi nhận các secret cũ trong
`application.properties` (đã rotate) — không đề cập việc `.env` hiện tại vẫn nằm trong
git. Nếu file này đang chứa secret thật, mọi secret trong đó cần được coi là đã lộ và
cần rotate, tương tự các secret cũ. Đây là quyết định của bạn (gỡ khỏi tracking, thêm
vào `.gitignore`, rotate secret) — bộ kit này chỉ thêm `permissions.deny` chặn Claude
đọc/sửa `.env` trực tiếp như một lớp phòng vệ tạm thời, không tự sửa vấn đề gốc.
