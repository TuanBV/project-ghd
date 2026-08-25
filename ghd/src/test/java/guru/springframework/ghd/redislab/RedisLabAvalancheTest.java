package guru.springframework.ghd.redislab;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.dto.redislab.AvalancheTtlEntry;
import guru.springframework.ghd.services.RedisLabService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Case 6 - Cache Avalanche. Without jitter, N keys seeded together all expire together
 * (TTLs stay tightly clustered). With jitter, the same seed spreads TTLs out - this is the
 * behavior the "before/after" comparison in REDIS_TEST_GUIDE.md relies on.
 */
@SpringBootTest
@ActiveProfiles("redis-lab")
class RedisLabAvalancheTest extends AbstractIntegrationTest {

    @Autowired
    private RedisLabService redisLabService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${app.redis-lab.namespace}")
    private String labNamespace;

    @BeforeEach
    void clearAvalancheKeys() {
        Set<String> keys = stringRedisTemplate.keys(labNamespace + ":avalanche:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Test
    void withoutJitterTtlsStayTightlyClustered() {
        redisLabService.seedAvalanche(30, false);

        List<AvalancheTtlEntry> ttls = redisLabService.avalancheTtlDistribution();
        long min = ttls.stream().mapToLong(AvalancheTtlEntry::getRemainingTtlSeconds).min().orElseThrow();
        long max = ttls.stream().mapToLong(AvalancheTtlEntry::getRemainingTtlSeconds).max().orElseThrow();

        assertTrue(max - min <= 2, "Expected all keys to expire within ~2s of each other, got spread=" + (max - min));
    }

    @Test
    void withJitterTtlsAreSpreadOut() {
        redisLabService.seedAvalanche(50, true);

        List<AvalancheTtlEntry> ttls = redisLabService.avalancheTtlDistribution();
        long min = ttls.stream().mapToLong(AvalancheTtlEntry::getRemainingTtlSeconds).min().orElseThrow();
        long max = ttls.stream().mapToLong(AvalancheTtlEntry::getRemainingTtlSeconds).max().orElseThrow();

        assertTrue(max - min > 5, "Expected jittered TTLs to spread by more than 5s, got spread=" + (max - min));
    }
}
