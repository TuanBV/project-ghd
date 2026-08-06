# Prompt cho Claude Code — Xây dựng hệ thống tối ưu giá và đồng bộ Google Merchant Center

Bạn là **Senior Software Architect + Senior Full-stack Engineer**. Hãy trực tiếp tạo một project chạy được, không chỉ đưa ra kế hoạch hoặc pseudo-code.

## 1. Mục tiêu nghiệp vụ

Xây dựng hệ thống quản trị giá sản phẩm cho website `tongkhodienmaymienbac.com` với luồng chính:

1. Nhập danh mục sản phẩm hiện tại từ Google Merchant Center/feed Excel.
2. Quản lý danh sách website đối thủ; người dùng có thể thêm/sửa/xóa website đối thủ bằng tay.
3. Ghép sản phẩm của mình với sản phẩm tương ứng trên từng website đối thủ.
4. Thu thập giá đối thủ tự động bằng crawler, đồng thời cho phép nhập giá thủ công.
5. Tính **trung bình cộng giá hợp lệ của các website đối thủ** cho từng sản phẩm.
6. Giá trung bình sau khi áp dụng các quy tắc làm tròn và kiểm soát rủi ro sẽ là **giá đề xuất cho website của mình**.
7. Có quy trình duyệt giá, cập nhật giá website, sau đó đồng bộ đúng giá đó lên Google Merchant Center.
8. Có dashboard phân tích sản phẩm, độ phủ dữ liệu, biến động giá và tình trạng job.
9. Có job tự động chạy hằng ngày và nút chạy thủ công.
10. Lưu toàn bộ lịch sử giá, lần crawl, lần tính toán, lần duyệt và lần đồng bộ.

## 2. Hai file đầu vào cần đọc và hỗ trợ import

Tìm và phân tích hai file sau trong thư mục project hoặc thư mục `data/import`:

- `Bao_cao_so_sanh_gia_dien_may_2026-07-29.xlsx`
- `MC.xlsx`

Nếu file chưa có trong repo, vẫn phải tạo đầy đủ importer và fixture test theo schema dưới đây.

### 2.1. File báo cáo so sánh giá

Workbook có hai sheet:

- `So sánh giá`: 2.128 dòng dữ liệu, 19 cột.
- `Báo cáo`: bảng tổng hợp.

Các cột sheet `So sánh giá`:

- `Tên sản phẩm`
- `Mã sản phẩm / SKU`
- `Giá Min`
- `tongkhodienmaymienbac.com`
- `sgt.com.vn`
- `dienmay88.vn`
- `dienmaythienphu.vn`
- `dienmayabc.com`
- `Trạng thái - Tổng kho`
- `Trạng thái - SGT`
- `Trạng thái - Điện Máy 88`
- `Trạng thái - Thiên Phú`
- `Trạng thái - Điện Máy ABC`
- `URL - Tổng kho`
- `URL - SGT`
- `URL - Điện Máy 88`
- `URL - Thiên Phú`
- `URL - Điện Máy ABC`
- `Chênh lệch Tổng kho - Min`

Quy tắc import:

- `Giá Min = 0` phải được hiểu là **không có giá**, không phải giá bằng 0.
- Chuỗi `Liên hệ` là không có giá số.
- `Chưa xác minh` khác với `Không tìm thấy`.
- Giữ nguyên dữ liệu raw để audit.
- Formula Excel không phải nguồn chân lý; backend phải tự tính lại.
- Một URL được ghép cho nhiều SKU phải được đánh dấu xung đột để người dùng xử lý.

Các thống kê dùng để kiểm thử importer:

- Tổng SKU: `2128`
- `Giá Min > 0`: `1502`
- `Giá Min = 0`: `626`
- Ghép được với website mình: `825`
  - `815` khớp chính xác
  - `10` nghi ngờ
- Không tìm thấy trên website mình: `1303`
- Trong 623 SKU vừa có `Giá Min > 0` vừa có giá website mình:
  - website mình cao hơn Min: `616`
  - thấp hơn Min: `6`
  - bằng Min: `1`
  - chênh lệch trung bình khoảng `984256.82 VND`
