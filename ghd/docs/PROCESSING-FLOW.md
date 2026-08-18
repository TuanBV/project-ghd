# Luồng xử lý của hệ thống (GHD)

Tài liệu này mô tả các luồng xử lý runtime chính của backend GHD: request đi qua những
lớp nào, luồng nghiệp vụ quan trọng (đặt hàng, đăng ký user) xử lý ra sao, và phần xử
lý bất đồng bộ qua Kafka được thêm vào ở đâu. Xem thêm `docs/PROJECT_INDEX.md` (cấu
trúc source code) và `docs/DOCKER.md` (vận hành Docker).

## 1. Luồng xử lý request chung

```mermaid
sequenceDiagram
    participant C as Client
    participant F as JwtAuthenticationFilter
    participant GC as GlobalCommon<br/>(@ControllerAdvice, chỉ áp dụng<br/>cho controllers.views)
    participant Ctrl as Controller<br/>(api/* hoặc views/*)
    participant Svc as Service
    participant Repo as Repository (JPA)
    participant DB as MySQL

    C->>F: HTTP request (kèm cookie JWT nếu có)
    F->>F: Đọc token từ cookie, validate,<br/>set SecurityContext nếu hợp lệ
    F->>Ctrl: filterChain.doFilter(...)
    alt Request vào /admin/v1/** hoặc / (Thymeleaf view)
        Ctrl->>GC: @ModelAttribute (navBarData, hotline, sysParams, currentUrl)
        GC->>Svc: categoryService/brandService/productService.getAll()
        Svc->>DB: SELECT ...
    end
    Ctrl->>Svc: gọi service method
    Svc->>Repo: gọi repository
    Repo->>DB: SQL (JPQL hoặc native @Query)
    DB-->>Repo: kết quả
    Repo-->>Svc: entity/projection
    Svc-->>Ctrl: DTO response
    Ctrl-->>C: JSON (ApiResponse) hoặc render Thymeleaf
    F->>F: finally: log [HTTP method] path -> status,<br/>copy response body ra ngoài
```

Ghi chú:

- `JwtAuthenticationFilter` chạy cho **mọi** request (`OncePerRequestFilter`), nhưng chỉ
  set `SecurityContext` nếu có cookie JWT hợp lệ - route công khai vẫn đi qua bình
  thường. Filter này cũng log request/response body (ẩn body nếu là HTML/Thymeleaf).
- `GlobalCommon` **chỉ** áp dụng cho `controllers.views` (`ViewAdminController`,
  `ViewClientController`) kể từ bản sửa gần nhất - trước đó nó là `@ControllerAdvice`
  không giới hạn phạm vi nên chạy cả trên API JSON (`controllers.api`), gây load thừa
  toàn bộ category/brand/product mỗi request và từng làm nhiễu một pessimistic lock (xem
  mục 2). Route `/api/v1/**` không còn bị ảnh hưởng bởi advice này.
- Các danh sách đọc nhiều/ít đổi (category, brand, banner, slider, policy, sys-param,
  product theo slug...) có `@Cacheable` vào Redis, xem `config/CacheConfig.java` để biết
  TTL từng cache.

## 2. Luồng đặt hàng (`POST /api/v1/order`)

