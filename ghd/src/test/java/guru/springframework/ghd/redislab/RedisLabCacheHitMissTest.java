package guru.springframework.ghd.redislab;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Case 1 (cache hit/miss) and Case 5 (penetration / null-caching) - exercises the real Redis
 * cache used by {@code ProductServiceImpl.getByIdProduct} (via {@link RedisLabService}), with
 * the JPA repository mocked so hit/miss can be asserted by invocation count, same pattern as
 * {@code CategoryServiceCacheTest}.
 */
@SpringBootTest
@ActiveProfiles("redis-lab")
class RedisLabCacheHitMissTest extends AbstractIntegrationTest {

    @Autowired
    private RedisLabService redisLabService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductImageRepository productImageRepository;

    @BeforeEach
    void clearCache() {
        Objects.requireNonNull(cacheManager.getCache(CacheConfig.PRODUCTS)).clear();
    }

    @Test
    void firstCallIsMissSecondCallIsHit() {
        // Build the stub BEFORE opening the outer when(...) - calling mock()/when() while
        // evaluating another when(...)'s argument confuses Mockito's ongoing-stubbing state.
        IProductDetailClient stub = stubProduct("p1", "Test Product", BigDecimal.TEN);
        when(productRepository.findByIdProduct("p1")).thenReturn(stub);
        when(productImageRepository.findByProductId("p1")).thenReturn(List.of());

        CacheProbeResponse first = redisLabService.probeProduct("p1");
        assertEquals("MISS", first.getCacheStatus());
        assertNotNull(first.getData());

        CacheProbeResponse second = redisLabService.probeProduct("p1");
        assertEquals("HIT", second.getCacheStatus());

        verify(productRepository, times(1)).findByIdProduct("p1");
    }

    @Test
    void unknownIdIsPenetrationProtectedAfterFirstMiss() {
        when(productRepository.findByIdProduct("missing")).thenReturn(null);

        CacheProbeResponse first = redisLabService.probeProduct("missing");
        assertEquals("NOT_FOUND", first.getCacheStatus());

        CacheProbeResponse second = redisLabService.probeProduct("missing");
        assertEquals("NOT_FOUND", second.getCacheStatus());

        // Case 5: the 2nd request never reaches the DB again - the NULL marker short-circuits it.
        verify(productRepository, times(1)).findByIdProduct("missing");
    }

    @Test
    void clearingCacheForcesNextReadBackToDatabase() {
        IProductDetailClient stub = stubProduct("p2", "Clear Test", BigDecimal.ONE);
        when(productRepository.findByIdProduct("p2")).thenReturn(stub);
        when(productImageRepository.findByProductId("p2")).thenReturn(List.of());

        redisLabService.probeProduct("p2");
        redisLabService.probeProduct("p2");
        verify(productRepository, times(1)).findByIdProduct("p2");

        redisLabService.clearProductsCache(null);

        redisLabService.probeProduct("p2");
        verify(productRepository, times(2)).findByIdProduct("p2");
    }

    private IProductDetailClient stubProduct(String id, String title, BigDecimal price) {
        IProductDetailClient product = mock(IProductDetailClient.class);
        when(product.getId()).thenReturn(id);
        when(product.getTitle()).thenReturn(title);
        when(product.getPrice()).thenReturn(price);
        when(product.getSalePrice()).thenReturn(price);
        return product;
    }
}
