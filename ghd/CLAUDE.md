# GHD — Rule chính cho Claude Code

GHD là ứng dụng Spring Boot 4.0.1 / Java 17 (Maven) cho một site thương mại điện tử:
có admin dashboard (Thymeleaf) và trang khách hàng (Thymeleaf), API JSON riêng dưới
`/api/v1/**`, MySQL (Flyway), Redis (cache + session), Kafka (event thông báo async).
Package gốc: `guru.springframework.ghd`.

Tài liệu cấu trúc/luồng xử lý chi tiết đã có sẵn, đọc trước khi chạm code:

- [docs/PROJECT_INDEX.md](docs/PROJECT_INDEX.md) — cấu trúc source, route index, entity, layer theo feature
- [docs/PROCESSING-FLOW.md](docs/PROCESSING-FLOW.md) — sequence diagram request/order/user/Kafka/login
- [docs/DOCKER.md](docs/DOCKER.md) — build/run/vận hành Docker Compose

File này không lặp lại nội dung 3 tài liệu trên — chỉ nêu **quy tắc bắt buộc** và **điều hướng**.

**Lưu ý phạm vi repo**: git repo root thực tế nằm ở thư mục cha (`project-ghd/`), chứa
cả project `CPMAP/` (pricing/crawler) hoàn toàn độc lập, không liên quan tới `ghd/`.
Toàn bộ rule/skill/hook trong file này và `.claude/` chỉ áp dụng cho cây thư mục `ghd/`.
**Ranh giới này được enforce cứng bằng hook** `.claude/hooks/scope-guard.cjs` — chặn
Read/Edit/Write/Glob/Grep/Bash đọc/ghi bất kỳ đường dẫn nào ngoài `ghd/` (kể cả
`CPMAP/`), trừ hạ tầng vận hành của Claude Code (thư mục temp/scratchpad, `~/.claude/`).
Nếu 1 tác vụ thật sự cần đọc file ngoài `ghd/`, hook sẽ chặn — dừng lại và hỏi người
dùng thay vì tìm cách lách qua.

## 1. Phân cấp nguồn chân lý (bắt buộc, không mơ hồ)

Khi có mâu thuẫn, thứ tự ưu tiên là:

1. **Hook thật trong `.claude/hooks/`** (đã enforce bằng code, chạy qua `PreToolUse`/
   `PostToolUse`) — nếu hook chặn một hành động, đó là quyết định vận hành cuối cùng.
   Nếu bạn (Claude) thấy hook chặn nhầm, **dừng lại và hỏi người dùng** thay vì tìm cách
   lách qua hook (đổi câu lệnh để né regex, tắt hook, dùng `--no-verify`...).
2. **File này (`CLAUDE.md`)** — quy tắc cấp dự án, thắng mọi skill nếu mâu thuẫn.
3. **Skill kỹ thuật** (`.claude/skills/<tech>/SKILL.md`) — quy tắc chi tiết theo từng
   công nghệ, đọc **trước khi** viết code liên quan.
4. **Task-template skill** (`.claude/skills/implement-*`, `debug-*`, `optimize-*`,
   `review-*`) — chỉ điều hướng tới các skill kỹ thuật liên quan, không tự có thẩm quyền
   kỹ thuật riêng. Nếu một task-template mô tả sai khác với skill kỹ thuật nó trỏ tới,
   skill kỹ thuật thắng.
5. **`docs/*.md`** — mô tả kiến trúc/luồng hiện có, dùng để hiểu bối cảnh, không phải quy
   tắc bắt buộc.

Mục lục đầy đủ: [.claude/README.md](.claude/README.md).

## 2. Ranh giới bảo mật — KHÔNG ĐƯỢC VI PHẠM

`config/SecurityConfig.java` định nghĩa **2 filter chain tách biệt** theo thứ tự `@Order`:

- `adminApiSecurityFilterChain` (`@Order(1)`, khớp `/api/v1/**` và `/admin/v1/**`):
  JWT stateless (`SessionCreationPolicy.STATELESS`), `PUBLIC_URLS` được `permitAll()`,
  còn lại yêu cầu `authenticated()`. `JwtAuthenticationFilter` được add thủ công vào
  đúng chain này qua `addFilterBefore`.
