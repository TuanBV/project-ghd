---
name: redis-cache-session
description: Cache Redis (tên cache/TTL thật, immediate-write, clear-on-startup) và Spring Session Redis của GHD — tách biệt với JWT. Đọc trước khi thêm @Cacheable/@CacheEvict hoặc đụng session.
---

# Redis — Cache + Session — GHD

Nguồn: `config/CacheConfig.java`, `config/SessionConfig.java`,
`application.properties` (mục Redis/Cache/Session).

## Cache (Spring Cache, `spring.cache.type=redis`)

Cache name **cố định**, mỗi cache có TTL riêng (không dùng 1 TTL chung):

| Cache name (hằng số trong `CacheConfig`) | TTL |
|---|---|
| `CATEGORIES` | 45 phút |
| `BRANDS` | 45 phút |
| `SYS_PARAMS` | 45 phút |
| `POLICIES` | 45 phút |
| `BANNERS` | 20 phút |
| `SLIDERS` | 20 phút |
| `NEWS` | 20 phút |
| `PRODUCTS` | 10 phút |
| `PRODUCTS_LATEST` | 10 phút |
| `PRODUCTS_RELATED` | 10 phút |

- Prefix key thật: `${app.cache.namespace}::<cacheName>::...` với
  `app.cache.namespace=${spring.application.name}:${app.environment}` (vd
  `ghd:local::categories::...`). Không tự đổi format prefix — service khác/dashboard
  Redis có thể đang match theo pattern này.
- `RedisCacheWriter` dùng **`immediateWrites`** (không phải async/deferred mặc định
  của Spring Data Redis 4 khi dùng Lettuce) — cố ý, vì `@CacheEvict` phải làm read tiếp
  theo thấy dữ liệu mới ngay (vd sau khi update product). Đừng đổi lại thành batching/
  async writer để "tối ưu throughput" mà không hiểu hệ quả tới consistency.
- **Cache tự clear toàn bộ khi app khởi động** (`clearCachesOnStartup` bean) — cố ý, vì
  cache Redis là volume bền, sống sót qua `docker compose down`/rebuild DB, từng gây
  bug thật (client thấy list rỗng/cũ sau khi restore DB vì cache cũ vẫn còn). Đừng xoá
  bean này để "cache ấm sẵn lúc start" — sẽ tái phát đúng bug đã fix.
- Thêm cache mới: khai hằng số tên cache trong `CacheConfig`, thêm vào
  `buildCacheTtls()` với TTL phù hợp độ "ít đổi" của dữ liệu, rồi dùng
  `@Cacheable(cacheNames = CacheConfig.X)` trong service. Khi ghi/update/xóa dữ liệu
  liên quan, `@CacheEvict(cacheNames = CacheConfig.X, allEntries = true)` — pattern
  hiện tại ưu tiên đúng-toàn-bộ hơn evict chính xác từng entry.

## Session (Spring Session Redis) — chỉ dùng cho phần client/view

- Namespace: `${spring.application.name}:${app.environment}:session`, cookie tên
  `GHDSESSION` (`SESSION_COOKIE_NAME`), `flush-mode=on-save`,
  `save-mode=on-set-attribute`.
- **Chỉ áp dụng cho `clientSecurityFilterChain`** (`SessionCreationPolicy.IF_REQUIRED`).
  Chain `/api/v1/**`/`/admin/v1/**` là `STATELESS` — JWT, không session. Xem
  [.claude/skills/spring-security-ghd/SKILL.md](../spring-security-ghd/SKILL.md).
- Serializer: Jackson 3 (`GenericJacksonJsonRedisSerializer`,
  `enableUnsafeDefaultTyping()`) — đây là lý do bẫy double-registration
  `JwtAuthenticationFilter` từng gây lỗi 500 (Jackson 3 không serialize được
  `UsernamePasswordAuthenticationToken`). Nếu định lưu object mới vào session, kiểm
  tra nó serialize/deserialize được qua Jackson 3 trước khi coi là xong.

## Test liên quan

`CacheConfigTest`, `SpringSessionRedisTest`, `CategoryServiceCacheTest` — chạy
`.\mvnw.cmd test -Dtest=CacheConfigTest,SpringSessionRedisTest,CategoryServiceCacheTest`
sau khi đổi cache/session config.
