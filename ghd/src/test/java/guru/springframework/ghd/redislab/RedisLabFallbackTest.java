package guru.springframework.ghd.redislab;

import com.github.dockerjava.api.DockerClient;
import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.config.CacheConfig;
import guru.springframework.ghd.dto.product.IProductDetailClient;
import guru.springframework.ghd.dto.redislab.CacheProbeResponse;
import guru.springframework.ghd.repositories.ProductImageRepository;
import guru.springframework.ghd.repositories.ProductRepository;
import guru.springframework.ghd.services.RedisLabService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Case 2 - Redis Down. Pauses (SIGSTOP, via the Docker daemon) the shared Testcontainers Redis
 * used by {@link AbstractIntegrationTest} just for the duration of one call, so every Redis
 * command from the app blocks until {@code spring.data.redis.timeout} then fails - exactly what
 * happens against a hung/unreachable real Redis. Always unpauses in a finally block: this
 * container is {@code withReuse(true)} and shared with every other test class in the JVM.
 * <p>
 * Before {@code CacheConfig.cacheErrorHandler()} existed, this scenario made
 * {@code ProductServiceImpl.getByIdProduct} (and every other {@code @Cacheable} method) throw
 * and return 500. This test is the regression guard for that fix.
 */
@SpringBootTest
@ActiveProfiles("redis-lab")
class RedisLabFallbackTest extends AbstractIntegrationTest {

    @Autowired
    private RedisLabService redisLabService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductImageRepository productImageRepository;

    @BeforeEach
    void clearCache() {
        Objects.requireNonNull(cacheManager.getCache(CacheConfig.PRODUCTS)).clear();
    }

    @Test
    void redisDownFallsBackToDatabaseInsteadOfFailing() {
        IProductDetailClient product = mock(IProductDetailClient.class);
        when(product.getId()).thenReturn("p3");
        when(product.getTitle()).thenReturn("Fallback Product");
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(99));
        when(productRepository.findByIdProduct("p3")).thenReturn(product);
        when(productImageRepository.findByProductId("p3")).thenReturn(List.of());

        DockerClient dockerClient = REDIS.getDockerClient();
        dockerClient.pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            CacheProbeResponse response = redisLabService.probeProduct("p3");

            assertEquals("REDIS_DOWN", response.getCacheStatus());
            assertNotNull(response.getData());
            assertEquals("Fallback Product", response.getData().getTitle());
        } finally {
            dockerClient.unpauseContainerCmd(REDIS.getContainerId()).exec();
            waitForRedisToRecover();
        }
    }

    // The shared Lettuce connection factory (also used by RedisLabCacheHitMissTest,
    // RedisLabStaleDataTest, ... via Spring's test context cache) doesn't necessarily
    // reconnect the instant the container is unpaused - without this, the NEXT test class
    // to reuse this context can itself see "Unable to connect to Redis" in its own @BeforeEach,
    // even though the container is healthy again by then.
    private void waitForRedisToRecover() {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                stringRedisTemplate.hasKey("redislab:warmup-after-pause");
                return;
            } catch (RuntimeException e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
