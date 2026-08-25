# Redis Failure & Cache Test Lab — GHD

Hướng dẫn tái hiện 10 tình huống Redis phổ biến trên chính project GHD, dùng API thật (không
mock), log rõ `[CACHE]`/`[DB]`/`[REDIS]`, và số liệu thật (đã tự chạy kiểm chứng toàn bộ 10 case
trên máy dev khi xây guide này — không có số liệu bịa).

Toàn bộ code lab nằm sau Spring profile `redis-lab` (chỉ tồn tại khi bật profile này — **zero
impact lên production/mặc định**), gồm:

- `controllers/api/RedisLabController.java` — endpoint dưới `/admin/v1/test/redis/**`
- `services/RedisLabService.java` + `services/impl/RedisLabServiceImpl.java`
- `services/RedisLabMetrics.java` — counter hit/miss/error/fallback/db-query
- `dto/redislab/*.java`
- `src/main/resources/application-redis-lab.properties`
- `compose.redis-lab.yml` — overlay: Toxiproxy (Case 3) + `redis-memtest` (Case 4)
- `scripts/redis-lab/*.sh`
- `loadtest/k6/redis-lab-product-detail.js`
- `src/test/java/guru/springframework/ghd/redislab/*Test.java` (Testcontainers, Redis thật)

**Case 1 (hit/miss) và Case 5 (penetration) dùng lại đúng cache production thật** — gọi
`ProductServiceImpl.getByIdProduct` (đã `@Cacheable(cacheNames = "products")` sẵn trong code,
TTL 10 phút, xem `CacheConfig`), không tạo cache giả lập song song. Case 6/7/8 dùng namespace
Redis riêng của lab (`<namespace>:redislab:...`) để không đụng TTL/key thật của cache `products`.

## 0. Cài đặt

```bash
cp .env.example .env   # điền DB_USERNAME/PASSWORD, REDIS_PASSWORD, JWT_SECRET, AES_SECRET...

# Build + chạy full stack VỚI profile redis-lab (thêm Toxiproxy + redis-memtest)
docker compose -f compose.yaml -f compose.redis-lab.yml up -d --build

# Kiểm tra
curl http://localhost:${APP_PORT:-8080}/actuator/health
```

Đăng nhập lấy cookie JWT (app này xác thực qua **cookie tên `ghd`**, KHÔNG phải header
`Authorization: Bearer` — xem `security/JwtAuthenticationFilter.getJwtFromCookie`, dùng chung
cho cả `/api/v1/**` lẫn `/admin/v1/**`):

```bash
TOKEN=$(curl -s -X POST http://localhost:18080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"<admin_user>","password":"<admin_pass>"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

curl "http://localhost:18080/admin/v1/test/redis/metrics" -H "Cookie: ghd=$TOKEN"
```

Mọi endpoint lab đều dưới `/admin/v1/test/redis/**` + `@PreAuthorize("hasRole('ADMIN')")` — cố
tình **không** đặt dưới `/api/v1/**` vì `SecurityConfig` có rule
`GET /api/v1/** → permitAll()` áp dụng bất kể `PUBLIC_URLS` (đúng tiền lệ
`AnalyticsStatsController`).

**Quay lại bình thường** (tắt lab): `docker compose -f compose.yaml up -d` (không kèm overlay).

---

## Case 1 — Cache Hit / Cache Miss

### Cách tạo lỗi
Không phải "lỗi" — đây là hành vi cache-aside bình thường, verify nó hoạt động thật.

### API test
```bash
curl "http://localhost:18080/admin/v1/test/redis/product/<productId>" -H "Cookie: ghd=$TOKEN"
# gọi lại lần 2 với CÙNG productId
```

### Kết quả mong đợi (thật, đã test)
```
Lần 1: cacheStatus=MISS, dbQueriesTriggered=2, elapsedMs≈100-120
Lần 2: cacheStatus=HIT,  dbQueriesTriggered=0, elapsedMs≈30-55
```
`GET /admin/v1/test/redis/metrics` → `cacheHit`/`cacheMiss` tăng tương ứng.

