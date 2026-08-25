package guru.springframework.ghd.services;

import guru.springframework.ghd.dto.redislab.AvalancheSeedResponse;
import guru.springframework.ghd.dto.redislab.AvalancheTtlEntry;
import guru.springframework.ghd.dto.redislab.CacheProbeResponse;
import guru.springframework.ghd.dto.redislab.StaleDataStepResponse;
import guru.springframework.ghd.dto.redislab.StampedeResultResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Redis Failure &amp; Cache Test Lab - only active under the {@code redis-lab} profile (see
 * {@code application-redis-lab.properties} and {@code REDIS_TEST_GUIDE.md}). Reuses the real,
 * already-cached {@link ProductService#getByIdProduct(String)} read path for Case 1/2/5 instead
 * of inventing a parallel cache; Cases 6/7/8 operate on a dedicated {@code redislab} Redis key
 * namespace so they never touch the production {@code products} cache's TTL/keys.
 */
public interface RedisLabService {

    /** Case 1 (hit/miss), Case 2 (Redis down fallback), Case 5 (penetration / null-caching). */
    CacheProbeResponse probeProduct(String idProduct);

    /** Case 1 - clear the real "products" cache (whole cache, or a single "idProduct:x"/"slug:x" key). */
    void clearProductsCache(String key);

    /** Case 6 - seed N lab keys with a fixed TTL, or base+random jitter. */
    AvalancheSeedResponse seedAvalanche(int count, boolean jitter);

    /** Case 6 - remaining TTL of every previously-seeded avalanche key, to compare spread. */
    List<AvalancheTtlEntry> avalancheTtlDistribution();

    /** Case 7 - fire {@code concurrency} concurrent loads of one hot key, with or without the anti-stampede lock. */
    StampedeResultResponse simulateStampede(String key, int concurrency, boolean withLock);

    /** Case 8 - warm cache, mutate DB bypassing eviction, read (stale), then evict-and-read (fixed). */
    List<StaleDataStepResponse> simulateStaleData(String idProduct, BigDecimal newPrice);
}
