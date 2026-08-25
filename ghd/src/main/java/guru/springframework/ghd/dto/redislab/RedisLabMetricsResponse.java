package guru.springframework.ghd.dto.redislab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RedisLabMetricsResponse {
    private String redisStatus;
    private long cacheHit;
    private long cacheMiss;
    private String cacheHitRate;
    private long redisErrors;
    private long fallbackToDatabase;
    private long databaseQueries;
    private long databaseQueriesPrevented;
}
