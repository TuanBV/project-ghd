---
name: vnpay-payment
description: Tích hợp thanh toán VNPay (thẻ ngân hàng + trả góp) của GHD - verify chữ ký, idempotency IPN, quy tắc return-URL-không-đáng-tin. Đọc trước khi đụng PaymentController/PaymentServiceImpl/VnpayServiceImpl.
---

# VNPay payment — GHD

Nguồn: `controllers/api/PaymentController.java`, `services/impl/PaymentServiceImpl.java`,
`services/impl/VnpayServiceImpl.java`, `utils/VnpayUtil.java`, entity `Payment`.
Plan gốc của tính năng này (bối cảnh đầy đủ, đã duyệt): xem lịch sử conversation hoặc
`docs/PROCESSING-FLOW.md` mục thanh toán VNPay.

**CHƯA có sandbox VNPay thật khi tính năng này được viết.** Mọi tên field cụ thể
(`vnp_TxnRef`, `vnp_Amount` nhân 100, `vnp_ResponseCode`, `vnp_TransactionNo`,
`vnp_BankCode`, mã "00"...) là **convention phổ biến của VNPay, chưa được verify với tài
liệu merchant thật**. Trước khi go-live: xác nhận lại toàn bộ tên field, method HTTP của
IPN (GET/POST), bảng mã response, và format Query API với tài liệu merchant thật.

## 3 quy tắc bắt buộc — không đổi mà không hiểu rõ hệ quả

1. **IPN (`/api/v1/payment/vnpay/ipn`) là nguồn sự thật DUY NHẤT** để đổi
   `Payment.status`/`Orders.status`. Return URL (`/vnpay/return`) và `/status` **CHỈ
   ĐỌC** — không bao giờ mutate state. Lý do: return URL do trình duyệt gọi, khách có
   thể replay/tự chế URL với param bất kỳ; nếu tin vào đó để đổi trạng thái, khách có
   thể tự đánh dấu đơn "đã thanh toán" mà VNPay chưa hề xác nhận.
2. **Verify chữ ký trước khi tin BẤT KỲ field nào trong IPN**
   (`VnpayService.verifySignature`, HMAC-SHA512, so sánh bằng
   `MessageDigest.isEqual`/`VnpayUtil.isSignatureValid` - constant-time, không dùng
   `String#equals`). Sai chữ ký → không đụng DB, trả lỗi ngay.
3. **Đối chiếu số tiền IPN với `Payment.amount` đã lưu** trước khi coi là thành công —
   đây là lớp lỗ hổng tích hợp VNPay hay gặp nhất (tin số tiền do client/gateway gửi mà
   không so với số tiền server đã chốt lúc tạo đơn).

## Idempotency (VNPay có thể gọi IPN nhiều lần cho cùng giao dịch)

Cơ chế chống trùng = `PaymentRepository.findByTxnRefForUpdate` (khoá pessimistic,
`SELECT ... FOR UPDATE`) + check `payment.status != PENDING` trước khi làm bất kỳ side
effect nào. Nếu đã xử lý rồi, trả về response xác nhận (`VnpayIpnResponse.ok()`) để
VNPay ngừng gọi lại — **không** làm lại decrement kho/publish Kafka lần 2.

`VnpayIpnResponse.rspCode` (`"00"`/`"Confirm Success"`) nghĩa là **"GHD đã nhận và xử lý
xong IPN này"**, KHÁC với `vnp_ResponseCode` (kết quả thanh toán thật từ VNPay). Trả
`rspCode=00` là đúng ngay cả khi kết quả nghiệp vụ là thất bại (thanh toán bị từ chối/
huỷ) — chỉ dùng `rspCode` khác 00 khi bản thân request IPN có vấn đề (sai chữ ký, không
tìm thấy payment, sai số tiền).

## Tồn kho — chỉ trừ SAU khi IPN xác nhận thành công

`OrdersServiceImpl.createOrder` KHÔNG trừ kho cho `CARD`/`INSTALLMENT` (khác COD/
BANK_TRANSFER, giữ nguyên hành vi cũ). Trừ kho thật xảy ra trong
`PaymentServiceImpl.handleIpn` qua `StockService.decrementStockForOrder` — **dùng chung**
logic khoá pessimistic (`ProductRepository.findByIdForUpdate`) với luồng COD, không viết
trùng ở PaymentServiceImpl. Nếu cần đổi logic trừ kho, sửa `StockServiceImpl` một chỗ,
không sửa riêng từng caller.