- Bốn cột website đối thủ hiện chỉ có tổng cộng `10` giá số được xác minh.
- Không có sản phẩm nào hiện có từ 2 giá số của đối thủ trở lên.
- Vì vậy hệ thống không được tự động tính và đẩy hàng loạt ngay sau import; phải chờ crawler/nhập tay tạo đủ dữ liệu.

### 2.2. File MC

Sheet `Trang tính1`: 3.265 dòng dữ liệu, 17 cột, là feed theo cấu trúc Google Merchant Center:

- `id`
- `item_group_id`
- `tiêu đề`
- `mô tả`
- `liên kết`
- `tình trạng`
- `giá`
- `còn hàng`
- `liên kết hình ảnh`
- `gtin`
- `mpn`
- `nhãn hiệu`
- `danh mục sản phẩm của Google`
- `loại sản phẩm`
- `nhãn tùy chỉnh 0`
- `nhãn tùy chỉnh 1`
- `nhãn tùy chỉnh 2`

Quy tắc import:

- Parse giá dạng `3550000 VND` thành `BigDecimal` và currency `VND`.
- Không dùng `double` hoặc `float` cho tiền.
- Chuẩn hóa condition và availability không phân biệt hoa thường hoặc khoảng trắng.
- Không tự động gộp các dòng có ID, URL hoặc SKU trùng nhau.
- Lưu lỗi/xung đột vào bảng import issue để xử lý trên UI.

Các thống kê dùng để kiểm thử importer:

- Tổng sản phẩm: `3265`
- Có `1680` dòng thiếu `item_group_id`.
- Có `10` nhóm ID trùng và `10` nhóm URL trùng.
- Có `18` nhóm `item_group_id` trùng sau khi chuẩn hóa.
- Availability hiện có nhiều biến thể như `In Stock`, `in Stock`, `In  Stock`, `Out of Stock`, `out of Stock`, `in`.
- Báo cáo hiện nối được 825 dòng sang MC qua URL, tương ứng 812 URL duy nhất.
- Phân loại cách ghép tham khảo:
  - khoảng `574` ghép SKU trực tiếp
  - khoảng `237` tìm thấy SKU trong title
  - khoảng `4` tìm thấy qua slug/ID
  - `10` trường hợp fuzzy/nghi ngờ cần duyệt
- Khoảng `2443` URL duy nhất trong MC chưa xuất hiện trong file báo cáo so sánh.

## 3. Kiến trúc kỹ thuật

Tạo **modular monolith**, không chia microservice ở giai đoạn này.

### Backend

- Java 21
- Spring Boot 3.x bản stable tương thích Java 21
- Maven
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security + JWT với role `ADMIN`, `ANALYST`, `OPERATOR`
- PostgreSQL
- Flyway migration
- Quartz Scheduler dùng JDBC JobStore để job không mất khi restart
- Apache POI để import Excel
- Jsoup cho trang HTML tĩnh
- Playwright Java làm fallback cho trang render bằng JavaScript
- Resilience4j cho retry, timeout, circuit breaker
- Spring Boot Actuator + Micrometer
- Testcontainers cho integration test
- MapStruct có thể dùng cho mapping DTO
- OpenAPI/Swagger

### Frontend

- React + TypeScript + Vite
- Ant Design hoặc một UI library quản trị tương đương
- TanStack Query
- React Router
- ECharts
- Form validation rõ ràng
- Responsive ở mức desktop/tablet

### Hạ tầng

- Dockerfile cho backend và frontend
- `docker-compose.yml` chạy PostgreSQL + backend + frontend
- `.env.example`
- GitHub Actions chạy build, unit test và integration test
- Timezone nghiệp vụ: `Asia/Ho_Chi_Minh`
- Currency mặc định: `VND`

Không thêm Redis nếu chưa có nhu cầu thực tế. Dùng Quartz/JDBC và transaction DB để khóa job.

## 4. Cấu trúc module backend

Thiết kế package theo feature, ví dụ:

