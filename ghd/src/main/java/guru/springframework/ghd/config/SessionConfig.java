package guru.springframework.ghd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Spring Session (Redis) attribute serialization.
 * <p>
 * Spring Boot's Redis session auto-configuration picks up a bean of type
 * {@code RedisSerializer<Object>} and uses it instead of the JDK-native default, so
 * session attributes are stored as JSON rather than Java serialization.
 * <p>
 * This uses the Jackson 3 ({@code tools.jackson}) serializer to match Spring Boot
 * 4's native JSON stack (see {@link CacheConfig}). Note: Spring Security's
 * {@code SecurityJackson2Modules} (for serializing a {@code SecurityContext} placed
 * in the HttpSession) targets classic Jackson 2 and cannot be registered on this
 * Jackson 3 mapper. This is not a practical gap today: the {@code /api/v1/**} and
 * {@code /admin/v1/**} chains are JWT/stateless and never store a SecurityContext in
 * the HttpSession (see {@link SecurityConfig}), and the client chain does not
 * authenticate via HttpSession either. If a SecurityContext-in-session flow is
 * introduced later, a dedicated Jackson 2 serializer/session namespace would be
 * needed for that specific attribute.
 */
@Configuration
public class SessionConfig {

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return GenericJacksonJsonRedisSerializer.create(builder -> builder.enableUnsafeDefaultTyping());
    }
}
