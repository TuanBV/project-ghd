package guru.springframework.ghd.dto.redislab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row of {@code GET /admin/v1/test/redis/avalanche/ttl-distribution}. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvalancheTtlEntry {
    private String key;
    private long remainingTtlSeconds;
}