### Log mong đợi
```
[CACHE] Checking Redis: products::idProduct:<id>
[CACHE] MISS: idProduct:<id>
[DB] Querying database
[CACHE] Saving data to Redis with TTL=600s
--- lần 2 ---
[CACHE] Checking Redis: products::idProduct:<id>
[CACHE] HIT: idProduct:<id>
[CACHE] Returning data from Redis
```

### Cách khôi phục
`POST /admin/v1/test/redis/cache/clear?key=idProduct:<id>` (hoặc bỏ `key` để clear cả cache
`products`).

---

## Case 2 — Redis Down

### Cách tạo lỗi
```bash
docker compose stop redis
```

### API test
```bash
curl "http://localhost:18080/admin/v1/test/redis/product/<productId>" -H "Cookie: ghd=$TOKEN"
```

### Kết quả mong đợi
```
HTTP 200 (KHÔNG phải 500), cacheStatus=REDIS_DOWN, data trả về từ DB bình thường
```
**Trước khi có fix `CacheConfig.cacheErrorHandler()`**: mọi method `@Cacheable` (không riêng gì
lab — `getByIdProduct`, `getBySlug`, list category/brand/news...) sẽ **500** khi Redis down, vì
`SimpleCacheErrorHandler` mặc định của Spring rethrow exception. Đã verify bằng test thật
(`RedisLabFallbackTest`) trước/sau khi thêm fix.

### Log mong đợi
```
[REDIS] Connection failed - ... . [REDIS] Fallback enabled
[DB] Querying database
```

### ⚠️ Phát hiện quan trọng khác (chưa fix, cần bạn quyết định)
`JwtAuthenticationFilter` → `TokenStoreService.isAccessTokenBlacklisted()` gọi Redis **trực
tiếp** (không qua Spring Cache, không được `CacheErrorHandler` bảo vệ) để check JWT blacklist
trên **mọi** request đã đăng nhập. Verify thật qua Toxiproxy (Case 3): khi Redis chậm/down, mọi
request `/admin/v1/**`/`/api/v1/**` cần JWT đều **500**, kể cả request không liên quan gì đến
cache. Đây là lỗ hổng resilience thật ngoài phạm vi cache — sửa nó là quyết định bảo mật
(fail-open: bỏ qua check khi Redis lỗi, chấp nhận rủi ro token đã bị revoke vẫn qua; hay
fail-closed: trả 401/503 rõ ràng thay vì 500) nên **chưa tự sửa**, cần bạn chọn hướng.

### Cách khôi phục
```bash
docker compose start redis
```

---

## Case 3 — Timeout / Connection Error (Toxiproxy)

### Cách tạo lỗi
`compose.redis-lab.yml` route Redis của app qua Toxiproxy (`REDIS_HOST=toxiproxy:8666`, không
toxic = pass-through gần như trong suốt). Toxiproxy control API expose ở `localhost:8474` (lưu
ý image `ghcr.io/shopify/toxiproxy` KHÔNG có `toxiproxy-cli`/shell bên trong — gọi thẳng HTTP
API từ host):

```bash
curl -X POST http://localhost:8474/proxies/redis_proxy/toxics \
  -H "Content-Type: application/json" \
  -d '{"type":"latency","name":"redis_latency","attributes":{"latency":5000}}'
```

### API test
```bash
time curl -o /tmp/resp.json -w "STATUS=%{http_code}" \
  "http://localhost:18080/admin/v1/test/redis/product/<productId>" -H "Cookie: ghd=$TOKEN"
```

