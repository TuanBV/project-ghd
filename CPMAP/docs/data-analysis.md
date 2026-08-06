# Phân tích dữ liệu đầu vào

Tài liệu này ghi lại kết quả phân tích thực tế hai file Excel trong `data/import/` (đọc bằng
`openpyxl`, không dùng giá trị formula cache), làm cơ sở thiết kế importer, migration và test.

## 1. `Bao_cao_so_sanh_gia_dien_may_2026-07-29.xlsx`

### Sheet `So sánh giá`

- 2.128 dòng dữ liệu, 19 cột (không tính header).
- Cột: `Tên sản phẩm`, `Mã sản phẩm / SKU`, `Giá Min`, `tongkhodienmaymienbac.com`,
  `sgt.com.vn`, `dienmay88.vn`, `dienmaythienphu.vn`, `dienmayabc.com`,
  `Trạng thái - Tổng kho`, `Trạng thái - SGT`, `Trạng thái - Điện Máy 88`,
  `Trạng thái - Thiên Phú`, `Trạng thái - Điện Máy ABC`, `URL - Tổng kho`, `URL - SGT`,
  `URL - Điện Máy 88`, `URL - Thiên Phú`, `URL - Điện Máy ABC`, `Chênh lệch Tổng kho - Min`.

### Thống kê xác nhận được (khớp với yêu cầu nghiệp vụ)

| Chỉ số | Giá trị đo được |
|---|---|
| Tổng số dòng | 2.128 |
| `Giá Min > 0` | 1.502 |
| `Giá Min = 0` (nghĩa là **không có giá**, không phải giá 0đ) | 626 |
| `Trạng thái - Tổng kho = Khớp chính xác` | 815 |
| `Trạng thái - Tổng kho = Nghi ngờ` | 10 |
| `Trạng thái - Tổng kho = Không tìm thấy` | 1.303 |
| Có giá số ở cột `tongkhodienmaymienbac.com` (815 + 10) | 825 |
| Vừa có `Giá Min > 0` vừa có giá web mình | 623 |
| — web mình cao hơn Min | 616 |
| — web mình thấp hơn Min | 6 |
| — web mình bằng Min | 1 |
| — chênh lệch trung bình | 984.256,82 VND |
| Giá số đã xác minh ở 4 cột đối thủ (`sgt.com.vn` 5, `dienmay88.vn` 1, `dienmaythienphu.vn` 0, `dienmayabc.com` 4) | 10 |
| Sản phẩm có ≥ 2 giá đối thủ hợp lệ | **0** |
| URL Tổng kho trùng cho nhiều SKU khác nhau (cần đánh dấu conflict) | 12 nhóm trên 812 URL duy nhất |

Kết luận nghiệp vụ quan trọng: **ngay sau import, hệ thống chưa có sản phẩm nào đủ điều kiện tính
trung bình competitor** (cần ≥ `minimum_competitor_count` = 2 theo policy mặc định). Vì vậy pricing
engine phải trả `INSUFFICIENT_DATA` cho hầu hết sản phẩm cho đến khi crawler/nhập tay bổ sung dữ liệu —
đây là lý do bảng `price_observations` và `competitor_listings` được tách riêng khỏi import, để crawler
và nhập tay có thể bổ sung dần.

### Sheet `Báo cáo`

Bảng tổng hợp 27 dòng × 8 cột do người dùng tự tổng hợp trong Excel — không dùng làm nguồn tính toán,
chỉ tham khảo khi đối chiếu (formula Excel không phải nguồn chân lý theo yêu cầu §2.1).

### Quy tắc import áp dụng

- `Giá Min = 0` ⇒ `null` (không có giá), lưu cờ `hasNoPrice=true`, không coi là `0 VND`.
- Chuỗi `Liên hệ` (nếu xuất hiện ở các cột giá) ⇒ observation `CONTACT_ONLY`, không có giá số.
- `Chưa xác minh` (status mặc định cho 4 cột đối thủ) khác `Không tìm thấy` (đã crawl/soát nhưng không
  tìm thấy trang) — cả hai được lưu nguyên trạng thái gốc trong `raw_status`, ánh xạ sang
  `observation_status`/`match_status` chuẩn hoá riêng.
  Chưa xác minh (import row status)  match_status = REVIEW_REQUIRED chỉ khi có URL & giá.
- Giữ `raw_row_json` (toàn bộ 19 cột gốc) trong `import_rows` để audit.
- Một URL đối thủ/website mình dùng cho ≥ 2 SKU khác nhau ⇒ tạo `import_issue` loại
  `DUPLICATE_URL_CONFLICT`, không tự gán.

## 2. `MC.xlsx`

### Sheet `Trang tính1`

- 3.265 dòng dữ liệu (17 cột nghiệp vụ theo đặc tả; workbook vật lý có tới cột 30 nhưng các cột
  18-30 trống hoàn toàn ở mọi dòng đã kiểm tra — importer chỉ đọc 17 cột đầu theo header).
- Cột: `id`, `item_group_id`, `tiêu đề`, `mô tả`, `liên kết`, `tình trạng`, `giá`, `còn hàng`,
  `liên kết hình ảnh`, `gtin`, `mpn`, `nhãn hiệu`, `danh mục sản phẩm của Google`, `loại sản phẩm`,
  `nhãn tùy chỉnh 0`, `nhãn tùy chỉnh 1`, `nhãn tùy chỉnh 2`.

