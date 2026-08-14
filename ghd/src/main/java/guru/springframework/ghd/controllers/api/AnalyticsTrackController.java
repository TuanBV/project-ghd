package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.dto.analytics.PageViewTrackRequest;
import guru.springframework.ghd.services.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public beacon endpoint hit by the client-side tracking script on every pageview.
 * Kept intentionally trivial - all the work (Redis online-set update + Kafka publish)
 * happens in the service, and the response carries no body so the browser's
 * sendBeacon()/fetch(keepalive) call never blocks page rendering.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analytics")
public class AnalyticsTrackController {

    private final AnalyticsService analyticsService;

    @PostMapping("/track")
    public ResponseEntity<Void> track(@Valid @RequestBody PageViewTrackRequest request, HttpServletRequest httpRequest) {
        try {
            analyticsService.track(request, httpRequest);
        } catch (Exception e) {
            log.warn("Bỏ qua pageview do lỗi tracking: {}", e.getMessage());
        }
        return ResponseEntity.accepted().build();
    }
}
