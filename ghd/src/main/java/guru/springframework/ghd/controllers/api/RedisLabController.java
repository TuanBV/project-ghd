package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.services.RedisLabMetrics;
import guru.springframework.ghd.services.RedisLabService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Redis Failure &amp; Cache Test Lab - manual/API-driven test endpoints for
 * {@code REDIS_TEST_GUIDE.md}. Only registered under the {@code redis-lab} profile.
 * <p>
 * Mapped under {@code /admin/v1/**} (not {@code /api/v1/**}) so it stays behind the admin
 * security chain's "authenticated by default" rule instead of the blanket GET-permitAll rule
 * that {@code /api/v1/**} carries - same reasoning as {@link AnalyticsStatsController}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/test/redis")
@Profile("redis-lab")
public class RedisLabController extends BaseController {

    private final RedisLabService redisLabService;
    private final RedisLabMetrics redisLabMetrics;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/product/{idProduct}")
    public ResponseEntity<?> probeProduct(@PathVariable String idProduct) {
        return ok(redisLabService.probeProduct(idProduct));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cache/clear")
    public ResponseEntity<?> clearCache(@RequestParam(required = false) String key) {
        redisLabService.clearProductsCache(key);
        return ok(null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/metrics")
    public ResponseEntity<?> metrics() {
        return ok(redisLabMetrics.snapshot());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/avalanche/seed")
    public ResponseEntity<?> seedAvalanche(
            @RequestParam(defaultValue = "100") int count,
            @RequestParam(defaultValue = "false") boolean jitter) {
        return ok(redisLabService.seedAvalanche(count, jitter));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/avalanche/ttl-distribution")
    public ResponseEntity<?> avalancheTtlDistribution() {
        return ok(redisLabService.avalancheTtlDistribution());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/stampede")
    public ResponseEntity<?> stampede(
            @RequestParam String key,
            @RequestParam(defaultValue = "50") int concurrency,
            @RequestParam(defaultValue = "true") boolean withLock) {
        return ok(redisLabService.simulateStampede(key, concurrency, withLock));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/stale-data")
    public ResponseEntity<?> staleData(
            @RequestParam String idProduct,
            @RequestParam BigDecimal newPrice) {
        return ok(redisLabService.simulateStaleData(idProduct, newPrice));
    }
}
