package guru.springframework.ghd.dto.redislab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Case 7 - Cache Stampede: result of firing N concurrent requests for one hot key. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StampedeResultResponse {
    private int totalRequests;
    private boolean lockEnabled;
    private int dbQueriesTriggered;
    private long elapsedMs;
}
