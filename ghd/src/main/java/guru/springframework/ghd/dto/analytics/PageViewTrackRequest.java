package guru.springframework.ghd.dto.analytics;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageViewTrackRequest {

    @NotBlank
    private String url;

    private String referrer;

    @NotBlank
    private String visitorId;

    @NotBlank
    private String sessionId;
}
