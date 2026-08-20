# Hướng dẫn tích hợp VNPay — GHD

Tài liệu này gom lại toàn bộ những gì cần biết để **cấu hình, chạy thử, và debug** luồng
thanh toán VNPay (thẻ ngân hàng CARD + trả góp INSTALLMENT) của GHD. Không lặp lại quy
tắc bắt buộc đã có ở [.claude/skills/vnpay-payment/SKILL.md](../.claude/skills/vnpay-payment/SKILL.md)
(đọc file đó trước khi sửa code vùng này) — tài liệu này tập trung vào **cấu hình +
vận hành + troubleshooting** để người mới join có thể tự chạy thử được.

Nguồn code: `controllers/api/PaymentController.java`, `services/impl/PaymentServiceImpl.java`,
`services/impl/VnpayServiceImpl.java`, `utils/VnpayUtil.java`, entity `Payment`.
Sequence diagram đầy đủ: [docs/PROCESSING-FLOW.md](PROCESSING-FLOW.md) mục 6.

**CHƯA có xác nhận cuối cùng với tài liệu merchant VNPay thật cho mọi chi tiết field/
format dưới đây** — xem mục "Chưa xác nhận với tài liệu thật" ở cuối file.

## 1. Cấu hình bắt buộc

### 1.1. Biến môi trường

| Biến | Ý nghĩa | Bắt buộc? |
|---|---|---|
| `VNPAY_TMN_CODE` | Mã website (Terminal ID) do VNPay cấp cho merchant | Có — không có default |
| `VNPAY_HASH_SECRET` | Secret key dùng ký/verify HMAC-SHA512 | Có — không có default |
| `VNPAY_PAY_URL` | URL trang thanh toán VNPay (sandbox: `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`) | Có — không có default |
| `VNPAY_API_URL` | URL Query API đối soát (sandbox: `https://sandbox.vnpayment.vn/merchant_webapi/api/transaction`) | Chưa dùng ở v1 (`queryTransaction` chưa hiện thực — xem mục 5) |
| `APP_BASE_URL` | URL public app này có thể được gọi tới — dùng build `vnp_ReturnUrl` **và** là domain VNPay gọi IPN về | Có, default `http://localhost:18080` (chỉ chạy được return URL, **không** chạy được IPN — xem mục 3) |

Map sang `application.properties` (không có default cho 4 biến VNPay đầu — cố ý, để
`VnpayServiceImpl.buildPaymentUrl` fail rõ ràng ngay khi thiếu cấu hình thay vì âm thầm
build 1 URL sai):

```properties
vnpay.tmn-code=${VNPAY_TMN_CODE:}
vnpay.hash-secret=${VNPAY_HASH_SECRET:}
vnpay.pay-url=${VNPAY_PAY_URL:}
vnpay.api-url=${VNPAY_API_URL:}
app.base-url=${APP_BASE_URL:http://localhost:18080}
```

### 1.2. File `.env` (chạy qua Docker Compose)

**Sửa nhầm lẫn thường gặp**: `.env.example` chỉ là file mẫu, **không bao giờ** được
`docker compose`/app đọc lúc chạy thật. Phải tạo file **`.env`** thật (`cp .env.example
.env` rồi điền giá trị thật) ở đúng thư mục `ghd/` (cùng cấp `compose.yaml`).

`compose.yaml` phải có khai báo truyền các biến này vào container `app` (đã có sẵn từ
bản sửa gần nhất — nếu không thấy, bug):

```yaml
VNPAY_TMN_CODE: ${VNPAY_TMN_CODE:-}
VNPAY_HASH_SECRET: ${VNPAY_HASH_SECRET:-}
VNPAY_PAY_URL: ${VNPAY_PAY_URL:-}
VNPAY_API_URL: ${VNPAY_API_URL:-}
APP_BASE_URL: ${APP_BASE_URL:-http://localhost:18080}
```

Chỉ sửa `.env.example` (không tạo `.env`) hoặc quên thêm biến vào `environment:` của
`compose.yaml` là 2 lý do phổ biến nhất khiến VNPay báo *"VNPay chưa được cấu hình"* dù
bạn "đã thêm" giá trị ở đâu đó.

Đổi `environment:` trong `compose.yaml` hoặc `.env` xong phải `docker compose up -d app`
lại (không phải `restart` — `restart` không nạp lại biến môi trường mới).

### 1.3. ⚠️ Cảnh báo bảo mật: `.env` từng bị track trong git

