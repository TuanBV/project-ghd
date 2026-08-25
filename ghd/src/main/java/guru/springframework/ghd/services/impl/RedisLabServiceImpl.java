package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.config.CacheConfig;
import guru.springframework.ghd.dto.product.ProductDetailClientResponse;
import guru.springframework.ghd.dto.redislab.AvalancheSeedResponse;
import guru.springframework.ghd.dto.redislab.AvalancheTtlEntry;
import guru.springframework.ghd.dto.redislab.CacheProbeResponse;
import guru.springframework.ghd.dto.redislab.StaleDataStepResponse;
import guru.springframework.ghd.dto.redislab.StampedeResultResponse;
import guru.springframework.ghd.entities.Product;
import guru.springframework.ghd.repositories.ProductRepository;
import guru.springframework.ghd.services.ProductService;
import guru.springframework.ghd.services.RedisLabMetrics;
import guru.springframework.ghd.services.RedisLabService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("redis-lab")
public class RedisLabServiceImpl implements RedisLabService {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final CacheManager cacheManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisLabMetrics metrics;

    @Value("${app.redis-lab.namespace}")
    private String labNamespace;

    @Value("${app.redis-lab.null-cache-ttl}")
    private Duration nullCacheTtl;

    @Value("${app.redis-lab.stampede-lock-ttl}")
    private Duration stampedeLockTtl;

    @Value("${app.redis-lab.stampede-wait-timeout}")
    private Duration stampedeWaitTimeout;

    @Value("${app.redis-lab.stampede-poll-interval}")
    private Duration stampedePollInterval;

    @Value("${app.redis-lab.avalanche-base-ttl}")
    private Duration avalancheBaseTtl;

    @Value("${app.redis-lab.avalanche-jitter-max}")
    private Duration avalancheJitterMax;

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    // ---------------------------------------------------------------- Case 1 / 2 / 5

    @Override
    public CacheProbeResponse probeProduct(String idProduct) {
        long start = System.currentTimeMillis();
        String cacheKey = "idProduct:" + idProduct;
        String nullMarkerKey = labNamespace + ":null:" + idProduct;

        if (isNullMarked(nullMarkerKey)) {
            log.info("[CACHE] NULL marker HIT for {} - skipping DB (penetration protection)", idProduct);
            metrics.recordDatabaseQueryPrevented();
            return CacheProbeResponse.builder()
                    .cacheName(CacheConfig.PRODUCTS)
                    .cacheKey(cacheKey)
                    .cacheStatus("NOT_FOUND")
                    .elapsedMs(System.currentTimeMillis() - start)
                    .dbQueriesTriggered(0)
                    .build();
        }

        Cache productsCache = cacheManager.getCache(CacheConfig.PRODUCTS);
        log.info("[CACHE] Checking Redis: {}::{}", CacheConfig.PRODUCTS, cacheKey);

        Cache.ValueWrapper wrapper;
        boolean redisReachable = true;
        try {
            wrapper = productsCache.get(cacheKey);
        } catch (RuntimeException e) {
            redisReachable = false;
            wrapper = null;
            metrics.recordRedisError();
            log.warn("[REDIS] Connection failed - {}. [REDIS] Fallback enabled", e.getMessage());
        }

        if (redisReachable && wrapper != null) {
            log.info("[CACHE] HIT: {}", cacheKey);
            log.info("[CACHE] Returning data from Redis");
            metrics.recordCacheHit();
            ProductDetailClientResponse data = productService.getByIdProduct(idProduct);
            return CacheProbeResponse.builder()
                    .cacheName(CacheConfig.PRODUCTS)
                    .cacheKey(cacheKey)
                    .cacheStatus("HIT")
                    .elapsedMs(System.currentTimeMillis() - start)
                    .dbQueriesTriggered(0)
                    .data(data)
                    .build();
        }

        if (redisReachable) {
            log.info("[CACHE] MISS: {}", cacheKey);
        }
        log.info("[DB] Querying database");
        try {
            ProductDetailClientResponse data = productService.getByIdProduct(idProduct);
            // getByIdProduct/getBySlug issue 2 SQL statements per miss (findByIdProduct/findBySlug
            // native query + one findByProductId for images) - see ProductServiceImpl.
            metrics.recordDatabaseQuery();
            if (!redisReachable) {
                metrics.recordFallbackToDatabase();
            } else {
                metrics.recordCacheMiss();
                log.info("[CACHE] Saving data to Redis with TTL={}s",
                        CacheConfig.cacheTtls().get(CacheConfig.PRODUCTS).toSeconds());
            }
            return CacheProbeResponse.builder()
                    .cacheName(CacheConfig.PRODUCTS)
                    .cacheKey(cacheKey)
                    .cacheStatus(redisReachable ? "MISS" : "REDIS_DOWN")
                    .elapsedMs(System.currentTimeMillis() - start)
                    .dbQueriesTriggered(2)
                    .data(data)
                    .build();
        } catch (RuntimeException e) {
            // ProductServiceImpl.getByIdProduct has no not-found guard of its own (it NPEs on an
            // unknown id) - the lab treats that as "not found in DB either" and caches a short-TTL
            // NULL marker so the next N requests for the same missing id never reach the DB again.
            log.info("[DB] Not found - caching NULL marker with TTL={}s", nullCacheTtl.toSeconds());
            cacheNullMarker(nullMarkerKey);
            return CacheProbeResponse.builder()
                    .cacheName(CacheConfig.PRODUCTS)
                    .cacheKey(cacheKey)
                    .cacheStatus("NOT_FOUND")
                    .elapsedMs(System.currentTimeMillis() - start)
                    .dbQueriesTriggered(1)
                    .build();
        }
    }