- `clientSecurityFilterChain` (`@Order(2)`, khớp `/**`): session-based
  (`SessionCreationPolicy.IF_REQUIRED`), `permitAll()` toàn bộ — đây là trang public,
  không có JWT.

**Bẫy đã từng xảy ra thật** (xem comment trong `SecurityConfig.java`): nếu để Spring Boot
tự auto-register `JwtAuthenticationFilter` như một servlet filter thường (`@Component`),
nó chạy luôn cho cả `clientSecurityFilterChain`, khiến JWT admin bị auth nhầm trên chain
session, rồi Jackson 3 không serialize được `UsernamePasswordAuthenticationToken` vào
Redis session → mọi request sau đó 500. Vì vậy `jwtAuthenticationFilterRegistration`
**phải luôn `setEnabled(false)`** — đừng xóa bean này, đừng bật lại registration đó.

Quy tắc bắt buộc khi đổi endpoint/bảo mật:

- Thêm route mới vào `PUBLIC_URLS` = chủ động mở public, không cần đăng nhập. **Phải hỏi
  người dùng xác nhận trước** nếu route đó trả về dữ liệu không nên public (đơn hàng,
  thông tin user...).
- Không gộp 2 filter chain lại thành 1. Không đổi `securityMatcher` của chain nào sang
  phạm vi chồng lấn với chain kia.
- **Đề xuất kiểm chứng tự động** (chưa bắt buộc viết ngay, nhưng phải nêu khi bạn sửa
  `SecurityConfig`/`PUBLIC_URLS`): thêm/mở rộng test kiểu
  `src/test/java/guru/springframework/ghd/security/ApiStatelessSessionTest.java` để assert
  danh sách `PUBLIC_URLS` không vô tình chứa endpoint ghi dữ liệu nhạy cảm.

Chi tiết đầy đủ + checklist: [.claude/skills/spring-security-ghd/SKILL.md](.claude/skills/spring-security-ghd/SKILL.md).

## 3. Ranh giới layer — hợp đồng, không phải gợi ý

```
controllers/api (JSON) ─┐
controllers/views (Thymeleaf) ─┴──> services / services.impl ──> repositories ──> entities
                                        │
                                        └──> mappers (entity <-> dto)
```

| Từ | Đến | Được phép? |
|---|---|---|
| `controllers/**` | `services/*Service` (interface) | ✅ luôn luôn |
| `controllers/**` | `repositories/*Repository` | ❌ **cấm — enforce bằng hook** |
| `controllers/**` | `entities/*` | ⚠️ chỉ dùng lại nếu đã có tiền lệ (vd `GlobalCommon`, vài view controller) — DTO là mặc định cho API mới, không phải entity |
| `services/impl/**` | `repositories/*Repository` | ✅ luôn luôn |
| `services/impl/**` | `mappers/*Mapper` | ✅ luôn luôn (map entity → DTO trước khi trả ra ngoài service) |
| `dto/**` | `entities/**` | ❌ DTO không phụ thuộc ngược vào entity |

Rule "controller không import repository trực tiếp" đã đúng 100% với code hiện tại
(verify bằng grep khi xây bộ kit này — 0 vi phạm) nên được **enforce cứng bằng hook**
`.claude/hooks/layer-boundary.cjs` (PostToolUse trên Edit/Write file `controllers/**`).
Nếu hook báo vi phạm, sửa lại: chuyển logic cần thiết vào Service, controller chỉ gọi
Service.

Chi tiết layer + quy ước DTO theo feature + MapStruct/Lombok:
[.claude/skills/spring-boot-layering/SKILL.md](.claude/skills/spring-boot-layering/SKILL.md).

## 4. Git — quy trình bắt buộc (enforce bằng hook `.claude/hooks/git-safety.cjs`)

- **Không commit thẳng vào `main`.** Luôn tạo branch trước
  (`git checkout -b <type>/<mo-ta-ngan>`, type theo Conventional Commits:
  `feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert`). Đây là quy ước
  branch **đề xuất** — repo chưa có tiền lệ đặt tên branch trước đây.
- **Conventional Commits bắt buộc**: `type(scope)?: mô tả ngắn`. Hook reject commit
  message không đúng dạng.
- **Không có `Co-Authored-By` nhắc tới Claude/Anthropic** trong commit message — hook
  reject nếu phát hiện, tự bỏ dòng đó rồi commit lại.
