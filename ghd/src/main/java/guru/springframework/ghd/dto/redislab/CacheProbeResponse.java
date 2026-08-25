package guru.springframework.ghd.dto.redislab;

import guru.springframework.ghd.dto.product.ProductDetailClientResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Case 1/5 - Cache Hit/Miss + Cache Penetration probe result for
 * {@code GET /admin/v1/test/redis/product/{idProduct}}.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CacheProbeResponse {
    private String cacheName;
    private String cacheKey;
    /** HIT, MISS, or NOT_FOUND (cache penetration - id doesn't exist in DB either). */
    private String cacheStatus;
    private long elapsedMs;
    private int dbQueriesTriggered;
    private ProductDetailClientResponse data;
}