```text
com.example.mcprice
├── auth
├── product
├── importdata
├── competitor
├── matching
├── crawling
├── pricing
├── websitepublish
├── merchant
├── job
├── dashboard
├── audit
├── common
└── config
```

Không tạo package kiểu chung chung chỉ gồm `controller/service/repository` cho toàn hệ thống. Mỗi feature tự chứa controller, service, repository, domain và DTO của nó.

## 5. Data model tối thiểu

### `products`

- `id`
- `mc_offer_id`
- `external_id`
- `sku_original`
- `sku_normalized`
- `title`
- `description`
- `product_url`
- `image_url`
- `brand`
- `google_category`
- `product_type`
- `condition`
- `availability`
- `current_website_price`
- `current_mc_price`
- `currency`
- `active`
- `import_source`
- `source_updated_at`
- audit timestamps
- optimistic locking version

### `product_aliases`

- `product_id`
- `alias_type`: `SKU`, `MODEL`, `TITLE_TOKEN`, `LEGACY_SKU`, `URL_SLUG`
- `alias_original`
- `alias_normalized`
- `confirmed`
- `confidence`
- unique constraint hợp lý

### `competitors`

- `id`
- `name`
- `base_url`
- `enabled`
- `crawl_mode`: `STATIC_HTML`, `BROWSER`, `MANUAL_ONLY`
- `requests_per_minute`
- `timeout_seconds`
- `extractor_config` dạng JSONB
- `last_success_at`
- `last_error_at`

Seed sẵn:

- `sgt.com.vn`
- `dienmay88.vn`
- `dienmaythienphu.vn`
- `dienmayabc.com`

Website của mình `tongkhodienmaymienbac.com` không được tính là đối thủ.

### `competitor_listings`

- `product_id`
- `competitor_id`
- `url`
- `external_sku`
- `match_method`
- `match_score`
- `match_status`: `AUTO_CONFIRMED`, `MANUALLY_CONFIRMED`, `REVIEW_REQUIRED`, `REJECTED`
- `active`
- unique theo competitor + URL
- không cho auto-publish nếu match chưa được xác nhận

### `price_observations`

- `competitor_listing_id`
- `price`
- `currency`
- `availability`
- `raw_price_text`
- `source_type`: `CRAWL`, `MANUAL`, `IMPORT`
- `observation_status`: `VALID`, `NO_PRICE`, `CONTACT_ONLY`, `OUT_OF_STOCK`, `PARSE_ERROR`, `STALE`
- `captured_at`
- `verified_by`
- `note`
- lưu response metadata cần thiết nhưng không lưu dữ liệu nhạy cảm

### `price_policies`

Hỗ trợ policy global, theo category và theo product:

- `minimum_competitor_count`, mặc định `2`
- `max_observation_age_hours`, mặc định `48`
- `rounding_step`, mặc định `10000 VND`
- `max_increase_percent`, mặc định `15`
- `max_decrease_percent`, mặc định `15`
- `outlier_threshold_percent`, mặc định `30`
- `outlier_strategy`, mặc định `FLAG_ONLY`
- `require_manual_approval`
- `minimum_allowed_price`
- `maximum_allowed_price`
- `auto_publish_enabled`

### `price_recommendations`

- `product_id`
- `current_price`
- `raw_average_price`
- `rounded_price`
- `final_suggested_price`
- `included_source_count`
- `excluded_source_count`
- `calculation_snapshot` JSONB
- `status`: `INSUFFICIENT_DATA`, `REVIEW_REQUIRED`, `READY`, `APPROVED`, `REJECTED`, `PUBLISHED`, `FAILED`
- `created_at`
- `approved_by`
- `approved_at`

Tạo bảng liên kết recommendation với các observation được dùng để giải thích chính xác giá được tính từ đâu.

### Các bảng còn lại

- `import_runs`
- `import_rows` hoặc `import_issues`
- `crawl_runs`
- `crawl_items`
- `job_runs`
- `website_publish_runs`
- `website_publish_items`
- `merchant_sync_runs`
- `merchant_sync_items`
- `audit_logs`

## 6. Chuẩn hóa và ghép sản phẩm

Tạo `ProductMatchingService` có pipeline rõ ràng:

1. Exact normalized SKU.
2. Exact alias đã xác nhận.
3. SKU/model xuất hiện trong title.
4. SKU/model xuất hiện trong URL slug.
5. Fuzzy matching theo token title, brand, model và category.
6. Manual confirmation.

Chuẩn hóa SKU:

- trim
- uppercase
- bỏ khoảng trắng thừa
- bỏ dấu phân cách không mang ý nghĩa
- vẫn giữ bản raw
- sinh alias khi SKU chứa ngoặc, nhiều model hoặc hậu tố
- không gộp mù quáng các model gần giống nhau

Ví dụ cần xử lý:

- `SL12C (LC1000C)` có thể tạo hai alias nhưng phải review.
- `HPF AD6783.1` không được tự động coi chắc chắn là `HPF AD6783`.
- SKU ngắn hơn xuất hiện trong title có thể là candidate, không phải luôn là exact.
- Một URL nối với nhiều SKU phải tạo conflict.

Mỗi kết quả ghép phải có:

- phương pháp
- điểm confidence
- lý do
- dữ liệu so sánh
- trạng thái duyệt

Không auto-publish đối với `REVIEW_REQUIRED`.

## 7. Crawler website đối thủ

Thiết kế theo Strategy/Adapter:

```java
public interface CompetitorPriceCrawler {
    boolean supports(Competitor competitor);
    CrawlResult crawl(CompetitorListing listing);
}
```

Có:

- `StaticHtmlCrawler` dùng Jsoup.
- `BrowserCrawler` dùng Playwright.
- `ManualOnlyCrawler`.
- Adapter/config riêng từng domain, nhưng không để CSS selector rải trong business service.
- Cho phép cấu hình selector giá, giá khuyến mại, trạng thái tồn kho và product code bằng JSONB.
- Lưu raw price text, URL cuối, HTTP status, thời gian và lỗi parse.
- Rate limit theo domain.
- Timeout và retry có giới hạn.
- User-Agent rõ ràng.
- Tuân thủ robots.txt/điều khoản website.
- Không bypass CAPTCHA, login, chống bot hoặc cơ chế bảo vệ.
- Khi crawl không được, chuyển sang manual entry thay vì giả lập dữ liệu.
- Test crawler bằng HTML fixture cục bộ; unit test không gọi website thật.

## 8. Thuật toán tính giá

Đây là yêu cầu nghiệp vụ bắt buộc: **giá đề xuất là trung bình cộng giá hợp lệ của các website đối thủ**.

Quy trình:

1. Chỉ lấy competitor đang enabled.
2. Chỉ lấy listing đã xác nhận.
3. Lấy observation mới nhất của mỗi competitor.
4. Chỉ lấy giá số `> 0`, đúng currency và chưa stale.
5. Loại `Liên hệ`, parse error, chưa xác minh, URL không khớp và dữ liệu của website mình.
6. Mỗi competitor chỉ đóng góp tối đa một giá.
7. Tính bằng `BigDecimal`.
8. `rawAverage = sum(prices) / count`.
9. Làm tròn theo `rounding_step`.
10. Lưu đầy đủ các giá đầu vào và phép tính vào calculation snapshot.

Outlier:

- Mặc định `FLAG_ONLY`: vẫn thể hiện trong dữ liệu nhưng recommendation chuyển sang `REVIEW_REQUIRED`.
- Không tự loại outlier làm thay đổi trung bình nếu người dùng chưa cấu hình policy.
- UI cho phép người có quyền loại một observation với lý do, sau đó tính lại.

Điều kiện tự động:

- Nếu số nguồn hợp lệ nhỏ hơn `minimum_competitor_count`, status là `INSUFFICIENT_DATA` và giữ nguyên giá hiện tại.
- Nếu thay đổi vượt guardrail phần trăm hoặc có outlier, yêu cầu duyệt.
- Nếu có duplicate/matching conflict, không cho publish.
- Manual override phải có người nhập, lý do và ngày hết hạn.
- Không dùng `Giá Min` làm giá đề xuất; nó chỉ là nguồn tham khảo/import lịch sử.
- Không cộng thêm margin vì yêu cầu hiện tại là lấy đúng trung bình đối thủ; tuy nhiên data model nên sẵn sàng bổ sung cost floor sau này.