```mermaid
sequenceDiagram
    participant C as Client
    participant Ctrl as OrderController
    participant Svc as OrdersServiceImpl
    participant Repo as ProductRepository /<br/>OrdersRepository
    participant DB as MySQL
    participant K as Kafka (order-events)
    participant L as OrderEventListener
    participant T as TelegramService

    C->>Ctrl: POST /api/v1/order (OrderRequest)
    Ctrl->>Svc: createOrder(request)  [@Transactional]
    Svc->>DB: INSERT orders (status=PENDING)
    loop mỗi item trong đơn hàng
        Svc->>Repo: findByIdForUpdate(productId)<br/>(SELECT ... FOR UPDATE)
        Repo->>DB: khóa row sản phẩm cho tới khi transaction kết thúc
        alt tồn kho không đủ
            Svc-->>Ctrl: throw RuntimeException("không đủ hàng")
            Ctrl-->>C: 400 - transaction rollback, không đơn nào được tạo
        else đủ hàng
            Svc->>DB: UPDATE product (trừ stock_qty, cộng sold_count)
            Svc->>Svc: gom OrderItemInfo cho event
        end
    end
    Svc->>DB: INSERT order_detail (saveAll)
    Svc->>DB: COMMIT
    Note over Svc,K: Sau khi commit thành công (afterCommit callback)
    Svc->>K: publish OrderCreatedEvent (key = orderId)
    Svc-->>Ctrl: return
    Ctrl-->>C: 200 OK (trả về ngay, KHÔNG chờ Telegram)

    par Xử lý bất đồng bộ (consumer group ghd-notifications)
        K->>L: onOrderCreated(event)
        L->>L: build message Telegram (HTML, escape)
        L->>T: sendMessage(...)
        alt Telegram lỗi (network/API down)
            L->>L: DefaultErrorHandler retry 3 lần (2s/lần)
            Note over L: Hết retry -> log lỗi, bỏ qua message<br/>(không ảnh hưởng đơn hàng đã tạo)
        end
    end
```

Điểm quan trọng:

- **Chống oversell**: `findByIdForUpdate` khóa row sản phẩm bằng `FOR UPDATE` ngay
  trong transaction tạo đơn. Nếu 2 request đặt hàng cùng sản phẩm gần như đồng thời,
  request thứ hai phải đợi request thứ nhất commit xong mới đọc được tồn kho, nên luôn
  thấy đúng số lượng còn lại (không đọc phải giá trị cũ, không dùng optimistic-lock
  version conflict để chặn).
- **Không chờ Telegram**: trước đây `TelegramService.sendMessage(...)` được gọi trực
  tiếp trong `afterCommit`, tức là vẫn chạy đồng bộ trên thread xử lý request - client
  phải đợi Telegram API trả lời mới nhận response. Giờ `afterCommit` chỉ publish 1
  message JSON nhỏ lên Kafka (rất nhanh), còn việc gọi Telegram thật sự chuyển hẳn sang
  `OrderEventListener` chạy trên thread riêng của Kafka consumer.
- Lỗi ở bước gửi Telegram (kể cả sau khi hết retry) **không** rollback hay ảnh hưởng gì
  đến đơn hàng - đơn đã được lưu chắc chắn trước khi event được publish.

## 3. Luồng đăng ký user (`POST /api/v1/user`)

```mermaid
sequenceDiagram
    participant C as Client (admin)
    participant Ctrl as UserController
    participant Svc as UserServiceImpl
    participant DB as MySQL
    participant K as Kafka (user-events)
    participant L as UserEventListener
    participant E as EmailService (SMTP)

    C->>Ctrl: POST /api/v1/user (multipart form)
    Ctrl->>Svc: addUser(request)
    Svc->>DB: INSERT user (password đã encode)
    opt có avatar
        Svc->>DB: INSERT user_image
    end
    Svc->>K: publish UserRegisteredEvent (key = userId)
    Note over Svc: lỗi publish chỉ log warn,<br/>không làm fail request tạo user
    Svc-->>Ctrl: UserResponse
    Ctrl-->>C: 200 OK (trả về ngay, KHÔNG chờ email)

    par Xử lý bất đồng bộ (consumer group ghd-notifications)
        K->>L: onUserRegistered(event)
        L->>E: sendWelcomeEmail(email, username)
        E->>E: gửi mail qua smtp.gmail.com
        alt SMTP lỗi
            L->>L: retry 3 lần (2s/lần) rồi log-and-skip
        end
    end
```

Trước đây `EmailService.sendWelcomeEmail` có `@Async` nhưng project không bật
`@EnableAsync` ở đâu cả, nên annotation này **vô hiệu** - gửi mail chạy đồng bộ
(SMTP handshake) ngay trong request tạo user. Giờ việc gửi mail chạy hẳn trong Kafka
consumer thread, tách khỏi request thread một cách thực sự.

## 4. Hạ tầng Kafka dùng chung cho 2 luồng trên