### Kết quả mong đợi (thật, đã đo)
```
real ≈ 2.15s (đúng bằng spring.data.redis.timeout=2000ms mặc định, KHÔNG đợi đủ 5s toxic)
```
Với endpoint cache-aside (`/product/{id}`) đã fallback đúng như Case 2. **Nhưng** nếu request
đi qua auth trước (mọi request cần JWT) sẽ dính lỗ hổng `TokenStoreService` ở Case 2 → **500**
thay vì fallback — đây chính là cách tôi phát hiện ra bug đó khi test case này.

### Log mong đợi
```
io.lettuce.core.RedisCommandTimeoutException: Command timed out after 2 second(s)
[REDIS] Connection failed - ... . [REDIS] Fallback enabled     (nếu tới được RedisLabServiceImpl)
```

### Cách khôi phục
```bash
curl -X DELETE http://localhost:8474/proxies/redis_proxy/toxics/redis_latency
```
(hoặc dùng `scripts/redis-lab/toxiproxy-toxic.sh remove-latency`)

---

## Case 4 — Redis Memory Full

### Cách tạo lỗi
`redis-memtest` (trong `compose.redis-lab.yml`) là Redis **riêng biệt hoàn toàn**, `maxmemory
20mb`, không liên quan gì tới Redis app dùng thật — an toàn tuyệt đối cho cache/session thật.

```bash
./scripts/redis-lab/memory-fill.sh noeviction    # 6.1
./scripts/redis-lab/memory-fill.sh allkeys-lru   # 6.2
```

Script dùng `redis-benchmark` (bundled sẵn trong image `redis`) để ghi hàng loạt key trong 1
lần gọi thay vì loop `redis-cli` từng key — loop từng key qua `docker exec` tốn ~0.7s/lần trên
máy Windows, 3000 key sẽ mất hàng chục phút; `redis-benchmark` làm xong dưới 1 giây.

### Kết quả mong đợi (số liệu THẬT đã đo)

**6.1 `noeviction`** (8000 SET, mỗi value ~4KB):
```
used_memory_human: 18.21M / maxmemory 20.00M
evicted_keys: 0
DBSIZE: 3464          (~4536 lệnh SET còn lại bị từ chối)
Error from server: OOM command not allowed when used memory > 'maxmemory'
```

**6.2 `allkeys-lru`** (15000 SET, mỗi value ~4KB):
```
used_memory_human: 18.22M / maxmemory 20.00M   (giữ ổn định, không tăng thêm)
evicted_keys: 8543
DBSIZE: 3466
SET: 85714.29 requests per second   (TOÀN BỘ 15000 lệnh đều THÀNH CÔNG, không có OOM error)
```

Đối chiếu rõ: `noeviction` bảo toàn dữ liệu cũ nhưng **từ chối ghi mới** khi đầy; `allkeys-lru`
**luôn ghi được** nhưng âm thầm xoá key cũ nhất để lấy chỗ.

### Log mong đợi
`noeviction`: client nhận lỗi `OOM command not allowed when used memory > 'maxmemory'`.
`allkeys-lru`: không lỗi, `INFO stats` → `evicted_keys` tăng liên tục.

### Cách khôi phục
```bash
docker compose exec redis-memtest redis-cli FLUSHALL
docker compose exec redis-memtest redis-cli CONFIG SET maxmemory-policy noeviction
```

---

## Case 5 — Cache Penetration

### Cách tạo lỗi
Gọi liên tục 1 id **không tồn tại** trong DB.

### API test
```bash
curl "http://localhost:18080/admin/v1/test/redis/product/does-not-exist-123" -H "Cookie: ghd=$TOKEN"
# gọi lại lần 2, lần 3...
```

### Kết quả mong đợi (thật, đã test)
```
Lần 1: cacheStatus=NOT_FOUND, dbQueriesTriggered=1   (thử query DB, không thấy, cache NULL marker)
Lần 2+: cacheStatus=NOT_FOUND, dbQueriesTriggered=0  (chặn ở marker, KHÔNG chạm DB)
```
`GET /admin/v1/test/redis/metrics` → `databaseQueriesPrevented` tăng dần theo số request bị
chặn. Không dùng Bloom Filter (không cần thêm dependency mới) — null-caching TTL ngắn (30s,
`app.redis-lab.null-cache-ttl`) là đủ cho yêu cầu.