**Hết hàng đúng lúc IPN xác nhận** (2 khách cùng vào gateway cho sản phẩm sắp hết —
đánh đổi đã chấp nhận, xem plan gốc): `Payment.status = SUCCESS` (tiền đã thu, không
được ghi sai), `Orders.status = PAYMENT_FAILED`, `adminNote` tự ghi cảnh báo cần hoàn
tiền thủ công. **v1 không tự động hoàn tiền** — admin xử lý tay qua merchant portal
VNPay (`VnpayService.queryTransaction` mới chỉ khai interface, chưa hiện thực — phase 2).

## Circular dependency — vì sao PaymentServiceImpl không phụ thuộc OrdersService

`OrdersServiceImpl` gọi `PaymentService.initiatePayment` (tạo Payment + URL). Nếu
`PaymentServiceImpl` lại phụ thuộc `OrdersService` (interface, implement bởi
`OrdersServiceImpl`) để cập nhật đơn sau IPN → circular bean dependency, Spring fail lúc
khởi động với constructor injection (`@RequiredArgsConstructor`). Giải quyết:
`PaymentServiceImpl` dùng thẳng `OrdersRepository` (Service → Repository luôn hợp lệ) và
`StockService` (thấp hơn, không phụ thuộc ngược Payment/Orders service nào). Khi thêm
dependency mới giữa các service liên quan tới payment/order, vẽ lại đồ thị phụ thuộc
trước để tránh lặp lại vấn đề này.

## Phase 2 — auto-cancel / reconcile scaffold / retry (đã triển khai)

Nguồn: `config/SchedulingConfig.java` (`@EnableScheduling` - hạ tầng scheduling ĐẦU TIÊN
của project), `services/impl/PaymentReconciliationServiceImpl.java`.

- **Tự động huỷ đơn `AWAITING_PAYMENT` quá hạn**: job `@Scheduled` (mặc định mỗi 60s,
  `app.scheduling.cancel-stale-orders-fixed-delay-ms`) gọi
  `cancelExpiredAwaitingPayments()`, dùng TTL `app.order.payment-ttl-minutes` (mặc định
  15 phút, env `ORDER_PAYMENT_TTL_MINUTES`). Khoá đúng row `Payment` mà
  `PaymentServiceImpl.handleIpn` cũng khoá
  (`PaymentRepository.findFirstByOrderIdForUpdateOrderByCreatedDateDesc`) trước khi
  huỷ - nếu IPN đang xử lý đồng thời cho cùng đơn, job này đợi rồi tự bỏ qua, không bao
  giờ đè lên kết quả IPN. Dùng lại `OrderStatus.CANCELLED` (không có enum riêng cho
  "hết hạn") - phân biệt bằng `adminNote`.
- **Đối soát Query API — CHỈ LÀ KHUNG**: job `@Scheduled` thứ 2 (mặc định mỗi 5 phút,
  `app.scheduling.reconcile-pending-fixed-delay-ms`) gọi `reconcilePendingPayments()`,
  tìm `Payment` PENDING đủ cũ (`app.payment.reconcile-grace-minutes`, mặc định 3 phút)
  nhưng chưa tới hạn bị huỷ, rồi gọi `VnpayService.queryTransaction(txnRef)`. Method đó
  **CHƯA hiện thực thật** (throw `UnsupportedOperationException` có chủ đích) - job chỉ
  log và bỏ qua, không đổi trạng thái gì. **Khi bạn hiện thực `queryTransaction` thật
  (có sandbox), job này tự động có tác dụng** mà không cần sửa lịch chạy - chỉ cần thêm
  logic xử lý kết quả trả về trong `reconcilePendingPayments` (đánh dấu rõ `// TODO`
  trong code).
- **Retry endpoint**: `POST /api/v1/payment/vnpay/retry?orderId=...` cho đơn CARD/
  INSTALLMENT đang `PAYMENT_FAILED`/`AWAITING_PAYMENT` - reset về `AWAITING_PAYMENT` rồi
  gọi lại nguyên `initiatePayment` (không viết trùng). Từ chối đơn COD/BANK_TRANSFER và
  đơn đã ở trạng thái khác (đã thanh toán xong).
- **KHÔNG có tự động hoàn tiền** (quyết định có chủ đích - rủi ro di chuyển tiền thật
  khi chưa test với sandbox) và **KHÔNG có trừu tượng hoá multi-gateway** (YAGNI, field
  `provider` trên `Payment` đã đủ dự phòng).

## Bất biến bắt buộc: tối đa 1 Payment PENDING cho 1 order (Phase 2, phát hiện qua QA)

`retryPayment` (và job auto-cancel) chỉ đúng nếu **tại mọi thời điểm, 1 order có tối đa
1 `Payment` đang PENDING**. Vi phạm bất biến này (đã từng xảy ra thật ở bản đầu của
`retryPayment` — QA subagent phát hiện) gây 2 hậu quả nghiêm trọng: job huỷ hạn/idempotency
xử lý nhầm payment "mồ côi", và IPN cho payment cũ có thể bị bỏ qua dù tiền đã được thu.

