package guru.springframework.ghd.services;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.config.CacheConfig;
import guru.springframework.ghd.dto.product.IProductDetailClient;
import guru.springframework.ghd.dto.product.ProductDetailClientResponse;
import guru.springframework.ghd.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code getLatestProducts}/{@code getLatestProductsByCategory} return a
 * {@link Page}, which is riskier to cache correctly than a plain List (Page/Pageable
 * need to round-trip through JSON). This proves the Redis-backed cache actually
 * serializes/deserializes a {@code Page<ProductDetailClientResponse>} correctly end
 * to end, not just that annotations are present.
 */
@SpringBootTest
class ProductServiceCacheTest extends AbstractIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ProductRepository productRepository;

    @BeforeEach
    void clearCache() {
        Objects.requireNonNull(cacheManager.getCache(CacheConfig.PRODUCTS_LATEST)).clear();
    }

    @Test
    void latestProductsPageRoundTripsThroughRedisAndHitsCacheOnSecondCall() {
        IProductDetailClient item = mock(IProductDetailClient.class);
        when(item.getId()).thenReturn("p1");
        when(item.getTitle()).thenReturn("Cached Product");
        when(item.getSlug()).thenReturn("cached-product");
        when(item.getPrice()).thenReturn(BigDecimal.valueOf(199_000));

        Page<IProductDetailClient> repoPage = new PageImpl<>(java.util.List.of(item), PageRequest.of(0, 4), 1);
        when(productRepository.getLatestProducts(any())).thenReturn(repoPage);

        Page<ProductDetailClientResponse> first = productService.getLatestProducts(4);
        Page<ProductDetailClientResponse> second = productService.getLatestProducts(4);

        verify(productRepository, times(1)).getLatestProducts(any());

        assertThat(second.getTotalElements()).isEqualTo(1);
        assertThat(second.getContent()).hasSize(1);
        assertThat(second.getContent().get(0).getId()).isEqualTo("p1");
        assertThat(second.getContent().get(0).getSlug()).isEqualTo("cached-product");
        assertThat(second.getContent().get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(199_000));
    }
}
