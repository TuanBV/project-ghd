---
name: optimize-slow-query
description: "Task-template: quy trình tối ưu 1 query/endpoint chậm trong GHD (JPQL/native query, N+1, index, cache). Không chứa quy tắc kỹ thuật riêng — điều hướng tới skill flyway-mysql và redis-cache-session."
---

# Task-template: tối ưu 1 query/endpoint chậm — GHD

## Quy trình

1. **Xác định query thật đang chạy**: bỏ comment tạm 2 dòng
   `spring.jpa.properties.hibernate.show_sql`/`hibernate.orm.jdbc.bind` trong
   `application.properties` để xem SQL + bind values thật (nhớ comment lại sau khi
   xong, đừng để log SQL bật vĩnh viễn — ảnh hưởng performance log + có thể lộ dữ liệu
   nhạy cảm vào file log).
2. **`ProductController`/`ProductServiceImpl`/`ProductRepository` là hotspot lớn nhất
   và nhiều behavior nhất** (theo `docs/PROJECT_INDEX.md` mục Hotspots) — nếu query
   chậm nằm ở đây, đặc biệt cẩn thận vì nhiều luồng khác phụ thuộc (list/search/gợi ý/
   export/sync).
3. **Kiểm tra N+1**: repository dùng JPQL/native `@Query` hay lazy-load quan hệ entity?
   Ưu tiên viết lại bằng 1 query có `JOIN FETCH`/projection thay vì load-rồi-lặp.
4. **Cân nhắc cache trước khi thêm index**, nếu dữ liệu đọc nhiều/ít đổi (category,
   brand, banner, slider, policy, sys-param, product theo slug/mới nhất/liên quan) —
   xem cache name/TTL đã có sẵn hay cần thêm mới:
   [.claude/skills/redis-cache-session/SKILL.md](../redis-cache-session/SKILL.md).
5. **Nếu cần thêm index DB**: tạo migration Flyway mới (không sửa migration cũ), theo
   [.claude/skills/flyway-mysql/SKILL.md](../flyway-mysql/SKILL.md). Đo lại (`EXPLAIN`)
   trước/sau nếu có thể, ghi lại trong PR/báo cáo con số cụ thể thay vì chỉ nói
   "nhanh hơn".
6. **Đơn hàng (`OrdersServiceImpl.createOrder`)**: có pessimistic lock
   `findByIdForUpdate` (`SELECT ... FOR UPDATE`) cố ý để chống oversell — đây **không**
   phải chỗ để "tối ưu" bằng cách đổi sang optimistic lock hay bỏ lock, trừ khi hiểu rõ
   và được người dùng đồng ý đánh đổi rủi ro oversell. Xem
   [docs/PROCESSING-FLOW.md](../../../docs/PROCESSING-FLOW.md) mục 2.
7. Sau khi tối ưu, chạy `.\mvnw.cmd test` + gọi
   [.claude/agents/ghd-qa.md](../../agents/ghd-qa.md) để xác nhận không phá hành vi cũ
   (đặc biệt số lượng/thứ tự kết quả trả về không đổi ngoài ý muốn).
