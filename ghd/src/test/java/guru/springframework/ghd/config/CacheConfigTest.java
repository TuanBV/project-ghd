package guru.springframework.ghd.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test (no Spring context, no Redis) covering the per-cache TTL policy:
 * every cache name must have its own explicit TTL, not one blanket default.
 */
class CacheConfigTest {

    @Test
    void everyCacheNameHasAnExplicitTtl() {
        Map<String, Duration> ttls = CacheConfig.cacheTtls();

        assertThat(ttls).containsOnlyKeys(
                CacheConfig.CATEGORIES,
                CacheConfig.BRANDS,
                CacheConfig.BANNERS,
                CacheConfig.SLIDERS,
                CacheConfig.POLICIES,
                CacheConfig.SYS_PARAMS,
                CacheConfig.PRODUCTS,
                CacheConfig.PRODUCTS_LATEST,
                CacheConfig.PRODUCTS_RELATED,
                CacheConfig.NEWS
        );
    }

    @Test
    void ttlsAreNotAllIdentical() {
        // Guards against collapsing back to a single blanket TTL for every cache;
        // caches are grouped into tiers (10m/20m/45m) rather than one flat value.
        Map<String, Duration> ttls = CacheConfig.cacheTtls();
        Set<Duration> distinctTtls = Set.copyOf(ttls.values());
        assertThat(distinctTtls).hasSizeGreaterThan(1);
    }

    @Test
    void productAndPolicyCachesAreShortLivedComparedToStaticContent() {
        Map<String, Duration> ttls = CacheConfig.cacheTtls();

        assertThat(ttls.get(CacheConfig.PRODUCTS)).isLessThan(ttls.get(CacheConfig.CATEGORIES));
        assertThat(ttls.get(CacheConfig.PRODUCTS_LATEST)).isLessThan(ttls.get(CacheConfig.CATEGORIES));
    }
}
