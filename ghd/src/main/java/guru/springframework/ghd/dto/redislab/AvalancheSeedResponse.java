package guru.springframework.ghd.dto.redislab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Case 6 - Cache Avalanche: result of seeding N lab keys with/without TTL jitter. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvalancheSeedResponse {
    private int keysSeeded;
    private boolean jitterEnabled;
    private long baseTtlSeconds;
    private long jitterMaxSeconds;
}
