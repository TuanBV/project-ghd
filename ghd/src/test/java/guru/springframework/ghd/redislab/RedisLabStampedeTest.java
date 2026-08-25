package guru.springframework.ghd.redislab;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.dto.redislab.StampedeResultResponse;
import guru.springframework.ghd.repositories.ProductRepository;
import guru.springframework.ghd.services.RedisLabService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Case 7 - Cache Stampede. {@code ProductRepository} is mocked (only {@code count()} is
 * exercised, standing in for "the expensive rebuild") so the assertions are about the lab's own
 * locking logic, not real query latency.
 */
@SpringBootTest
@ActiveProfiles("redis-lab")
class RedisLabStampedeTest extends AbstractIntegrationTest {

    @Autowired
    private RedisLabService redisLabService;

    @MockitoBean
    private ProductRepository productRepository;

    @Test
    void withLockOnlyOneRequestRebuildsTheCache() {
        when(productRepository.count()).thenReturn(42L);

        StampedeResultResponse result = redisLabService.simulateStampede("with-lock-key", 40, true);

        assertEquals(1, result.getDbQueriesTriggered(),
                "Exactly one of the 40 concurrent requests should have rebuilt the cache");
    }

    @Test
    void withoutLockManyRequestsRebuildTheCache() {
        when(productRepository.count()).thenReturn(42L);

        StampedeResultResponse result = redisLabService.simulateStampede("no-lock-key", 40, false);

        assertTrue(result.getDbQueriesTriggered() > 1,
                "Expected multiple concurrent requests to all miss the cold cache without a lock, got "
                        + result.getDbQueriesTriggered());
    }
}