## 9. Quy trình cập nhật website và Google Merchant Center

Tạo hai adapter riêng:

```java
public interface WebsitePricePublisher {
    PublishResult updateWebsitePrice(Product product, BigDecimal newPrice);
    VerificationResult verifyLandingPagePrice(Product product, BigDecimal expectedPrice);
}

public interface MerchantCenterPublisher {
    PublishResult updateMerchantPrice(Product product, BigDecimal newPrice);
}
```

Luồng publish bắt buộc:

1. Recommendation được approved hoặc đủ điều kiện auto-publish.
2. Update giá trên website/CMS trước.
3. Đọc/kiểm tra lại landing page hoặc API website để xác nhận giá mới.
4. Chỉ khi website đã đúng giá mới được đồng bộ lên Merchant Center.
5. Sau đó đọc trạng thái Merchant Center và lưu kết quả.
6. Nếu website chưa cập nhật thành công thì tuyệt đối không đẩy MC.
7. Tất cả thao tác phải idempotent, retry an toàn và có audit log.

Do chưa chắc website đang dùng CMS/API nào:

- Tạo `MockWebsitePricePublisher` hoạt động trong local.
- Tạo interface để thêm WooCommerce/custom API adapter sau.
- Có thể chuẩn bị `WooCommerceWebsitePricePublisher` phía sau feature flag, nhưng không hardcode credentials.
- Cho phép export file CSV/XLSX để cập nhật thủ công khi chưa có API website.

### Google Merchant Center

- Dùng **Google Merchant API**, không xây mới trên API cũ.
- Encapsulate hoàn toàn trong module `merchant`.
- Hỗ trợ `DRY_RUN=true` mặc định.
- Tạo `MockMerchantCenterPublisher` cho local/test.
- Khi cấu hình thật, dùng OAuth/service account theo cách chính thức phù hợp tài khoản.
- Hỗ trợ API data source/product input theo API stable hiện tại.
- Không hardcode account ID, data source ID hoặc credential.
- Có retry cho lỗi tạm thời, không retry vô hạn lỗi validation.
- Có batch/chunk, progress, partial failure và resume.
- Có fallback xuất lại feed theo đúng schema của `MC(1).xlsx`.

## 10. Job tự động và chạy tay

Dùng Quartz JDBC JobStore.

Các job:

1. `ImportRefreshJob` nếu có file/feed định kỳ.
2. `CompetitorCrawlJob`.
3. `PriceCalculationJob`.
4. `WebsitePublishJob`.
5. `MerchantSyncJob`.
6. `StaleObservationCleanupJob`.

Job tổng hằng ngày:

```text
crawl -> validate -> calculate -> approve/auto-policy -> update website -> verify -> sync MC
```

- Mặc định chạy lúc `02:00` hằng ngày theo `Asia/Ho_Chi_Minh`.
- Cron chỉnh được trong Settings.
- Có nút chạy toàn bộ hoặc chạy từng bước bằng tay.
- Không cho hai lần chạy cùng loại chồng nhau.
- Có trạng thái `QUEUED`, `RUNNING`, `SUCCESS`, `PARTIAL_SUCCESS`, `FAILED`, `CANCELLED`.
- Có progress, số item thành công/thất bại và error detail.
- Manual run phải trả về `jobRunId`.
- Có endpoint xem log và retry các item lỗi.

## 11. Dashboard và màn hình

### Dashboard

KPI:

- Tổng sản phẩm MC.
- Sản phẩm đã ghép/chưa ghép/xung đột.
- Sản phẩm có 0, 1, 2, 3+ giá đối thủ hợp lệ.
- Số recommendation `INSUFFICIENT_DATA`, `REVIEW_REQUIRED`, `READY`, `APPROVED`, `PUBLISHED`.
- Số sản phẩm tăng giá/giảm giá/không đổi.
- Tổng và trung bình chênh lệch giá.
- Crawl success rate theo website.
- Số dữ liệu stale.
- Job gần nhất và lỗi gần nhất.
- Số bản ghi sync MC thành công/thất bại.

