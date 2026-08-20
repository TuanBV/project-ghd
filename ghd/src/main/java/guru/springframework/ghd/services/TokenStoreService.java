package guru.springframework.ghd.services;

import lombok.RequiredArgsConstructor;
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

    public boolean isAccessTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(jti)));
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