| | |
|---|---|
| Bootstrap servers | `localhost:9092` (chạy app ngoài Docker) / `kafka:19092` (app trong cùng docker network - internal listener) |
| Topics | `order-events`, `user-events` (1 partition/topic, đủ cho khối lượng hiện tại - xem comment trong `KafkaTopicConfig`) |
| Consumer group | `ghd-notifications` (dùng chung cho cả `OrderEventListener` và `UserEventListener`) |
| Serialization | Key: `StringSerializer`/`StringDeserializer`. Value: `JsonSerializer`/`JsonDeserializer` (Jackson 2, có sẵn qua `jjwt-jackson`) |
| Retry khi consumer lỗi | `KafkaConsumerConfig` - `DefaultErrorHandler` với `FixedBackOff(2000ms, 3 lần)`, hết retry thì log lỗi và bỏ qua message (không có dead-letter topic ở bản hiện tại) |
| File liên quan | `config/KafkaTopicConfig.java`, `config/KafkaConsumerConfig.java`, `events/*.java`, `listeners/*.java` |

Xem `docs/DOCKER.md` mục 12 để biết cách chạy Kafka qua Docker Compose và cách xem
message trong topic bằng `kafka-console-consumer`.

## 5. Luồng đăng nhập admin (`POST /api/v1/auth/login`)

```mermaid
sequenceDiagram
    participant C as Client
    participant Ctrl as AuthenticationController
    participant AM as AuthenticationManager
    participant UDS as CustomerUserDetailsService
    participant DB as MySQL
    participant J as JwtService

    C->>Ctrl: POST /api/v1/auth/login (username, password)
    Ctrl->>AM: authenticate(username, password)
    AM->>UDS: loadUserByUsername(username)
    UDS->>DB: SELECT user WHERE username=...
    alt sai username/password
        AM-->>Ctrl: UsernameNotFoundException / BadCredentialsException
        Ctrl-->>C: 401
    else hợp lệ
        AM-->>Ctrl: Authentication (principal = User)
        Ctrl->>J: generateToken(username)
        Ctrl-->>C: 200 + Set-Cookie (JWT) + LoginResponse
    end
```

Từ request tiếp theo, `JwtAuthenticationFilter` (mục 1) đọc cookie này, validate và set
`SecurityContext` cho mỗi request tới `/api/v1/**`/`/admin/v1/**` - session Thymeleaf
(Redis) chỉ dùng cho cookie `GHDSESSION` của phần view, tách biệt với cơ chế JWT.

## 6. Thanh toán online qua VNPay (thẻ ngân hàng + trả góp)

Chi tiết đầy đủ + quy tắc bắt buộc: xem
[.claude/skills/vnpay-payment/SKILL.md](../.claude/skills/vnpay-payment/SKILL.md).
**Chưa có sandbox VNPay thật** khi luồng này được viết - tên field `vnp_*` dưới đây là
convention phổ biến, chưa được xác nhận với tài liệu merchant thật.