### Log mong đợi
```
[DB] Not found - caching NULL marker with TTL=30s
--- lần sau ---
[CACHE] NULL marker HIT for does-not-exist-123 - skipping DB (penetration protection)
```

### Cách khôi phục
Tự hết hạn sau 30s, hoặc restart app (cache clear-on-startup có sẵn).

---

## Case 6 — Cache Avalanche

### Cách tạo lỗi
Seed nhiều key cùng TTL (namespace riêng `redislab:avalanche:*`, không đụng cache `products`).

```bash
curl -X POST "http://localhost:18080/admin/v1/test/redis/avalanche/seed?count=30&jitter=false" -H "Cookie: ghd=$TOKEN"
curl "http://localhost:18080/admin/v1/test/redis/avalanche/ttl-distribution" -H "Cookie: ghd=$TOKEN"
```

### Kết quả mong đợi (thật, đã đo)
```
jitter=false (30 key, base TTL 60s): toàn bộ TTL còn lại = 59s, spread ≈ 0s → hết hạn ĐỒNG LOẠT
jitter=true  (30 key, TTL 60s + random 0..30s): TTL từ 59s đến 88s, spread = 29s → PHÂN TÁN
```

### Log mong đợi
```
[CACHE] Seeded 30 avalanche keys (jitter=false)
[CACHE] Seeded 30 avalanche keys (jitter=true)
```

### Cách khôi phục
Không cần — key tự hết hạn theo TTL, không ảnh hưởng cache `products` thật.

---

## Case 7 — Cache Stampede

### Cách tạo lỗi
Xoá cache 1 key hot rồi bắn N request đồng thời (lab tự dựng N thread nội bộ qua
`ExecutorService`, không cần công cụ ngoài).

```bash
curl -X POST "http://localhost:18080/admin/v1/test/redis/stampede?key=hot-product-1&concurrency=60&withLock=false" -H "Cookie: ghd=$TOKEN"
curl -X POST "http://localhost:18080/admin/v1/test/redis/stampede?key=hot-product-2&concurrency=60&withLock=true"  -H "Cookie: ghd=$TOKEN"
```

### Kết quả mong đợi (thật, đã đo)
```
withLock=false: totalRequests=60, dbQueriesTriggered=60   (MỌI request đều query DB)
withLock=true:  totalRequests=60, dbQueriesTriggered=1    (CHỈ 1 request rebuild cache)
```

Cơ chế: `SETNX` (Redis `SET key val NX PX <ttl>`, qua `StringRedisTemplate.opsForValue()
.setIfAbsent`, không cần thêm dependency Redisson) làm distributed lock. Lock có TTL tự hết hạn
(`app.redis-lab.stampede-lock-ttl=5s`, chống deadlock nếu instance giữ lock chết), release bằng
Lua script compare-and-delete (chỉ instance đang giữ token mới xoá được, tránh xoá nhầm lock của
owner khác), luôn release trong `finally`.

### Log mong đợi
```
[LOCK] Acquired ghd:local:redislab:stampede:hot-product-2:lock - rebuilding cache
[DB] Querying database (stampede, lock owner)
[LOCK] Released ghd:local:redislab:stampede:hot-product-2:lock (owner-checked)
[STAMPEDE] 60 concurrent requests, withLock=true, dbQueriesTriggered=1
```

### Cách khôi phục
Không cần — key/lock tự hết hạn.

---

## Case 8 — Stale Data

### Cách tạo lỗi
Warm cache → update DB **bypass** cơ chế evict (mô phỏng đúng bug thật, không phải giả lập).