`.claude/README.md` ghi nhận `.env` từng **được track trong git** (`git ls-files` xác
nhận lúc khảo sát), không nằm trong `.gitignore`. Trước khi commit bất cứ gì, tự kiểm
tra lại:

```bash
git ls-files | grep -x "\.env"
git check-ignore -v .env
```

Nếu `.env` đang được track: **coi mọi secret trong đó là đã lộ** (kể cả
`VNPAY_TMN_CODE`/`VNPAY_HASH_SECRET` sandbox vừa thêm) — gỡ khỏi tracking
(`git rm --cached .env`), thêm vào `.gitignore`, và cân nhắc xin cấp lại `HASH_SECRET`
mới từ VNPay nếu đây không chỉ là tài khoản sandbox dùng chung.

## 2. Luồng hoạt động (tóm tắt — chi tiết xem PROCESSING-FLOW.md mục 6)

```
Client → POST /api/v1/order (paymentMethod=CARD|INSTALLMENT)
  → OrdersServiceImpl.createOrder: tạo Orders(status=AWAITING_PAYMENT), KHÔNG trừ kho
  → PaymentServiceImpl.initiatePayment: tạo Payment(status=PENDING) + gọi VnpayService.buildPaymentUrl
Client ← 200 { orderId, paymentUrl }
Client → redirect trình duyệt sang paymentUrl (trang VNPay, khách nhập thẻ/OTP)

VNPay → GET/POST /api/v1/payment/vnpay/ipn (server-to-server, KHÔNG qua trình duyệt)
  → verify chữ ký → đối chiếu số tiền → trừ kho (StockService, khoá pessimistic) →
    Payment=SUCCESS, Orders=PENDING → publish OrderCreatedEvent (Kafka, tái dùng luồng COD)

VNPay → redirect trình duyệt về GET /api/v1/payment/vnpay/return?vnp_TxnRef=...
  → CHỈ đọc trạng thái hiện tại (không đổi gì) — IPN có thể chưa tới lúc này, client tự
    poll thêm GET /api/v1/payment/status?orderId=...
```

**3 quy tắc không được đổi mà không hiểu hệ quả** (chi tiết + lý do ở SKILL.md):
1. IPN là nguồn sự thật DUY NHẤT để đổi `Payment`/`Orders` status — return URL/`/status`
   chỉ đọc.
2. Verify chữ ký HMAC-SHA512 TRƯỚC KHI tin bất kỳ field nào trong IPN.
3. Đối chiếu `vnp_Amount` với `Payment.amount` đã lưu trước khi coi là thành công.

## 3. Chạy thử với sandbox thật — bắt buộc có URL public cho IPN

`APP_BASE_URL=http://localhost:18080` (mặc định) đủ cho `vnp_ReturnUrl` (trình duyệt của
chính khách gọi được `localhost`), nhưng **IPN là VNPay gọi từ server của họ trên
internet — không thể nào gọi tới `localhost` của máy bạn**. Thiếu bước này, redirect
sang VNPay + nhập thẻ vẫn chạy được, nhưng `Payment`/`Orders` sẽ **treo mãi ở
PENDING/AWAITING_PAYMENT** vì IPN không bao giờ tới nơi.

Các bước:

1. Chạy 1 tunnel public trỏ về `localhost:18080` (ví dụ `ngrok http 18080`, Cloudflare
   Tunnel, hoặc localtunnel) → có URL dạng `https://xxxx.ngrok-free.app`.
2. Sửa `.env`: `APP_BASE_URL=https://xxxx.ngrok-free.app`, rồi `docker compose up -d app`.
3. Đăng nhập **trang quản trị merchant sandbox của VNPay** (bằng tài khoản gắn với
   `VNPAY_TMN_CODE` đang dùng) và đăng ký/cập nhật **IPN URL** =
   `https://xxxx.ngrok-free.app/api/v1/payment/vnpay/ipn`. Đây thường là cấu hình phía
   VNPay (1 lần trên dashboard), không phải tham số gửi kèm mỗi request — tự thực hiện
   trên portal VNPay.
4. Đặt 1 đơn CARD/INSTALLMENT thật, theo dõi log app (`docker compose logs -f app`) xem
   IPN có tới không (`[HTTP POST] /api/v1/payment/vnpay/ipn`).

### Test card sandbox (theo tài liệu công khai của VNPay — có thể đã đổi, kiểm tra lại
trên trang sandbox VNPay hiện tại trước khi dùng)

