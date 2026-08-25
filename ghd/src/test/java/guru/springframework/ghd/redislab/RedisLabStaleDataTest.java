package guru.springframework.ghd.redislab;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.config.CacheConfig;
import guru.springframework.ghd.dto.product.IProductDetailClient;
import guru.springframework.ghd.dto.redislab.StaleDataStepResponse;
import guru.springframework.ghd.entities.Product;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Case 8 - Stale Data. {@code currentPrice} stands in for the "real" row in the database:
 * {@code findByIdProduct}/{@code findById} always read it, {@code save} writes it - so the test
 * can show the cached DTO going stale after a bypassed write, then getting fixed by the same
 * evict-on-write mechanism {@code ProductServiceImpl.updateProduct} already uses in production.
 */
@SpringBootTest
@ActiveProfiles("redis-lab")
class RedisLabStaleDataTest extends AbstractIntegrationTest {

    private static final String PRODUCT_ID = "p5";

    @Autowired
    private RedisLabService redisLabService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductImageRepository productImageRepository;

    private final AtomicReference<BigDecimal> currentPrice = new AtomicReference<>(BigDecimal.valueOf(100));

    @BeforeEach
    void setUp() {
        Objects.requireNonNull(cacheManager.getCache(CacheConfig.PRODUCTS)).clear();
        currentPrice.set(BigDecimal.valueOf(100));

        when(productRepository.findByIdProduct(PRODUCT_ID)).thenAnswer(inv -> stubProduct(currentPrice.get()));
        when(productImageRepository.findByProductId(PRODUCT_ID)).thenReturn(List.of());
        when(productRepository.findById(PRODUCT_ID)).thenAnswer(inv ->
                Optional.of(Product.builder().id(PRODUCT_ID).price(currentPrice.get()).build()));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product product = inv.getArgument(0);
            currentPrice.set(product.getPrice());
            return product;
        });
    }

    @Test
    void bypassingEvictLeavesCacheStaleUntilExplicitlyCleared() {
        List<StaleDataStepResponse> steps = redisLabService.simulateStaleData(PRODUCT_ID, BigDecimal.valueOf(200));

        assertEquals(4, steps.size());
        assertFalse(steps.get(0).isStale(), "warm-cache step should not be stale");
        assertTrue(steps.get(2).isStale(), "read right after the bypassed write must still be stale");
        assertEquals(BigDecimal.valueOf(100), steps.get(2).getCachedPrice());
        assertFalse(steps.get(3).isStale(), "read after evict must reflect the new price");
        assertEquals(BigDecimal.valueOf(200), steps.get(3).getCachedPrice());
    }

    private IProductDetailClient stubProduct(BigDecimal price) {
        IProductDetailClient product = mock(IProductDetailClient.class);
        when(product.getId()).thenReturn(PRODUCT_ID);
        when(product.getTitle()).thenReturn("Stale Data Test Product");
        when(product.getPrice()).thenReturn(price);
        when(product.getSalePrice()).thenReturn(price);
        return product;
    }
}