```bash
curl -X POST "http://localhost:18080/admin/v1/test/redis/stale-data?idProduct=<id>&newPrice=1234567" -H "Cookie: ghd=$TOKEN"
```

### Kết quả mong đợi (thật, đã test — giá gốc 42.990.000, đổi thành 1.234.567)
```
1-warm-cache:              cachedPrice=42990000.00  stale=false
2-mutate-db-bypass-evict:  databasePrice=1234567    stale=true
3-read-after-mutation:     cachedPrice=42990000.00  stale=true   ← BUG: đọc ra giá CŨ
4-after-evict-fix:         cachedPrice=1234567.00   stale=false  ← ĐÚNG sau khi evict
```
Bước 4 dùng **đúng cơ chế** `CacheConfig`/`ProductServiceImpl.updateProduct` đã có sẵn
(`@CacheEvict(cacheNames = PRODUCTS, allEntries = true)`) — không phải code mới. `immediateWrites`
(đã cấu hình sẵn trong `CacheConfig`) giảm race giữa "đọc cache" và "update+evict" (evict đồng
bộ, không async) nhưng không loại bỏ tuyệt đối — không thêm lock/versioning mới cho race này
(ngoài phạm vi yêu cầu ban đầu).

### Log mong đợi
```
[DB] Updated product <id> price to 1234567 directly (cache NOT evicted)
[CACHE] Read after DB mutation - cached price=42990000.00, real DB price=1234567, stale=true
[CACHE] Read after evict - cached price=1234567.00, fixed=true
```

### Cách khôi phục
Case tự chứa cách fix (bước 4) — không cần thao tác thêm. Muốn fix thật trong production: luôn
update qua `PUT /api/v1/product/{id}` (đã evict sẵn), không update thẳng DB.

---

## Case 9 — Redis Restart / Persistence

### Cách tạo lỗi
Không phải lỗi — Redis app dùng đã bật AOF sẵn (`compose.yaml`:
`redis-server --appendonly yes`), verify nó thật sự giữ data qua restart.

```bash
docker compose exec redis sh -c 'redis-cli -a "$REDIS_PASSWORD" --no-auth-warning SET redislab:persist-test hello-aof'
docker compose restart redis
docker compose exec redis sh -c 'redis-cli -a "$REDIS_PASSWORD" --no-auth-warning GET redislab:persist-test'
```

### Kết quả mong đợi (thật, đã test)
```
GET trước restart: hello-aof
GET sau restart:   hello-aof     ← còn nguyên nhờ AOF, không cần thêm cấu hình gì
```

### Log mong đợi
Không có gì đặc biệt — đây chính là điểm cần chứng minh (persistence "vô hình", không mất data).

### Cách khôi phục
Không cần.

---

## Case 10 — Load Test (k6, với/không cache)

### Cách chạy

```bash
# Cần grafana/k6 image (docker pull grafana/k6) - dùng chung network với stack:
docker run --rm --network <project>_ghd-net \
  -v "$(pwd)/loadtest/k6:/scripts" \
  -e BASE_URL=http://app:8080 -e PRODUCT_ID=<id> \
  -e ADMIN_USERNAME=<u> -e ADMIN_PASSWORD=<p> -e VUS=100 -e DURATION=30s \
  grafana/k6 run /scripts/redis-lab-product-detail.js

# Without cache: thêm -e DISABLE_CACHE=true
```

**Lưu ý quan trọng đã verify thật**: kỹ thuật `SPRING_CACHE_TYPE=none` (docs/DOCKER.md mục 9)
**KHÔNG tắt được cache** trong app này — `CacheConfig.cacheManager()` là `@Bean` không điều
kiện (không có `@ConditionalOnProperty`), nên Spring Boot's cache autoconfiguration theo
`spring.cache.type` không bao giờ chạy; cache Redis luôn được tạo bất kể giá trị property này
(đã test trực tiếp: set `SPRING_CACHE_TYPE=none`, gọi 2 lần liên tiếp vẫn thấy `cacheStatus=HIT`
lần 2). Vì vậy k6 script dùng `DISABLE_CACHE=true` → tự `POST cache/clear` ngay trước mỗi `GET`
để đảm bảo **luôn miss thật**, thay vì dựa vào property không có tác dụng.

