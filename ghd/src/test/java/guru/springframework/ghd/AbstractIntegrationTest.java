package guru.springframework.ghd;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for tests that need a real MySQL + Redis, started via Testcontainers so
 * no manually-run local services are required. Containers are shared (static) across
 * all subclasses in the same JVM to keep the overall test suite fast - Spring's
 * context cache also reuses a single ApplicationContext across subclasses since the
 * container-backed properties are identical.
 */
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4.11"))
            .withDatabaseName("core")
            .withReuse(true);

    @Container
    @ServiceConnection
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4.10-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);
}