- **Không force-push** (`--force`/`-f`/`--force-with-lease`) — bị chặn cứng, không có
  cách bypass hợp lệ trong workflow này.
- **Không amend/rebase commit đã push lên `origin/*`** — hook kiểm tra bằng
  `git merge-base --is-ancestor`, chặn nếu commit đã public.
- **Không push thẳng `main`** — merge vào `main` phải qua Pull Request (mở PR bằng
  `gh pr create`, không tự `git push origin <branch>:main`).
- **Giới hạn đã biết của hook**: hook kiểm tra branch hiện tại *trước khi* lệnh chạy,
  nên `git checkout -b feat/x && git commit -m "..."` gộp trong **1 lời gọi Bash** vẫn
  bị chặn (branch lúc kiểm tra vẫn là `main`) dù bản thân lệnh gộp đó an toàn. Luôn tách
  thành 2 lời gọi Bash riêng: tạo branch trước, commit sau — đã verify bằng test thật
  khi xây bộ kit này.
- Repo dùng **GitHub Issues** (`github.com/TuanBV/project-ghd`) để theo dõi việc cần làm.
  Quy trình xử lý 1 issue trọn vẹn: xem slash command
  [.claude/commands/work-issue.md](.claude/commands/work-issue.md).
- **Bảo vệ lớp 2 (khuyến nghị, chưa tự cấu hình)**: nên bật branch protection rule cho
  `main` trên GitHub (Settings → Branches) — require PR review, chặn force-push phía
  server. Hook ở đây chỉ là lớp chặn phía client.

## 5. Vòng lặp QA

Sau khi sửa code không tầm thường (thêm/sửa logic nghiệp vụ, endpoint, migration, cấu
hình bảo mật/cache/kafka), **agent chính** (bạn) phải:

1. Tự chạy kiểm tra cơ bản trước (`.\mvnw.cmd test` cho phần liên quan, hoặc test class
   cụ thể).
2. Gọi subagent QA độc lập [.claude/agents/ghd-qa.md](.claude/agents/ghd-qa.md) — agent
   này chỉ có quyền đọc + chạy lệnh kiểm tra, **không có Edit/Write**, không tự sửa code.
3. Đọc `VERDICT`/`FINDINGS` từ QA. Nếu `FAIL`, tự sửa rồi gọi lại QA.
4. Lặp tối đa **3 lần**. Sau 3 lần vẫn `FAIL`, dừng lại và báo cáo người dùng thay vì tự
   lặp vô hạn hoặc tự nới lỏng tiêu chí.

**Miễn vòng lặp QA** (không bắt buộc gọi QA subagent): thay đổi chỉ gồm
docs (`*.md`), đổi tên biến/method nội bộ không đổi hành vi, chỉnh format code, sửa nhỏ
trong `application.properties`/`.env.example` không đụng logic (vd thêm comment, đổi
default không ảnh hưởng secret thật).

## 6. Dependency — allowlist

Toàn bộ dependency hợp lệ lấy trực tiếp từ `pom.xml` + `package.json` — xem
[.claude/DEPENDENCY_ALLOWLIST.md](.claude/DEPENDENCY_ALLOWLIST.md) để biết danh sách đầy
đủ + các bất thường cần hỏi trước khi đổi (vd `spring-boot-starter-security` đang pin
`3.4.3` dù parent BOM là `4.0.1`). **Không tự thêm dependency mới** ngoài danh sách này
mà không hỏi người dùng trước — kể cả khi có vẻ "tiện".

## 7. Build/test/run — chỉ dùng lệnh có thật

```powershell
.\mvnw.cmd test              # chạy toàn bộ test (unit + Testcontainers MySQL/Redis)
.\mvnw.cmd spring-boot:run    # chạy app local (cần MySQL/Redis khả dụng theo application.properties)
npm run build:frontend        # build lại Tailwind CSS + copy Alpine.js vào static resources
docker compose up -d          # chạy full stack (app+mysql+redis+kafka) — xem docs/DOCKER.md
```

Không có lệnh lint/checkstyle/spotless nào được cấu hình trong `pom.xml` — đừng bịa ra
lệnh `mvn checkstyle:check` hay tương tự. Không có CI (`.github/workflows` rỗng) — mọi
kiểm tra hiện tại chạy local/qua QA subagent.

## 8. Mục lục skill/agent/command

Xem [.claude/README.md](.claude/README.md).