Cũng **không thể** dùng cách tắt hẳn Redis để mô phỏng "không cache", vì như Case 2/3 đã phát
hiện, Redis down làm auth (`TokenStoreService`) cũng chết theo — không so sánh công bằng được.

### Kết quả THẬT (100 VUs, 30s, cùng máy, cùng endpoint `/admin/v1/test/redis/product/{id}`)

|                     | Without cache (evict trước mỗi GET) | With cache |
|---------------------|--------------------------------------|------------|
| Requests/sec (GET)  | ~336 req/s (10090 iter / 30s)        | ~639 req/s (19166 iter / 30s) |
| Avg Response        | 98.59 ms *(gộp cả call evict + GET)* | 55.82 ms |
| p90                 | 174.39 ms                            | 102.11 ms |
| p95                 | 209.31 ms                            | 128.57 ms |
| Max                 | 502.41 ms                            | 309.60 ms |
| Error Rate          | 0%                                    | 0% |
| DB Queries (run này)| ~6625 / 10090 request (~66%)         | ~6 / 19166 request |
| Cache Hit Rate      | ~0% hiệu quả (evict liên tục)        | 100% |

Ghi chú trung thực: cột "Without cache" gồm **2 HTTP request/iteration** (1 evict + 1 GET) nên
`Avg Response`/`p95` là số gộp của cả 2 loại call, không phải riêng GET — số DB Queries mới
phản ánh đúng nhất mức chênh lệch tải xuống MySQL (66% request chạm DB so với ~0.03% khi có
cache). Máy chạy test là dev machine chia sẻ tài nguyên với nhiều project Docker khác (xem phần
"Giới hạn môi trường" bên dưới) — số tuyệt đối (req/s) sẽ khác trên máy khác, nhưng **tỷ lệ
chênh lệch** (RPS gần gấp đôi, DB load giảm ~2000 lần khi có cache) là tín hiệu đáng tin cậy.

### Giới hạn môi trường khi chạy guide này
Máy dev dùng để verify guide này có **nhiều project Docker khác đang chạy song song**
(tổng RAM cấp cho Docker Desktop ~7.7GB, đã dùng ~4GB trước khi bắt đầu) — Docker Desktop từng
crash giữa lúc test do quá tải RAM, phải khởi động lại. Nếu bạn tái hiện trên máy ít tài nguyên
hơn, cân nhắc giảm `VUS`/đóng bớt container khác trước khi chạy Case 10.

---

## Test tự động

```powershell
.\mvnw.cmd test -Dtest=RedisLabCacheHitMissTest,RedisLabFallbackTest,RedisLabAvalancheTest,RedisLabStampedeTest,RedisLabStaleDataTest
```

Dùng Testcontainers Redis thật (`AbstractIntegrationTest`, giống `CategoryServiceCacheTest`),
không mock Redis. **Lưu ý đã gặp thật**: chạy CẢ 5 class cùng 1 lần `mvn test` trên máy dev chia
sẻ tài nguyên (nhiều project Docker khác chạy song song) có thể khiến Redis container bị
Docker/host giới hạn RAM làm gián đoạn tạm thời giữa các class → lỗi `Unable to connect to
Redis` không liên quan gì tới logic code (đã verify: chạy TỪNG class riêng lẻ, cả 9 test đều
PASS). Nếu gặp lỗi tương tự, thử chạy `-Dtest=<TênClass>` riêng từng class.

Case 3/4/9/10 là hạ tầng/docker-level, không cover bằng test tự động (lý do đã nêu ở từng case),
verify thủ công theo hướng dẫn trên — đã tự chạy thật toàn bộ khi viết guide này.
