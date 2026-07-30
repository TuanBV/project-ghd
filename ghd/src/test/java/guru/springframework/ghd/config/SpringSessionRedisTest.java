package guru.springframework.ghd.config;

import guru.springframework.ghd.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Spring Session (Redis) wiring end-to-end against the real Redis
 * Testcontainers instance: a session created through {@link SessionRepository} is
 * actually persisted in Redis under the configured namespace, with a TTL set, and
 * round-trips its attributes correctly (JSON, via {@link SessionConfig}).
 * <p>
 * This exercises the Spring Session infrastructure directly; it does not depend on
 * the application already storing anything in HttpSession today (see SessionConfig).
 */
@SpringBootTest
class SpringSessionRedisTest extends AbstractIntegrationTest {

    @Autowired
    private SessionRepository<? extends Session> sessionRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${spring.session.redis.namespace}")
    private String sessionNamespace;

    @Test
    void sessionRoundTripsThroughRedisWithNamespaceAndTtl() {
        exerciseSession(sessionRepository);
    }

    // A single type parameter <S> captures the repository's session type consistently
    // across create/save/findById - mixing independent wildcard captures on the field
    // directly does not compile.
    private <S extends Session> void exerciseSession(SessionRepository<S> repository) {
        S session = repository.createSession();
        session.setAttribute("greeting", "hello-redis-session");
        repository.save(session);

        S loaded = repository.findById(session.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.<String>getAttribute("greeting")).isEqualTo("hello-redis-session");

        Set<String> keys = stringRedisTemplate.keys(sessionNamespace + "*" + session.getId() + "*");
        assertThat(keys).as("session %s should be stored under namespace %s", session.getId(), sessionNamespace)
                .isNotEmpty();

        Long ttl = stringRedisTemplate.getExpire(keys.iterator().next());
        assertThat(ttl).as("session key must have a TTL, not persist forever").isPositive();
    }
}
