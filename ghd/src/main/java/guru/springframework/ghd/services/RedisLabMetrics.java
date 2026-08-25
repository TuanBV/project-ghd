package guru.springframework.ghd.services;

import guru.springframework.ghd.dto.redislab.RedisLabMetricsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * In-process counters backing {@code GET /admin/v1/test/redis/metrics}. Deliberately plain
 * {@link AtomicLong}s rather than Micrometer - this project has no existing MeterRegistry
 * usage (Actuator here is health/info only), so introducing one just for the lab would add a
 * new pattern for no benefit; these counters are reset on every app restart, which is fine for
 * a manual test lab (not a durable production metric). Lives directly under {@code services}
 * (no interface) - same placement as {@link TokenStoreService}, a concrete infra-style service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("redis-lab")
public class RedisLabMetrics {

    private final RedisConnectionFactory redisConnectionFactory;

    private final AtomicLong cacheHit = new AtomicLong();
    private final AtomicLong cacheMiss = new AtomicLong();
    private final AtomicLong redisErrors = new AtomicLong();
    private final AtomicLong fallbackToDatabase = new AtomicLong();
    private final AtomicLong databaseQueries = new AtomicLong();
    private final AtomicLong databaseQueriesPrevented = new AtomicLong();

    public void recordCacheHit() {
        cacheHit.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMiss.incrementAndGet();
    }

    public void recordRedisError() {
        redisErrors.incrementAndGet();
    }

    public void recordFallbackToDatabase() {
        fallbackToDatabase.incrementAndGet();
    }

    public void recordDatabaseQuery() {
        databaseQueries.incrementAndGet();
    }

    public void recordDatabaseQueryPrevented() {
        databaseQueriesPrevented.incrementAndGet();
    }

    public RedisLabMetricsResponse snapshot() {
        long hit = cacheHit.get();
        long miss = cacheMiss.get();
        long total = hit + miss;
        String hitRate = total == 0 ? "0%" : String.format("%.0f%%", (hit * 100.0) / total);

        return RedisLabMetricsResponse.builder()
                .redisStatus(pingRedis() ? "UP" : "DOWN")
                .cacheHit(hit)
                .cacheMiss(miss)
                .cacheHitRate(hitRate)
                .redisErrors(redisErrors.get())
                .fallbackToDatabase(fallbackToDatabase.get())
                .databaseQueries(databaseQueries.get())
                .databaseQueriesPrevented(databaseQueriesPrevented.get())
                .build();
    }

    private boolean pingRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            log.warn("[REDIS] Status ping failed - {}", e.getMessage());
            return false;
        }
    }
}
