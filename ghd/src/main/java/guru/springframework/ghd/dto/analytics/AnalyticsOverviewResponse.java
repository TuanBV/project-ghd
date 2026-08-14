package guru.springframework.ghd.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnalyticsOverviewResponse {
    private long totalPageviews;
    private long uniqueVisitors;
    private long onlineNow;
    private double bounceRate;
    private double avgDurationSeconds;
}