| Trường | Giá trị |
|---|---|
| Ngân hàng | NCB |
| Số thẻ | 9704198526191432198 |
| Tên chủ thẻ | NGUYEN VAN A |
| Ngày phát hành | 07/15 |
| OTP | 123456 |

## 4. Chữ ký HMAC-SHA512 (`VnpayUtil`)

`VnpayUtil.buildSortedQueryString(params, "vnp_SecureHash")`:
1. Bỏ field chữ ký (`vnp_SecureHash`) và mọi field null/rỗng.
2. Sort key theo thứ tự tự nhiên của `String` (`TreeMap`).
3. **URL-encode từng value** (`URLEncoder.encode(value, UTF_8)`) rồi mới nối
   `key=value&key=value...`.
4. `VnpayUtil.hmacSHA512(hashSecret, chuỗi trên)` → hex lowercase 128 ký tự.

`VnpayServiceImpl.buildPaymentUrl` build URL redirect **trực tiếp từ đúng chuỗi đã ký ở
bước 3** (`payUrl + "?" + signedQuery + "&vnp_SecureHash=" + hash`) — cố ý dùng chung 1
nguồn, không build URL bằng 1 lượt encode khác, để chuỗi được ký và chuỗi thật sự gửi đi
luôn khớp nhau tuyệt đối.

**Bug lịch sử đã sửa (2026-08-20)**: bản đầu tính chữ ký trên giá trị THÔ (chưa encode)
nhưng build URL bằng giá trị ĐÃ encode riêng — 2 chuỗi lệch nhau ngay khi 1 value có ký
tự cần encode (`vnp_OrderInfo` luôn có dấu cách), khiến VNPay luôn báo sai chữ ký bất kể
`VNPAY_HASH_SECRET` đúng hay sai. Test hồi quy:
`VnpayUtilTest.buildSortedQueryStringUrlEncodesValuesContainingSpaces`.

`verifySignature(params)` dùng lại đúng `buildSortedQueryString` để re-encode giá trị
IPN nhận vào (Spring đã tự URL-decode `@RequestParam Map<String,String>` trước khi tới
đây) rồi so sánh bằng `MessageDigest.isEqual` (constant-time, chống timing attack).

## 5. Đối soát & retry (Phase 2)

| Cơ chế | Trạng thái |
|---|---|
| Tự động huỷ đơn `AWAITING_PAYMENT` quá hạn (`app.order.payment-ttl-minutes`, mặc định 15 phút) | Đã hiện thực (`PaymentReconciliationServiceImpl`, `@Scheduled`) |
| Đối soát Query API cho payment PENDING đủ cũ | Đã hiện thực (`VnpayServiceImpl.queryTransaction`, `querydr`) — **chưa verify format request/response với tài liệu thật**, xem chi tiết ngay dưới |
| `POST /api/v1/payment/vnpay/retry?orderId=...` | Đã hiện thực — tạo lại `paymentUrl` mới cho đơn `PAYMENT_FAILED`/`AWAITING_PAYMENT` |
| Tự động hoàn tiền | **Không có** — quyết định có chủ đích (rủi ro động vào tiền thật), admin xử lý tay qua merchant portal khi `Orders.adminNote` có cảnh báo cần đối soát |

### Chi tiết `queryTransaction` (querydr)

- Cần `Payment.vnpCreateDate` (cột `vnp_create_date`, thêm ở migration `V29`) — đúng
  giá trị `vnp_CreateDate` đã gửi lúc build URL thanh toán ban đầu, VNPay dùng để định vị
  đúng giao dịch qua `vnp_TransactionDate`. Payment tạo **trước** migration này sẽ có
  giá trị `null` → `queryTransaction` trả `null` ngay (bỏ qua, không đối soát được cho
  payment cũ).
- Ký request bằng **thuật toán RIÊNG của Query/Refund API** — nối field theo thứ tự CỐ
  ĐỊNH bằng dấu `|` (`VnpayUtil.buildPipeDelimitedHash`), **KHÁC HẲN**
  `buildSortedQueryString` (sort + URL-encode) dùng cho payment URL/IPN. Convention phổ
  biến, **chưa verify với tài liệu thật**:
  `vnp_RequestId|vnp_Version|vnp_Command|vnp_TmnCode|vnp_TxnRef|vnp_TransactionDate|vnp_CreateDate|vnp_IpAddr|vnp_OrderInfo`.
