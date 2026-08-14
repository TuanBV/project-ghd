package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Mapped under /admin/v1/** (not /api/v1/**) so it stays behind the admin
 * security chain's "authenticated by default" rule instead of the blanket
 * GET-permitAll rule that /api/v1/** carries for the public client site.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/analytics")
public class AnalyticsStatsController extends BaseController {

    private final AnalyticsService analyticsService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/overview")
    public ResponseEntity<?> overview(
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return ok(analyticsService.getOverview(range, from, to));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/timeseries")
    public ResponseEntity<?> timeseries(
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return ok(analyticsService.getTimeSeries(range, from, to));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/devices")
    public ResponseEntity<?> devices(
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return ok(analyticsService.getDeviceBreakdown(range, from, to));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/top-pages")
    public ResponseEntity<?> topPages(
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ok(analyticsService.getTopPages(range, from, to, limit));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/top-referrers")
    public ResponseEntity<?> topReferrers(
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ok(analyticsService.getTopReferrers(range, from, to, limit));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/realtime")
    public ResponseEntity<?> realtime() {
        return ok(Map.of("onlineNow", analyticsService.getOnlineNow()));
    }
}