```mermaid
sequenceDiagram
    participant C as Client (trình duyệt)
    participant Ctrl as OrderController
    participant Svc as OrdersServiceImpl
    participant PSvc as PaymentServiceImpl
    participant VNP as VnpayServiceImpl
    participant DB as MySQL
    participant V as VNPay (bên ngoài)

    C->>Ctrl: POST /api/v1/order (paymentMethod=CARD|INSTALLMENT)
    Ctrl->>Svc: createOrder(request, clientIp)
    Svc->>DB: INSERT orders (status=AWAITING_PAYMENT)
    Svc->>DB: INSERT order_detail (KHÔNG trừ stockQty ở bước này)
    Svc->>PSvc: initiatePayment(order, paymentMethod, clientIp)
    PSvc->>DB: INSERT payment (status=PENDING, txnRef sinh mới)
    PSvc->>VNP: buildPaymentUrl(payment, clientIp)
    VNP-->>PSvc: URL đã ký HMAC-SHA512
    PSvc-->>Svc: paymentUrl
    Svc-->>Ctrl: OrderCreationResult(orderId, paymentUrl)
    Ctrl-->>C: 200 { orderId, paymentUrl }
    C->>V: redirect (window.location.href = paymentUrl)
    Note over C,V: Khách tự chọn thẻ/ngân hàng/đối tác trả góp và nhập KYC<br/>hoàn toàn trên trang VNPay - GHD không thu thập KYC.

    par IPN (server-to-server, nguồn sự thật DUY NHẤT)
        V->>Ctrl: GET/POST /api/v1/payment/vnpay/ipn (vnp_TxnRef, vnp_Amount, vnp_ResponseCode...)
        Ctrl->>PSvc: handleIpn(params)
        PSvc->>PSvc: verifySignature (HMAC-SHA512) - sai thì dừng, không đụng DB
        PSvc->>DB: SELECT payment FOR UPDATE theo txnRef (chống trùng IPN)
        alt payment đã xử lý trước đó
            PSvc-->>Ctrl: rspCode=00 (bỏ qua, không lặp side effect)
        else số tiền không khớp payment.amount
            PSvc->>DB: payment=FAILED, order=PAYMENT_FAILED
            PSvc-->>Ctrl: rspCode=04
        else gateway báo thành công (typical vnp_ResponseCode=00)
            PSvc->>Svc: (qua StockService) decrementStockForOrder - dùng lại<br/>đúng khoá pessimistic findByIdForUpdate của luồng COD
            alt đủ hàng
                PSvc->>DB: payment=SUCCESS, order=PENDING (nhập lại luồng admin bình thường)
                PSvc->>PSvc: publish OrderCreatedEvent lên order-events (afterCommit)<br/>- tái dùng nguyên OrderEventListener/TelegramService
            else hết hàng đúng lúc IPN về (đánh đổi đã chấp nhận)
                PSvc->>DB: payment=SUCCESS (tiền đã thu, không ghi sai)<br/>order=PAYMENT_FAILED + adminNote cảnh báo hoàn tiền thủ công
            end
            PSvc-->>Ctrl: rspCode=00
        else gateway báo thất bại/huỷ
            PSvc->>DB: payment=FAILED, order=PAYMENT_FAILED
            PSvc-->>Ctrl: rspCode=00 (đã xử lý xong IPN, dù kết quả nghiệp vụ là fail)
        end
    and Return URL (chỉ hiển thị, KHÔNG đổi trạng thái)
        V->>C: redirect trình duyệt về /api/v1/payment/vnpay/return?vnp_TxnRef=...
        C->>Ctrl: GET /vnpay/return
        Ctrl->>PSvc: getStatusByTxnRef(txnRef)
        PSvc-->>C: trạng thái hiện tại (có thể vẫn PENDING nếu IPN chưa tới - client tự poll /status)
    end
```

Điểm quan trọng:

- **IPN là nguồn sự thật duy nhất**: return URL do trình duyệt gọi, tham số có thể bị
  replay/tự chế - không bao giờ dùng để đổi `Payment`/`Orders` status.
- **Tồn kho chỉ trừ sau khi IPN xác nhận thành công**, dùng lại đúng
  `StockService.decrementStockForOrder` (cùng khoá pessimistic `findByIdForUpdate`) với
  luồng COD/BANK_TRANSFER - không viết trùng logic ở 2 nơi.
- **COD/BANK_TRANSFER không đổi hành vi**: vẫn trừ kho ngay lúc tạo đơn và publish
  `OrderCreatedEvent` ngay, như trước khi có tính năng VNPay.
- `PaymentServiceImpl` **không** phụ thuộc `OrdersService` (tránh circular bean
  dependency vì `OrdersServiceImpl` đã phụ thuộc `PaymentService`) - dùng thẳng
  `OrdersRepository`/`StockService` thay vào đó.

**Phase 2** (2 job `@Scheduled` + retry, xem
[.claude/skills/vnpay-payment/SKILL.md](../.claude/skills/vnpay-payment/SKILL.md) mục
"Phase 2"): đơn `AWAITING_PAYMENT` quá hạn (mặc định 15 phút) tự động chuyển
`CANCELLED`; job đối soát Query API mới chỉ là khung (chưa gọi VNPay thật); endpoint
`POST /api/v1/payment/vnpay/retry` cho phép tạo lại `paymentUrl` mới cho đơn
`PAYMENT_FAILED`/`AWAITING_PAYMENT`.
