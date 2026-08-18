---
name: kafka-events
description: 3 Kafka topic thật của GHD (order/user/analytics events), consumer group, retry không dead-letter, và cách thêm 1 event mới đúng pattern hiện có. Đọc trước khi đụng events/listeners hoặc thêm luồng async mới.
---

# Kafka — GHD

Nguồn: `config/KafkaTopicConfig.java`, `config/KafkaConsumerConfig.java`,
`events/*.java`, `listeners/*.java`. Sequence diagram đầy đủ:
[docs/PROCESSING-FLOW.md](../../../docs/PROCESSING-FLOW.md) mục 2–4.

## Topic thật

| Topic | Partitions | Dùng cho |
|---|---|---|
| `order-events` | 1 | `OrderCreatedEvent` → `OrderEventListener` → gửi Telegram |
| `user-events` | 1 | `UserRegisteredEvent` → `UserEventListener` → gửi email chào mừng |
| `analytics-events` | 3 | `PageViewEvent` → `AnalyticsEventListener` — nhiều partition hơn vì throughput pageview cao hơn order/registration |

Consumer group dùng chung cho order/user: **`ghd-notifications`**
(`spring.kafka.consumer.group-id`). Analytics có listener riêng
(`AnalyticsEventListener`) nhưng vẫn cùng cấu hình consumer chung trong
`KafkaConsumerConfig`.

Serialization: key = `StringSerializer`/`StringDeserializer`, value =
`JsonSerializer`/`ErrorHandlingDeserializer` bọc `JsonDeserializer` (Jackson).
`spring.kafka.consumer.properties.spring.json.trusted.packages=guru.springframework.ghd.events`
— event class mới **phải** nằm trong package `events/` để deserialize được, không đặt
ở nơi khác.

## Retry — không có dead-letter topic

`KafkaConsumerConfig` dùng `DefaultErrorHandler` với `FixedBackOff(2000ms, 3 lần)`. Hết
retry → **log lỗi và bỏ qua message**, không có DLQ ở bản hiện tại. Nghĩa là:

- Lỗi gửi Telegram/email sau khi hết retry **không** ảnh hưởng transaction gốc (đơn
  hàng/user đã lưu chắc chắn trước khi publish event) — đây là thiết kế cố ý.
  - Nhưng cũng nghĩa là message mất vĩnh viễn nếu lỗi kéo dài quá 3 lần retry (~6s) —
    nếu thêm 1 luồng mới mà mất message là không chấp nhận được (vd tác vụ tài chính),
    **phải nói rõ với người dùng** trước khi tái dùng pattern này, không mặc định copy.

## Điểm dễ quên khi thêm event mới

1. Event class mới đặt trong `events/`, immutable/POJO đơn giản, serialize được bằng
   Jackson.
2. Đăng ký topic mới trong `KafkaTopicConfig` (bean `NewTopic`, chọn số partition theo
   throughput dự kiến — xem comment có sẵn trong file để biết lý do chọn 1 vs 3).
3. Listener mới trong `listeners/`, cùng consumer group `ghd-notifications` nếu là
   luồng notification, hoặc group riêng nếu cần xử lý độc lập.
4. Publish **sau khi commit transaction** (pattern `afterCommit`/`TransactionSynchronization`
   như `OrdersServiceImpl`), không publish giữa transaction đang mở — tránh consumer
   xử lý event trước khi dữ liệu thật sự tồn tại trong DB.
5. `@Async`/`@EnableAsync` **không được bật** ở đâu trong project (từng có
   `@Async` trên `EmailService.sendWelcomeEmail` vô hiệu vì thiếu `@EnableAsync`, đã
   fix bằng cách chuyển hẳn sang Kafka consumer thread) — đừng dựa vào `@Async` để
   chạy nền, dùng Kafka listener như pattern hiện có.

## Bootstrap servers theo môi trường

`localhost:9092` khi chạy app ngoài Docker, `kafka:19092` khi app chạy trong cùng
docker network (internal listener) — xem `docs/DOCKER.md` mục 12 để xem message trong
topic bằng `kafka-console-consumer`.
