package guru.springframework.ghd.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Trạng thái JWT phía server, lưu trong Redis, TTL theo từng token (khác {@code CacheConfig}
 * vốn dùng TTL cố định theo cache name) - xem .claude/skills/spring-security-ghd/SKILL.md.
 * <p>
 * - Access token: blacklist (revoke sớm khi logout, tự hết hạn theo TTL).
 * - Refresh token: whitelist (chỉ token vừa phát hành mới hợp lệ - rotate mỗi lần dùng).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenStoreService {

    private static final String VALID_MARKER = "1";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.cache.namespace}")
    private String namespace;

    public void blacklistAccessToken(String jti, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(blacklistKey(jti), VALID_MARKER, Duration.ofSeconds(ttlSeconds));
    }

    // Fail-open: called on every authenticated request (JwtAuthenticationFilter), so a Redis
    // outage/timeout here used to 500 every single request that carried a JWT - not just cache
    // reads. Redis being down means we genuinely don't know whether this token was revoked;
    // treating "unknown" as "not blacklisted" keeps the app usable during the outage instead of
    // locking out every logged-in user, at the cost of a revoked token staying valid until it
    // naturally expires (jwt.access-token.expiration, 30 min default) if revoked exactly while
    // Redis is unreachable. Deliberate trade-off, confirmed with the project owner - see
    // REDIS_TEST_GUIDE.md Case 2/3 for how this gap was found (via the Redis Test Lab).
    public boolean isAccessTokenBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(jti)));
        } catch (RuntimeException e) {
            log.warn("[REDIS] Connection failed checking blacklist for jti={} - failing open (treating as not blacklisted): {}",
                    jti, e.getMessage());
            return false;
        }
    }

    public void registerRefreshToken(String username, String jti, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(refreshKey(username, jti), VALID_MARKER, Duration.ofSeconds(ttlSeconds));
    }

    public boolean isRefreshTokenValid(String username, String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(refreshKey(username, jti)));
    }

    public void revokeRefreshToken(String username, String jti) {
        redisTemplate.delete(refreshKey(username, jti));
    }

    private String blacklistKey(String jti) {
        return namespace + ":jwt:blacklist:" + jti;
    }

    private String refreshKey(String username, String jti) {
        return namespace + ":jwt:refresh:" + username + ":" + jti;
    }
}
