package guru.springframework.ghd.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redis-backed Spring Cache configuration.
 * <p>
 * Every public, read-heavy/low-change dataset gets its own named cache with an
 * explicit TTL below (no single blanket TTL). Cache names are also used as the
 * eviction target in the service layer ({@code @CacheEvict(cacheNames = ..., allEntries = true)}),
 * so correctness on write wins over precise per-entry invalidation.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String CATEGORIES = "categories";
    public static final String BRANDS = "brands";
    public static final String BANNERS = "banners";
    public static final String SLIDERS = "sliders";
    public static final String POLICIES = "policies";
    public static final String SYS_PARAMS = "sys-params";
    public static final String PRODUCTS = "products";
    public static final String PRODUCTS_LATEST = "products-latest";
    public static final String PRODUCTS_RELATED = "products-related";
    public static final String NEWS = "news";

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private static final Map<String, Duration> CACHE_TTLS = buildCacheTtls();

    private static Map<String, Duration> buildCacheTtls() {
        Map<String, Duration> ttls = new LinkedHashMap<>();
        ttls.put(CATEGORIES, Duration.ofMinutes(45));
        ttls.put(BRANDS, Duration.ofMinutes(45));
        ttls.put(BANNERS, Duration.ofMinutes(20));
        ttls.put(SLIDERS, Duration.ofMinutes(20));
        ttls.put(POLICIES, Duration.ofMinutes(45));
        ttls.put(SYS_PARAMS, Duration.ofMinutes(45));
        ttls.put(PRODUCTS, Duration.ofMinutes(10));
        ttls.put(PRODUCTS_LATEST, Duration.ofMinutes(10));
        ttls.put(PRODUCTS_RELATED, Duration.ofMinutes(10));
        ttls.put(NEWS, Duration.ofMinutes(20));
        return Collections.unmodifiableMap(ttls);
    }

    /** Exposed for {@code CacheConfigTest} so TTL assignments are covered without a live Redis. */
    public static Map<String, Duration> cacheTtls() {
        return CACHE_TTLS;
    }

    @Value("${app.cache.namespace}")
    private String cacheNamespace;

    @Bean
    public GenericJacksonJsonRedisSerializer cacheRedisSerializer() {
        // Jackson 3 (tools.jackson) is Spring Boot 4's native JSON stack (spring-boot-starter-jackson);
        // java.time (de)serialization is bundled directly in jackson-databind 3.x, no extra module needed.
        return GenericJacksonJsonRedisSerializer.create(builder -> builder
                .enableSpringCacheNullValueSupport()
                .enableUnsafeDefaultTyping());
    }

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(GenericJacksonJsonRedisSerializer cacheRedisSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> cacheNamespace + "::" + cacheName + "::")
                .disableCachingNullValues()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(cacheRedisSerializer));
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                          RedisCacheConfiguration redisCacheConfiguration) {
        // Spring Data Redis 4.0 defaults RedisCacheWriter to asynchronous, deferred
        // writes/evictions (for throughput) when the connection factory also supports
        // reactive access, as Lettuce's does here. That is wrong for this app: every
        // @CacheEvict is relied on to make the next read immediately consistent (e.g.
        // right after a product update), so writes/evictions must be immediate/blocking.
        RedisCacheWriter cacheWriter = RedisCacheWriter.create(connectionFactory,
                RedisCacheWriter.RedisCacheWriterConfigurer::immediateWrites);

        Map<String, RedisCacheConfiguration> perCacheConfigurations = new LinkedHashMap<>();
        CACHE_TTLS.forEach((name, ttl) -> perCacheConfigurations.put(name, redisCacheConfiguration.entryTtl(ttl)));

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(redisCacheConfiguration)
                .withInitialCacheConfigurations(perCacheConfigurations)
                .build();
    }

    // Implementing CachingConfigurer (rather than just declaring a standalone @Bean) is
    // required for this to actually reach CacheInterceptor: AbstractCachingConfiguration only
    // wires a custom CacheErrorHandler/KeyGenerator/CacheResolver in through a CachingConfigurer
    // bean - with none present it never looks up a CacheErrorHandler bean by type at all, so a
    // plain @Bean CacheErrorHandler here would be silently ignored (verified with a real Redis
    // Test Lab integration test before adding this interface - the plain @Bean version still
    // rethrew and 500'd).
    //
    // Without this, the default SimpleCacheErrorHandler RETHROWS any exception thrown while
    // talking to Redis (connection refused, timeout...), so every @Cacheable method
    // (getByIdProduct, getBySlug, category/brand/news lists...) would fail with a 500 the
    // moment Redis is unreachable, even though it's only a cache in front of MySQL. Swallowing
    // the error here instead lets the annotated method fall through to its own body (cache miss
    // path -> query the DB) on GET failures, and simply skips the write/evict on
    // PUT/EVICT/CLEAR failures instead of failing the request.
    @Override
    public CacheErrorHandler errorHandler() {
        return cacheErrorHandler();
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[REDIS] Connection failed on GET {}::{} - {}. [FALLBACK] Switching to database",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("[REDIS] Connection failed on PUT {}::{} - {}. Skipping cache write",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[REDIS] Connection failed on EVICT {}::{} - {}. Skipping cache eviction",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("[REDIS] Connection failed on CLEAR {} - {}. Skipping cache clear",
                        cache.getName(), exception.getMessage());
            }
        };
    }

    // Redis cache persists across `docker compose down`/rebuild (it's a durable volume,
    // separate from the app container), so a value cached before a DB reset/restore can
    // silently survive the rebuild and keep serving stale (e.g. empty) results forever -
    // this bit the client-side slider/banner/category/brand lists after one such reset.
    // Clearing every cache once on startup guarantees each rebuild/restart repopulates
    // from the current DB on first access instead of trusting whatever was left behind.
    @Bean
    public ApplicationRunner clearCachesOnStartup(CacheManager cacheManager) {
        return args -> cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }
}
