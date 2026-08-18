---
name: flyway-mysql
description: Quy ước migration Flyway/MySQL của GHD (naming, schema core, ddl-auto=validate, seed dump). Đọc trước khi thêm/sửa bất kỳ file trong src/main/resources/db/migration.
---

# Flyway + MySQL — GHD

Migration thật nằm ở `src/main/resources/db/migration/`, hiện tại **`V1` → `V26`**
(tăng dần theo thời gian, không liền mạch về chủ đề — vd `V16`, `V20`, `V23`, `V25` đều
là "alter product" ở các thời điểm khác nhau). Schema đích: `core` (xem
`spring.datasource.url` trong `application.properties`).

## Quy tắc bắt buộc

- **Không bao giờ sửa một file migration đã tồn tại/đã áp dụng** (`V1`...`V26` hiện
  tại). Flyway checksum-validate; sửa file cũ sẽ làm mọi môi trường khác (staging,
  máy đồng nghiệp, container đã chạy) báo lỗi checksum mismatch khi khởi động. Luôn tạo
  file **mới** với version tăng tiếp theo.
- Đặt tên đúng convention đang dùng: `V{n}__{mo_ta_bang_snake_case}.sql` (2 dấu gạch
  dưới sau version, vd `V27__add-review-reply.sql` hoặc theo style gạch nối như phần
  lớn file hiện có — giữ nhất quán với các file gần nhất, không tự đổi style).
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate **không** tự tạo/sửa schema.
  Mọi thay đổi bảng/cột phải đi qua migration, không dựa vào Hibernate auto-DDL.
- `spring.flyway.enabled=true`, `spring.flyway.locations=classpath:db/migration` — chỉ
  thư mục này được Flyway quét, không đặt migration ở chỗ khác.

## Seed data cho local/Docker

- `backups/core-local-dump.sql` được mount vào container MySQL lúc khởi tạo lần đầu
  (volume rỗng) — Flyway chạy **sau** dump này để validate/áp thêm migration mới hơn.
  Xem [docs/DOCKER.md](../../../docs/DOCKER.md) mục 2 để biết chi tiết vòng đời.
- `scripts/mysql-init.sql` **không** được mount/chạy trong compose hiện tại — file này
  tham chiếu database/user khác (`manageruser`/`admin`) còn sót từ môi trường khác.
  Đừng chạy nó nhắm vào DB compose hiện tại, và đừng "dọn dẹp" xóa nó mà không hỏi —
  chưa rõ nó có được dùng ở nơi khác không.

## Test tích hợp DB thật

`src/test/java/guru/springframework/ghd/AbstractIntegrationTest.java` dùng
Testcontainers (`mysql:8.4.11`, `redis:7.4.10-alpine`, `withReuse(true)`) — không cần
MySQL/Redis chạy tay khi chạy `.\mvnw.cmd test`. Test class mới cần DB thật thì
`extends AbstractIntegrationTest` thay vì tự dựng container riêng.

## Checklist thêm 1 migration

- [ ] Version tiếp theo chưa bị trùng với file nào đang có
- [ ] Không sửa file `V1`...`V26` hiện có, kể cả sửa lỗi chính tả trong comment SQL
- [ ] Đã kiểm tra migration mới chạy được trên schema `core` hiện tại (chạy
      `.\mvnw.cmd test` để Testcontainers áp toàn bộ migration từ đầu — cách chắc chắn
      nhất để phát hiện lỗi thứ tự/phụ thuộc)
- [ ] Nếu đổi cột đang có dữ liệu (rename/drop/đổi kiểu) → cân nhắc ảnh hưởng tới
      `backups/core-local-dump.sql` (dump cũ không tự có cột/kiểu mới) và tới entity
      JPA tương ứng cùng lúc
- [ ] Nếu thêm index để tối ưu query chậm → xem
      [.claude/skills/optimize-slow-query/SKILL.md](../optimize-slow-query/SKILL.md)