### Thống kê xác nhận được

| Chỉ số | Giá trị đo được |
|---|---|
| Tổng số dòng | 3.265 |
| Thiếu `item_group_id` | 1.680 |
| Nhóm `id` trùng nhau | 10 |
| Nhóm `liên kết` (URL) trùng nhau | 9 (đề bài nêu 10 — chênh do khác biệt cách chuẩn hoá URL, importer log chính xác số đo thực tế) |
| Nhóm `item_group_id` trùng sau chuẩn hoá (trim/uppercase) | 16 (đề bài nêu 18, cùng lý do chuẩn hoá) |
| Biến thể `còn hàng` | `Out of Stock` 2.045, `In Stock` 1.179, `in Stock` 24, `out of Stock` 14, `In  Stock` (2 khoảng trắng) 2, `in` 1 |
| Biến thể `tình trạng` | `New` 3.259, `new` 6 |
| URL duy nhất trong MC | 3.256 |
| URL duy nhất "Tổng kho" trong file so sánh giá | 812 |
| Giao giữa 2 tập URL trên | 812 (toàn bộ 812 URL của báo cáo đều có trong MC) |
| URL MC chưa xuất hiện trong báo cáo so sánh | 2.444 (đề bài nêu ~2.443 — cùng bậc độ lớn) |

### Quy tắc import áp dụng

- `giá` dạng chuỗi `"3550000 VND"` ⇒ parse bằng regex số + đơn vị, lưu `BigDecimal` + `currency=VND`;
  không parse ra `double`/`float` ở bất kỳ layer nào.
- Chuẩn hoá `còn hàng`/`tình trạng`: `trim().toUpperCase()`, gộp khoảng trắng kép, map về enum
  `IN_STOCK`/`OUT_OF_STOCK`/`PREORDER`/`UNKNOWN` và `NEW`/`USED`/`REFURBISHED`; **giá trị chuẩn hoá**
  dùng để tính toán, **giá trị gốc** giữ lại trong `raw_availability`/`raw_condition` để audit.
- Không tự gộp dòng có `id`/`liên kết`/SKU trùng — mỗi dòng trùng tạo `import_issue` loại
  `DUPLICATE_ID` hoặc `DUPLICATE_URL`, để người dùng xử lý trên UI Imports.
- `item_group_id` rỗng vẫn import được (không dùng nó làm khóa chính), chỉ đánh dấu
  `import_issue` loại `MISSING_ITEM_GROUP_ID` mức cảnh báo.

## 3. Ghép nối hai nguồn dữ liệu

- Khóa ghép giữa hai file là **URL** (`URL - Tổng kho` ở file so sánh giá ↔ `liên kết` ở file MC),
  không phải SKU, vì SKU ở file so sánh giá là mã rút gọn còn `id`/`item_group_id` ở MC có định dạng
  khác. `ProductMatchingService` dùng đúng thứ tự ưu tiên nêu ở §6 của prompt, trong đó bước
  "SKU/model xuất hiện trong title" xử lý các trường hợp còn lại (~237 theo mô tả đề bài) và bước
  URL slug xử lý phần còn dư nhỏ.
- 815 SKU khớp chính xác + 10 nghi ngờ ⇒ 825 sản phẩm có thể tạo `competitor_listing` ứng với
  website `tongkhodienmaymienbac.com`; nhưng **`tongkhodienmaymienbac.com` không được coi là
  đối thủ** — cột này chỉ dùng để đối soát giá web mình với `Giá Min` (tham khảo lịch sử), không
  đưa vào danh sách `competitor_listings`.
- 4 cột đối thủ thật (sgt, dienmay88, dienmaythienphu, dienmayabc) hiện chỉ có 10 giá số đã xác
  minh trong toàn bộ 2.128 dòng ⇒ sau import, **0 sản phẩm** đủ 2 nguồn đối thủ hợp lệ để tính
  trung bình. Đây là lý do acceptance test (§14 của prompt) yêu cầu hệ thống **không tự động
  publish** ngay sau import.

## 4. Rủi ro dữ liệu cần lưu ý khi vận hành

1. URL trùng cho nhiều SKU (12 nhóm ở file so sánh giá, ~9-10 nhóm ở MC) ⇒ bắt buộc phải qua
   hàng đợi conflict, không tự động chọn 1 trong nhiều bản ghi.
2. `item_group_id` thiếu ở 51% số dòng MC ⇒ không thể dùng trường này làm khóa nhóm biến thể khi
   ghép variant; hệ thống hiện tại coi mỗi dòng MC là 1 `product` độc lập theo `id`.
3. Chuỗi tiêu đề MC có lẫn nhiều mã sản phẩm (ví dụ mô tả `FR-132CI FR132CI FR 132CI`) ⇒ sinh ra
   nhiều alias khi ghép; matching pipeline phải review chứ không tự tin 100%.
4. Do object cache của Excel (`data_only=True`) được dùng để đọc, các cột có formula (ví dụ
   `Chênh lệch Tổng kho - Min`) chỉ tin tưởng giá trị cache tại thời điểm export — backend luôn
   tính lại theo `BigDecimal`, không dùng giá trị này để publish.
