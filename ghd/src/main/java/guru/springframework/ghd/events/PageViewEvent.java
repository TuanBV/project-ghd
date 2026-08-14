package guru.springframework.ghd.events;

public record PageViewEvent(
        String visitorId,
        String sessionId,
        String url,
        String referrerHost,
        String ipHash,
        String userAgent,
        String deviceType,
        String browser
) {
}
