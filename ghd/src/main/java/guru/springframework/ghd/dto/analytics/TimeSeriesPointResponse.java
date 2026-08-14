package guru.springframework.ghd.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TimeSeriesPointResponse {
    private LocalDate date;
    private long pageviews;
    private long uniqueVisitors;
}