    @Override
    public void clearProductsCache(String key) {
        Cache cache = cacheManager.getCache(CacheConfig.PRODUCTS);
        if (key == null || key.isBlank()) {
            cache.clear();
            log.info("[CACHE] Cleared entire cache: {}", CacheConfig.PRODUCTS);
        } else {
            cache.evict(key);
            log.info("[CACHE] Evicted: {}::{}", CacheConfig.PRODUCTS, key);
        }
    }

    private boolean isNullMarked(String nullMarkerKey) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(nullMarkerKey));
        } catch (RuntimeException e) {
            metrics.recordRedisError();
            log.warn("[REDIS] Connection failed checking NULL marker - {}", e.getMessage());
            return false;
        }
    }

    private void cacheNullMarker(String nullMarkerKey) {
        try {
            stringRedisTemplate.opsForValue().set(nullMarkerKey, "1", nullCacheTtl);
        } catch (RuntimeException e) {
            metrics.recordRedisError();
            log.warn("[REDIS] Connection failed caching NULL marker - {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------- Case 6 (avalanche)

    @Override
    public AvalancheSeedResponse seedAvalanche(int count, boolean jitter) {
        for (int i = 0; i < count; i++) {
            String key = labNamespace + ":avalanche:" + i;
            Duration ttl = jitter
                    ? avalancheBaseTtl.plusMillis((long) (Math.random() * avalancheJitterMax.toMillis()))
                    : avalancheBaseTtl;
            stringRedisTemplate.opsForValue().set(key, "seeded-at-" + System.currentTimeMillis(), ttl);
        }
        log.info("[CACHE] Seeded {} avalanche keys (jitter={})", count, jitter);
        return AvalancheSeedResponse.builder()
                .keysSeeded(count)
                .jitterEnabled(jitter)
                .baseTtlSeconds(avalancheBaseTtl.toSeconds())
                .jitterMaxSeconds(jitter ? avalancheJitterMax.toSeconds() : 0)
                .build();
    }

    @Override
    public List<AvalancheTtlEntry> avalancheTtlDistribution() {
        Set<String> keys = stringRedisTemplate.keys(labNamespace + ":avalanche:*");
        List<AvalancheTtlEntry> entries = new ArrayList<>();
        if (keys == null) {
            return entries;
        }
        for (String key : keys) {
            Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            entries.add(AvalancheTtlEntry.builder()
                    .key(key)
                    .remainingTtlSeconds(ttl == null ? -1 : ttl)
                    .build());
        }
        return entries;
    }

    // ---------------------------------------------------------------- Case 7 (stampede)

    @Override
    public StampedeResultResponse simulateStampede(String key, int concurrency, boolean withLock) {
        String dataKey = labNamespace + ":stampede:" + key;
        stringRedisTemplate.delete(dataKey);

        AtomicInteger dbQueries = new AtomicInteger();
        int poolSize = Math.min(concurrency, 64);
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch starter = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(concurrency);

        for (int i = 0; i < concurrency; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    starter.await();
                    if (withLock) {
                        loadWithLock(dataKey, dbQueries);
                    } else {
                        loadWithoutLock(dataKey, dbQueries);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        long start = System.currentTimeMillis();
        try {
            ready.await(5, TimeUnit.SECONDS);
            starter.countDown();
            done.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdown();
        }
        long elapsed = System.currentTimeMillis() - start;

        log.info("[STAMPEDE] {} concurrent requests, withLock={}, dbQueriesTriggered={}",
                concurrency, withLock, dbQueries.get());

        return StampedeResultResponse.builder()
                .totalRequests(concurrency)
                .lockEnabled(withLock)
                .dbQueriesTriggered(dbQueries.get())
                .elapsedMs(elapsed)
                .build();
    }

    private String loadWithoutLock(String dataKey, AtomicInteger dbQueries) {
        String cached = stringRedisTemplate.opsForValue().get(dataKey);
        if (cached != null) {
            return cached;
        }
        log.info("[DB] Querying database (stampede, no lock)");
        String value = expensiveLoad();
        dbQueries.incrementAndGet();
        stringRedisTemplate.opsForValue().set(dataKey, value, Duration.ofSeconds(60));
        return value;
    }

    private String loadWithLock(String dataKey, AtomicInteger dbQueries) throws InterruptedException {
        String cached = stringRedisTemplate.opsForValue().get(dataKey);
        if (cached != null) {
            return cached;
        }

        String lockKey = dataKey + ":lock";
        String token = UUID.randomUUID().toString();
        boolean acquired = Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(lockKey, token, stampedeLockTtl));

        if (acquired) {
            try {
                cached = stringRedisTemplate.opsForValue().get(dataKey);
                if (cached != null) {
                    return cached;
                }
                log.info("[LOCK] Acquired {} - rebuilding cache", lockKey);
                log.info("[DB] Querying database (stampede, lock owner)");
                String value = expensiveLoad();
                dbQueries.incrementAndGet();
                stringRedisTemplate.opsForValue().set(dataKey, value, Duration.ofSeconds(60));
                return value;
            } finally {
                releaseLockIfOwner(lockKey, token);
            }
        }

        long deadline = System.currentTimeMillis() + stampedeWaitTimeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            cached = stringRedisTemplate.opsForValue().get(dataKey);
            if (cached != null) {
                return cached;
            }
            Thread.sleep(stampedePollInterval.toMillis());
        }
        log.warn("[LOCK] Timed out waiting for lock owner to rebuild {}", dataKey);
        return null;
    }

    private void releaseLockIfOwner(String lockKey, String token) {
        stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), token);
        log.info("[LOCK] Released {} (owner-checked)", lockKey);
    }

    /** Stands in for an expensive DB rebuild - a real SQL round trip plus artificial latency,
     * wide enough for concurrent callers to genuinely race each other in Case 7. */
    private String expensiveLoad() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long count = productRepository.count();
        return "rebuilt-" + count + "-at-" + System.currentTimeMillis();
    }

    // ---------------------------------------------------------------- Case 8 (stale data)

    @Override
    public List<StaleDataStepResponse> simulateStaleData(String idProduct, BigDecimal newPrice) {
        List<StaleDataStepResponse> steps = new ArrayList<>();

        ProductDetailClientResponse warmed = productService.getByIdProduct(idProduct);
        steps.add(StaleDataStepResponse.builder()
                .step("1-warm-cache")
                .cachedPrice(warmed.getPrice())
                .databasePrice(warmed.getPrice())
                .stale(false)
                .build());

        Product product = productRepository.findById(warmed.getId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm ID: " + warmed.getId()));
        product.setPrice(newPrice);
        // Bypasses ProductServiceImpl.updateProduct on purpose - that method already
        // @CacheEvict(allEntries = true)s the PRODUCTS cache; saving directly through the
        // repository here reproduces the real bug this case demonstrates (DB changed, cache didn't).
        productRepository.save(product);
        log.info("[DB] Updated product {} price to {} directly (cache NOT evicted)", idProduct, newPrice);
        steps.add(StaleDataStepResponse.builder()
                .step("2-mutate-db-bypass-evict")
                .cachedPrice(warmed.getPrice())
                .databasePrice(newPrice)
                .stale(true)
                .build());

        ProductDetailClientResponse staleRead = productService.getByIdProduct(idProduct);
        boolean stillStale = staleRead.getPrice().compareTo(newPrice) != 0;
        log.info("[CACHE] Read after DB mutation - cached price={}, real DB price={}, stale={}",
                staleRead.getPrice(), newPrice, stillStale);
        steps.add(StaleDataStepResponse.builder()
                .step("3-read-after-mutation")
                .cachedPrice(staleRead.getPrice())
                .databasePrice(newPrice)
                .stale(stillStale)
                .build());

        // The fix: evict the same way ProductServiceImpl.updateProduct's
        // @CacheEvict(cacheNames = PRODUCTS, allEntries = true) already does for every real
        // update - not a new mechanism, just applying the existing one for this bypassed write.
        clearProductsCache(null);
        ProductDetailClientResponse fixedRead = productService.getByIdProduct(idProduct);
        boolean fixed = fixedRead.getPrice().compareTo(newPrice) == 0;
        log.info("[CACHE] Read after evict - cached price={}, fixed={}", fixedRead.getPrice(), fixed);
        steps.add(StaleDataStepResponse.builder()
                .step("4-after-evict-fix")
                .cachedPrice(fixedRead.getPrice())
                .databasePrice(newPrice)
                .stale(!fixed)
                .build());

        return steps;
    }
}