Biểu đồ:

- Phân bố % thay đổi giá.
- Giá hiện tại và giá đề xuất theo category.
- Biến động giá theo thời gian của sản phẩm.
- Độ phủ giá theo competitor.
- Tỷ lệ crawl thành công theo ngày.
- Top sản phẩm tăng/giảm mạnh.
- Top category có nhiều sản phẩm thiếu dữ liệu.

### Products

- Search theo SKU, title, URL.
- Filter category, availability, match status, recommendation status.
- Table có current price, average competitor price, suggested price, source count, last crawl.
- Product detail:
  - thông tin MC
  - aliases
  - competitor listings
  - lịch sử giá
  - breakdown phép tính
  - conflict
  - approve/reject/manual override
  - publish history

### Competitors

- CRUD website đối thủ.
- Enable/disable.
- Cấu hình crawl mode, selector, rate limit, timeout.
- Test crawl một URL.
- Xem success rate và lỗi gần nhất.

### Imports

- Upload Excel.
- Preview mapping.
- Validate trước khi commit.
- Hiển thị duplicate, missing field, invalid price và conflict.
- Download error report.
- Import idempotent theo file hash + row identity.

### Jobs

- Danh sách job.
- Trigger manual.
- Theo dõi progress.
- Retry failed items.
- Xem log.

### Settings

- Cron.
- Price policy.
- Rounding.
- Minimum source count.
- Guardrail.
- Auto-publish.
- Dry run.
- Merchant configuration metadata không chứa secret.

## 12. REST API tối thiểu

Ví dụ:

```text
POST   /api/auth/login

POST   /api/imports/mc
POST   /api/imports/comparison
GET    /api/imports/{id}
GET    /api/imports/{id}/issues

GET    /api/products
GET    /api/products/{id}
PUT    /api/products/{id}
POST   /api/products/{id}/aliases
POST   /api/products/{id}/matches/{matchId}/confirm
POST   /api/products/{id}/matches/{matchId}/reject

GET    /api/competitors
POST   /api/competitors
PUT    /api/competitors/{id}
DELETE /api/competitors/{id}
POST   /api/competitors/{id}/test-crawl

POST   /api/crawls/run
GET    /api/crawls/{runId}
POST   /api/products/{id}/manual-price

POST   /api/pricing/recalculate
POST   /api/pricing/products/{productId}/recalculate
GET    /api/recommendations
POST   /api/recommendations/{id}/approve
POST   /api/recommendations/{id}/reject
POST   /api/recommendations/{id}/override

POST   /api/publish/website
POST   /api/publish/merchant
POST   /api/publish/full-pipeline
GET    /api/publish/runs/{id}

GET    /api/jobs
POST   /api/jobs/{jobKey}/trigger
GET    /api/job-runs/{id}

GET    /api/dashboard/summary
GET    /api/dashboard/price-trends
GET    /api/dashboard/competitor-health
```

Tạo DTO riêng; không expose JPA entity trực tiếp.

## 13. Bảo mật và an toàn dữ liệu

- JWT expiration/refresh hợp lý.
- Password hash bằng BCrypt hoặc Argon2.
- RBAC ở endpoint và UI.
- Không log credential/token.
- Secret chỉ lấy từ environment/secret manager.
- Validate URL để giảm SSRF:
  - chỉ crawl domain đã khai báo
  - block localhost/private IP
  - limit redirect
- Giới hạn kích thước upload.
- Chống formula injection khi export CSV/XLSX.
- Audit các hành động:
  - sửa match
  - nhập giá tay
  - thay đổi policy
  - approve/reject
  - publish
- `DRY_RUN=true` mặc định.
- Không có tác vụ thật nào được chạy tự động trong local/test.

## 14. Testing

### Unit test

- Parse giá VND.
- Chuẩn hóa SKU.
- Alias từ SKU có ngoặc/ký tự đặc biệt.
- Product matching.
- Phân biệt exact và fuzzy.
- Tính arithmetic mean bằng BigDecimal.
- Rounding.
- Insufficient source count.
- Stale observation.
- Outlier flag.
- Guardrail.
- Manual override.
- Availability normalization.

