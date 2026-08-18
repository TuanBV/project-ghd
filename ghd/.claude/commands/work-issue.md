---
description: Xử lý trọn vẹn 1 GitHub issue của project GHD — đọc issue, tạo branch, sửa, test, comment kết quả, commit/push an toàn, mở PR.
argument-hint: <issue-number>
---

Xử lý issue **#$1** trên `github.com/TuanBV/project-ghd` theo đúng quy trình dưới đây.
Không bỏ bước, không tự ý rút gọn trừ khi có lý do rõ ràng và đã nói với người dùng.

## 1. Đọc issue

```
gh issue view $1 --repo TuanBV/project-ghd --comments
```

Đọc kỹ mô tả + toàn bộ comment (yêu cầu có thể đã được làm rõ/đổi trong comment, không
chỉ nằm trong mô tả gốc). Nếu issue thiếu thông tin để bắt tay vào việc (không rõ hành
vi mong muốn, không rõ phạm vi), dừng lại và hỏi người dùng — đừng tự suy đoán yêu cầu
nghiệp vụ.

## 2. Đối chiếu yêu cầu với code hiện tại

Trước khi sửa, xác định issue chạm layer/tech nào để đọc đúng skill kỹ thuật liên quan
(không đọc tất cả, chỉ đọc cái liên quan):

- Đụng entity/service/repository/controller → `.claude/skills/spring-boot-layering/SKILL.md`
- Đụng auth/endpoint public/private → `.claude/skills/spring-security-ghd/SKILL.md`
- Đụng schema DB → `.claude/skills/flyway-mysql/SKILL.md`
- Đụng cache/session → `.claude/skills/redis-cache-session/SKILL.md`
- Đụng event/thông báo async → `.claude/skills/kafka-events/SKILL.md`
- Đụng template/CSS → `.claude/skills/thymeleaf-tailwind-alpine/SKILL.md`

Nếu issue mô tả 1 feature end-to-end, dùng
`.claude/skills/implement-feature-end-to-end/SKILL.md` làm khung quy trình.

## 3. Tạo branch

```
git checkout -b <type>/issue-$1-<mo-ta-ngan>
```

`type` theo Conventional Commits (`feat|fix|docs|style|refactor|perf|test|build|ci|chore`).
**Không commit thẳng vào `main`** — hook `.claude/hooks/git-safety.cjs` sẽ chặn nếu quên
bước này.

## 4. Sửa code

Theo đúng skill kỹ thuật đã xác định ở bước 2. Tuân thủ ranh giới layer/bảo mật ở
`CLAUDE.md` §2–3 — hook sẽ chặn nếu controller import repository trực tiếp.

## 5. Test

```powershell
.\mvnw.cmd test
```

Nếu issue có phạm vi hẹp, có thể thu hẹp bằng `-Dtest=<TestClass>` khi lặp nhanh,
nhưng **phải chạy full `test` ít nhất 1 lần** trước khi coi là xong.

## 6. Tick checklist liên quan

Nếu skill kỹ thuật liên quan có `checklist.md` (`spring-boot-layering`,
`spring-security-ghd`, `flyway-mysql`), rà lại từng mục trước khi qua bước tiếp theo.

## 7. QA

Gọi subagent `.claude/agents/ghd-qa.md` để review độc lập thay đổi vừa làm (trừ khi
thay đổi được miễn theo `CLAUDE.md` §5 — docs-only/rename/config nhỏ). Nếu `FAIL`, sửa
rồi gọi lại, tối đa 3 lần, quá 3 lần thì báo người dùng thay vì tự lặp tiếp.

## 8. Comment kết quả lên issue

```
gh issue comment $1 --repo TuanBV/project-ghd --body "<tóm tắt đã làm gì, test nào đã chạy, VERDICT của QA>"
```

## 9. Commit

Conventional Commits, không `Co-Authored-By` AI (hook enforce, nhưng vẫn tự viết đúng
ngay từ đầu để đỡ phải sửa lại):

```
git add <file cụ thể, không dùng -A/. mù quáng>
git commit -m "$(cat <<'EOF'
<type>(<scope>): <mô tả ngắn, liên quan tới issue #$1>
EOF
)"
```

## 10. Push + mở PR

```
git push -u origin <branch>
gh pr create --repo TuanBV/project-ghd --title "<type>(<scope>): <mô tả>" --body "$(cat <<'EOF'
## Summary
- Giải quyết #$1: <tóm tắt ngắn>

## Test plan
- [x] `.\mvnw.cmd test`
- [x] QA subagent: VERDICT ...
EOF
)"
```

**Không tự merge PR vào `main`** — merge là quyết định của người dùng/reviewer, hook
cũng chặn push thẳng `main`. Báo lại link PR cho người dùng và dừng ở đây.
