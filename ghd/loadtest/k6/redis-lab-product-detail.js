// Case 10 - Load Test comparison (with/without Redis cache).
//
// Targets GET /admin/v1/test/redis/product/{idProduct} (RedisLabController) - the SAME
// @Cacheable service method (ProductServiceImpl.getByIdProduct) the real product page uses,
// wrapped just enough to require no browser/session and to update RedisLabMetrics so
// "database queries" is a real number, not a guess.
//
// Two real runs are needed for the comparison table in REDIS_TEST_GUIDE.md:
//
//   # 1) With cache (normal):
//   k6 run -e BASE_URL=http://localhost:18080 -e PRODUCT_ID=<id> \
//          -e ADMIN_USERNAME=<u> -e ADMIN_PASSWORD=<p> -e VUS=100 -e DURATION=60s \
//          loadtest/k6/redis-lab-product-detail.js
//
//   # 2) Without cache: same command + `-e DISABLE_CACHE=true`.
//
// IMPORTANT: docs/DOCKER.md section 9's `SPRING_CACHE_TYPE=none` trick does NOT work for this
// endpoint - CacheConfig.cacheManager() is an unconditional @Bean (no
// @ConditionalOnProperty), so Spring Boot's spring.cache.type-driven autoconfiguration never
// even runs; the custom RedisCacheManager is created either way (verified live: cache stayed
// HIT on the 2nd request with SPRING_CACHE_TYPE=none set). Instead, DISABLE_CACHE=true evicts
// the key via POST /admin/v1/test/redis/cache/clear right before every GET, forcing a genuine
// cache miss (and therefore a real DB round trip) on every single request - functionally
// equivalent to "no cache" for this benchmark, without needing to break Redis/auth entirely
// (Redis being fully down would also break JwtAuthenticationFilter's blacklist check on every
// request - see the Case 3 finding in REDIS_TEST_GUIDE.md - so it can't be used to model this).
//
// GET /admin/v1/test/redis/metrics right after each run to read cacheHit/cacheMiss/databaseQueries.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18080';
const PRODUCT_ID = __ENV.PRODUCT_ID;
const ADMIN_USERNAME = __ENV.ADMIN_USERNAME;
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;
const VUS = Number(__ENV.VUS || 100);
const DURATION = __ENV.DURATION || '60s';
const DISABLE_CACHE = (__ENV.DISABLE_CACHE || 'false').toLowerCase() === 'true';

if (!PRODUCT_ID) {
  throw new Error('Set -e PRODUCT_ID=<an existing product id> (see IProductDetailClient.id / Product.id in DB)');
}
if (!ADMIN_USERNAME || !ADMIN_PASSWORD) {
  throw new Error('Set -e ADMIN_USERNAME=... -e ADMIN_PASSWORD=... for an ADMIN-role user');
}

export const options = {
  scenarios: {
    load: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
  },
};

export function setup() {
  // This app's JwtAuthenticationFilter (security/JwtAuthenticationFilter.java) reads the JWT
  // ONLY from a cookie named "ghd" (RequestHeaderNames.COOKIE_TOKEN_NAME) - the `body.token`
  // field in the login response is not accepted via `Authorization: Bearer` on any route, so
  // every request below must resend it as a `Cookie: ghd=<token>` header.
  const res = http.post(
      `${BASE_URL}/api/v1/auth/login`,
      JSON.stringify({ username: ADMIN_USERNAME, password: ADMIN_PASSWORD }),
      { headers: { 'Content-Type': 'application/json' } },
  );
  if (res.status !== 200) {
    throw new Error(`Login failed (status ${res.status}): ${res.body}`);
  }
  const token = res.json('body.token');
  if (!token) {
    throw new Error(`Login response had no body.token: ${res.body}`);
  }
  return { token };
}

export default function (data) {
  const headers = { Cookie: `ghd=${data.token}` };

  if (DISABLE_CACHE) {
    http.post(
        `${BASE_URL}/admin/v1/test/redis/cache/clear?key=idProduct:${PRODUCT_ID}`,
        null,
        { headers },
    );
  }

  const res = http.get(`${BASE_URL}/admin/v1/test/redis/product/${PRODUCT_ID}`, { headers });
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(0.1);
}

export function handleSummary(data) {
  const m = data.metrics;
  const line = (name, metric, fields) =>
    `${name}: ` + fields.map((f) => `${f}=${metric.values[f]?.toFixed ? metric.values[f].toFixed(2) : metric.values[f]}`).join(' ');

  const summaryText = [
    `iterations: ${m.iterations.values.count}`,
    line('http_req_duration', m.http_req_duration, ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max']),
    line('http_req_failed', m.http_req_failed, ['rate', 'passes', 'fails']),
    `http_reqs: count=${m.http_reqs.values.count} rate=${m.http_reqs.values.rate.toFixed(2)}`,
  ].join('\n');

  return {
    stdout: summaryText + '\n',
    // Written inside the k6 container's filesystem (not the mounted host volume) - just here
    // so `handleSummary` doesn't also have to special-case "no file output", the real numbers
    // are read from stdout above.
    'summary.json': JSON.stringify(data, null, 2),
  };
}
