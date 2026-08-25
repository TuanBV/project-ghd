package guru.springframework.ghd.dto.redislab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Case 8 - Stale Data: one step of the simulate/{warm,mutate,read,fix} flow. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaleDataStepResponse {
    private String step;
    private BigDecimal cachedPrice;
    private BigDecimal databasePrice;
    private boolean stale;
}