- Gọi POST JSON tới `vnpay.api-url` (`RestTemplate`, cùng pattern `TelegramService`).
- **Không verify chữ ký response** — quyết định có chủ đích: đây là cuộc gọi HTTPS GHD
  chủ động gọi thẳng domain thật VNPay (TLS đã xác thực server, khác IPN nơi bất kỳ ai
  cũng POST được vào endpoint public của GHD), và thuật toán ký cho response của Query
  API càng chưa chắc chắn hơn cả request.
- `vnp_TransactionStatus` từ response: `"00"` → coi là thành công, `"01"` → vẫn đang xử
  lý (trả `null`, bỏ qua, chờ lần đối soát sau), giá trị khác → coi là thất bại. Map
  thẳng sang `vnp_ResponseCode` để tái dùng `PaymentService.applyGatewayResult` (logic
  y hệt `handleIpn` — idempotency, đối chiếu số tiền, trừ kho — không viết trùng).

Bất biến bắt buộc: **tối đa 1 `Payment` đang PENDING cho 1 order tại mọi thời điểm**,
giữ bởi `PaymentServiceImpl.initiatePayment` luôn gọi `cancelAnyExistingPendingPayments`
trước khi tạo payment mới. Nếu thêm chỗ mới tạo `Payment`, phải đi qua
`initiatePayment` hoặc tự giữ bất biến này — vi phạm từng gây bug thật (QA subagent phát
hiện), xem SKILL.md mục "Bất biến bắt buộc".

## 6. Test hiện có (không cần sandbox thật)

| File | Cover gì |
|---|---|
| `src/test/java/.../utils/VnpayUtilTest.java` | Sort + URL-encode + HMAC-SHA512 + so sánh constant-time + `buildPipeDelimitedHash` (Query API) |
| `src/test/java/.../services/PaymentServiceImplIntegrationTest.java` | verify chữ ký, idempotency IPN, đối chiếu số tiền, trừ kho, case hết hàng lúc IPN về, case IPN "thành công" cho payment đã CANCELLED |
| `src/test/java/.../services/PaymentReconciliationServiceImplIntegrationTest.java` | auto-cancel quá hạn + `queryTransaction` trả null/thành công/thất bại (`@MockitoBean VnpayService`) áp dụng đúng qua `applyGatewayResult` |
| `src/test/java/.../security/PaymentPublicUrlsTest.java` | `/api/v1/payment/**` không bị chặn bởi JWT (public đúng thiết kế) |

Chạy: `.\mvnw.cmd test -Dtest=VnpayUtilTest,PaymentServiceImplIntegrationTest,PaymentReconciliationServiceImplIntegrationTest,PaymentPublicUrlsTest`

## 7. Checklist trước khi go-live thật

- [ ] Xác nhận lại toàn bộ tên field (`vnp_*`), method HTTP của IPN (GET/POST — code
      hiện chấp nhận cả 2), bảng mã `vnp_ResponseCode`, format `vnp_Amount` (x100), và
      có cần gửi kèm `vnp_SecureHashType` khi build URL không (code hiện tại không gửi
      field này) — đối chiếu với tài liệu merchant VNPay thật (không phải sandbox demo).
- [ ] `.env` không bị track trong git, secret không nằm trong git history (mục 1.3).
- [ ] `APP_BASE_URL` trỏ đúng domain production thật (không phải tunnel dev/localhost).
- [ ] IPN URL đã đăng ký đúng trên merchant portal VNPay **production** (khác sandbox).
- [ ] Chạy end-to-end thật trên sandbox cho cả CARD và INSTALLMENT trước khi chuyển
      `VNPAY_*` sang giá trị production.
- [ ] Xác nhận lại format request/response + thuật toán ký thật của Query API (`querydr`,
      mục 5) — hiện code dùng convention phổ biến (pipe-delimited theo thứ tự cố định),
      chưa verify field/thứ tự với tài liệu merchant thật.

## Chưa xác nhận với tài liệu thật

Toàn bộ tên field/format cụ thể trong code (`vnp_TxnRef`, `vnp_Amount` × 100,
`vnp_ResponseCode`, `vnp_TransactionNo`, `vnp_BankCode`, mã `"00"` = thành công,
request/response + thuật toán ký pipe-delimited của Query API `querydr`...) là
**convention phổ biến của VNPay**, được viết trước khi project có tài khoản sandbox thật.
Giờ đã có `VNPAY_TMN_CODE`/`VNPAY_HASH_SECRET` sandbox thật, nên đối chiếu lại với tài
liệu API chính thức tải từ trang sandbox VNPay trước khi tin tưởng hoàn toàn — xem mục 7.
