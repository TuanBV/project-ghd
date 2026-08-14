package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.dto.analytics.*;
import guru.springframework.ghd.entities.PageView;
import guru.springframework.ghd.events.PageViewEvent;
import guru.springframework.ghd.repositories.PageViewRepository;
import guru.springframework.ghd.services.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;

import static guru.springframework.ghd.config.KafkaTopicConfig.ANALYTICS_EVENTS_TOPIC;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final String ONLINE_KEY = "analytics:online";
    private static final long ONLINE_WINDOW_MS = 5 * 60 * 1000L;

    private final PageViewRepository pageViewRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.analytics.ip-salt:ghd-analytics-salt}")
    private String ipSalt;

    @Override
    public void track(PageViewTrackRequest request, HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipHash = hashIp(extractClientIp(httpRequest));
        String referrerHost = extractReferrerHost(request.getReferrer(), httpRequest);

        long now = System.currentTimeMillis();
        stringRedisTemplate.opsForZSet().add(ONLINE_KEY, request.getVisitorId(), now);

        PageViewEvent event = new PageViewEvent(
                request.getVisitorId(),
                request.getSessionId(),
                truncate(request.getUrl(), 500),
                referrerHost,
                ipHash,
                truncate(userAgent, 500),
                detectDeviceType(userAgent),
                detectBrowser(userAgent)
        );

        kafkaTemplate.send(ANALYTICS_EVENTS_TOPIC, request.getVisitorId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Không publish được PageViewEvent lên Kafka: {}", ex.getMessage(), ex);
                    }
                });
    }

    @Override
    public void persist(PageViewEvent event) {
        PageView pageView = PageView.builder()
                .visitorId(event.visitorId())
                .sessionId(event.sessionId())
                .url(event.url())
                .referrerHost(event.referrerHost())
                .ipHash(event.ipHash())
                .userAgent(event.userAgent())
                .deviceType(event.deviceType())
                .browser(event.browser())
                .build();

        pageViewRepository.save(pageView);
    }

    @Override
    public AnalyticsOverviewResponse getOverview(String range, String from, String to) {
        DateRange dateRange = resolveRange(range, from, to);

        long totalPageviews = pageViewRepository.countByCreatedDateBetween(dateRange.from(), dateRange.to());
        long uniqueVisitors = pageViewRepository.countDistinctVisitorsBetween(dateRange.from(), dateRange.to());
        PageViewRepository.SessionStatsProjection sessionStats =
                pageViewRepository.findSessionStats(dateRange.from(), dateRange.to());

        long sessionCount = sessionStats != null && sessionStats.getSessionCount() != null
                ? sessionStats.getSessionCount() : 0;
        long bouncedCount = sessionStats != null && sessionStats.getBouncedCount() != null
                ? sessionStats.getBouncedCount() : 0;
        double avgDuration = sessionStats != null && sessionStats.getAvgDurationSeconds() != null
                ? sessionStats.getAvgDurationSeconds() : 0;
        double bounceRate = sessionCount > 0 ? (bouncedCount * 100.0 / sessionCount) : 0;

        return AnalyticsOverviewResponse.builder()
                .totalPageviews(totalPageviews)
                .uniqueVisitors(uniqueVisitors)
                .onlineNow(getOnlineNow())
                .bounceRate(Math.round(bounceRate * 10) / 10.0)
                .avgDurationSeconds(Math.round(avgDuration * 10) / 10.0)
                .build();
    }

    @Override
    public List<TimeSeriesPointResponse> getTimeSeries(String range, String from, String to) {
        DateRange dateRange = resolveRange(range, from, to);

        return pageViewRepository.findDailyStats(dateRange.from(), dateRange.to()).stream()
                .map(row -> TimeSeriesPointResponse.builder()
                        .date(row.getDay().toLocalDate())
                        .pageviews(row.getPageviews())
                        .uniqueVisitors(row.getUniqueVisitors())
                        .build())
                .toList();
    }

    @Override
    public List<DeviceBreakdownResponse> getDeviceBreakdown(String range, String from, String to) {
        DateRange dateRange = resolveRange(range, from, to);

        List<PageViewRepository.DeviceCountProjection> rows =
                pageViewRepository.findDeviceBreakdown(dateRange.from(), dateRange.to());
        long total = rows.stream().mapToLong(PageViewRepository.DeviceCountProjection::getCount).sum();

        return rows.stream()
                .map(row -> DeviceBreakdownResponse.builder()
                        .deviceType(row.getDeviceType())
                        .count(row.getCount())
                        .percentage(total > 0 ? Math.round(row.getCount() * 1000.0 / total) / 10.0 : 0)
                        .build())
                .toList();
    }

    @Override
    public List<TopPageResponse> getTopPages(String range, String from, String to, int limit) {
        DateRange dateRange = resolveRange(range, from, to);

        return pageViewRepository.findTopPages(dateRange.from(), dateRange.to(), limit).stream()
                .map(row -> TopPageResponse.builder().url(row.getUrl()).views(row.getCount()).build())
                .toList();
    }

    @Override
    public List<TopReferrerResponse> getTopReferrers(String range, String from, String to, int limit) {
        DateRange dateRange = resolveRange(range, from, to);

        return pageViewRepository.findTopReferrers(dateRange.from(), dateRange.to(), limit).stream()
                .map(row -> TopReferrerResponse.builder().referrer(row.getReferrerHost()).views(row.getCount()).build())
                .toList();
    }

    @Override
    public long getOnlineNow() {
        long cutoff = System.currentTimeMillis() - ONLINE_WINDOW_MS;
        stringRedisTemplate.opsForZSet().removeRangeByScore(ONLINE_KEY, 0, cutoff);
        Long count = stringRedisTemplate.opsForZSet().zCard(ONLINE_KEY);
        return count != null ? count : 0;
    }

    private DateRange resolveRange(String range, String from, String to) {
        LocalDateTime now = LocalDateTime.now();

        if ("custom".equalsIgnoreCase(range) && StringUtils.hasText(from) && StringUtils.hasText(to)) {
            LocalDateTime fromDateTime = LocalDate.parse(from).atStartOfDay();
            LocalDateTime toDateTime = LocalDate.parse(to).atTime(LocalTime.MAX);
            return new DateRange(fromDateTime, toDateTime);
        }

        if ("today".equalsIgnoreCase(range)) {
            return new DateRange(LocalDate.now().atStartOfDay(), now);
        }

        if ("30d".equalsIgnoreCase(range)) {
            return new DateRange(now.minusDays(30), now);
        }

        // default: 7 days
        return new DateRange(now.minusDays(7), now);
    }

    private record DateRange(LocalDateTime from, LocalDateTime to) {
    }

    private static String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String hashIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((ip + ipSalt).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.warn("Không thể hash IP cho analytics", e);
            return null;
        }
    }

    private static String extractReferrerHost(String referrer, HttpServletRequest httpRequest) {
        if (!StringUtils.hasText(referrer)) {
            return null;
        }
        try {
            String referrerHost = URI.create(referrer).getHost();
            if (referrerHost == null) {
                return null;
            }
            String ownHost = httpRequest.getServerName();
            if (referrerHost.equalsIgnoreCase(ownHost)) {
                return null;
            }
            return referrerHost;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String detectDeviceType(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "DESKTOP";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("ipad") || ua.contains("tablet") || (ua.contains("android") && !ua.contains("mobile"))) {
            return "TABLET";
        }
        if (ua.contains("mobi") || ua.contains("iphone") || ua.contains("android")) {
            return "MOBILE";
        }
        return "DESKTOP";
    }

    private static String detectBrowser(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Other";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) {
            return "Edge";
        }
        if (ua.contains("chrome/") && !ua.contains("edg")) {
            return "Chrome";
        }
        if (ua.contains("firefox/")) {
            return "Firefox";
        }
        if (ua.contains("safari/") && !ua.contains("chrome")) {
            return "Safari";
        }
        return "Other";
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