### Integration test

Dùng Testcontainers:

- Excel import.
- Flyway.
- Repository.
- Quartz job.
- Full calculation pipeline.
- Publish dry-run.
- Idempotency.
- Duplicate/conflict handling.

### Frontend test

- Dashboard render.
- Filter/search.
- Approve/reject flow.
- Manual price form.
- Job progress.
- Import issue table.

### Acceptance test với hai file

Khi import đúng hai file:

- MC import ra 3.265 dòng.
- Comparison import ra 2.128 dòng.
- Hệ thống báo 1.680 dòng MC thiếu item group.
- Hệ thống phát hiện duplicate/conflict thay vì ghi đè.
- Hệ thống hiển thị 825 mapping hiện có.
- Hệ thống không auto-publish vì hầu hết sản phẩm chưa đủ 2 giá đối thủ.
- Dashboard thể hiện đúng số liệu độ phủ.
- Chạy manual price cho một sản phẩm, thêm đủ 2 nguồn, tính trung bình, approve, publish dry-run thành công và có audit log.

## 15. Dữ liệu demo

Tạo seed/demo:

- 5–10 sản phẩm.
- 4 competitor.
- Các trường hợp:
  - đủ 2 giá
  - chỉ 1 giá
  - `Liên hệ`
  - outlier
  - stale
  - fuzzy match
  - duplicate URL
  - manual override
- Mock website publisher.
- Mock Merchant publisher.
- Một job run thành công và một job run partial failure.

## 16. Yêu cầu chất lượng code

- Code compile và chạy được.
- Không chỉ tạo skeleton trống.
- Không để TODO ở luồng import, manual price, calculation, dashboard API, job manual và dry-run publish.
- Tách domain logic khỏi controller và crawler.
- Transaction boundary rõ ràng.
- Dùng pagination.
- Tránh N+1.
- Có index DB cho normalized SKU, product URL, captured time, statuses và foreign keys thường query.
- Dùng optimistic locking cho product/recommendation.
- Error response theo chuẩn thống nhất.
- Logging có correlation ID/job run ID.
- Javadoc chỉ ở phần logic khó, không viết comment thừa.
- README bằng tiếng Việt, có sơ đồ kiến trúc Mermaid và mô tả luồng dữ liệu.

## 17. Trình tự thực hiện

1. Kiểm tra repo hiện tại; nếu đã có project thì không phá cấu trúc/cấu hình đang dùng.
2. Đọc hai file Excel thật nếu chúng tồn tại.
3. Viết `docs/data-analysis.md` ghi lại schema, thống kê và vấn đề dữ liệu.
4. Tạo backend, migration và importer.
5. Tạo matching + manual correction.
6. Tạo observation + pricing engine.
7. Tạo Quartz jobs và manual trigger.
8. Tạo publisher interface + mock + dry run.
9. Tạo Merchant API adapter có feature flag.
10. Tạo frontend dashboard và các màn hình chính.
11. Tạo tests.
12. Tạo Docker/README.
13. Chạy toàn bộ build/test và sửa đến khi pass.

Không dừng ở việc đề xuất kiến trúc. Hãy trực tiếp tạo file và code.

## 18. Kết quả cuối cùng phải báo cáo

Khi hoàn thành, in ra:

- Cây thư mục chính.
- Các module đã hoàn thành.
- Lệnh chạy local.
- Lệnh chạy test.
- Tài khoản demo.
- URL Swagger và frontend.
- Kết quả import/test hai file.
- Những phần đang dùng mock.
- Danh sách biến môi trường cần để kết nối website thật và Google Merchant Center.
- Rủi ro hoặc bước thủ công còn lại.

Bắt đầu bằng cách kiểm tra repo và hai file Excel, sau đó triển khai lần lượt. Không hỏi lại những quyết định đã được nêu rõ trong prompt. Chỉ yêu cầu thông tin bổ sung khi thực sự cần credential hoặc API của hệ thống website thật.