Bất biến được giữ bởi **duy nhất 1 chỗ**:
`PaymentServiceImpl.initiatePayment` luôn gọi `cancelAnyExistingPendingPayments(orderId)`
(huỷ payment PENDING mới nhất, nếu có) **trước khi** tạo payment mới — áp dụng cho cả
tạo đơn lần đầu (`OrdersServiceImpl.createOrder`) lẫn `retryPayment`. **Nếu sau này thêm
bất kỳ chỗ nào khác tạo `Payment` mà không đi qua `initiatePayment`, bất biến này vỡ** —
phải gọi `cancelAnyExistingPendingPayments` (hoặc tương đương) ở đó, hoặc tái cấu trúc để
mọi nơi tạo Payment đều đi qua `initiatePayment`.

## Thứ tự khoá bắt buộc: Payment TRƯỚC, Orders SAU — không bao giờ đảo ngược

Mọi luồng khoá pessimistic (`FOR UPDATE`) trong khu vực payment phải khoá **Payment
trước, Orders sau (nếu có)** — `handleIpn`, `initiatePayment`/`cancelAnyExistingPendingPayments`,
`retryPayment`, `PaymentReconciliationServiceImpl.cancelOneIfStillStale` đều theo đúng
thứ tự này. **Đảo ngược thứ tự ở bất kỳ đâu (khoá Orders trước rồi mới khoá Payment) sẽ
tạo nguy cơ deadlock InnoDB thật** với các luồng khác đang khoá đúng thứ tự (đã từng xảy
ra ở bản đầu của `retryPayment` — QA subagent phát hiện ở vòng review thứ 2). Khi thêm
logic mới cần khoá cả 2 bảng, luôn khoá Payment trước.

## `handleIpn` không được âm thầm bỏ qua IPN "thành công" cho payment đã CANCELLED/FAILED

Khi `payment.getStatus() != PENDING`, phải phân biệt: **payment đã SUCCESS** (trùng lặp
IPN thật, an toàn bỏ qua, chỉ log INFO) **khác với payment đã CANCELLED/FAILED nhưng IPN
lần này lại báo thành công** (`vnp_ResponseCode == "00"`) — trường hợp sau nghĩa là VNPay
có thể đã thu tiền cho 1 payment mà GHD đã coi là "xong" (hết hạn, hoặc bị 1 lần
`retryPayment` thay thế bằng payment mới, rồi khách vẫn hoàn tất thanh toán qua link cũ).
Đây **không được** coi là trùng lặp bình thường — phải `log.error` + gọi
`flagOrderForManualReconciliation` (ghi `adminNote`, chỉ khi đơn chưa được xác nhận
thành công qua đường khác) để admin đối soát thủ công, cùng tinh thần với case "hết hàng
lúc IPN về" ở mục Tồn kho phía trên. Xem `PaymentServiceImpl.handleIpn` +
`flagOrderForManualReconciliation`.

## Checklist khi sửa code vùng này

- [ ] Không đổi return URL (`/vnpay/return`) hay `/status` để mutate state - chỉ IPN
      được phép.
- [ ] Mọi thay đổi ở `handleIpn` vẫn giữ verify chữ ký là bước ĐẦU TIÊN.
- [ ] Không bỏ qua đối chiếu số tiền dù chỉ tạm thời để debug.
- [ ] Nếu thêm chỗ mới tạo `Payment`, đảm bảo vẫn đi qua `initiatePayment` (giữ bất biến
      tối đa 1 PENDING/order) — xem mục "Bất biến bắt buộc" phía trên.
- [ ] Nếu thêm logic khoá `FOR UPDATE` mới liên quan Payment/Orders, giữ đúng thứ tự
      Payment trước - Orders sau — xem mục "Thứ tự khoá bắt buộc" phía trên.
- [ ] Nếu thêm field/response code VNPay mới, ghi rõ trong code là "convention phổ biến -
      cần xác nhận với tài liệu merchant thật", đừng khẳng định như đã chắc chắn đúng.
- [ ] Test mới cho vùng này (không cần sandbox thật) đặt cùng chỗ với
      `VnpayUtilTest`/`PaymentServiceImplIntegrationTest`/`PaymentPublicUrlsTest`
      (`src/test/java/guru/springframework/ghd/utils`, `.../services`, `.../security`).
- [ ] Trước go-live thật: xác nhận lại toàn bộ tên field/method HTTP/mã response với
      tài liệu merchant VNPay thật, chạy end-to-end trên sandbox cho cả CARD và
      INSTALLMENT.
