package proyecto.sistemaGestion.data;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for Data Testing (migrations, constraints, integrity, duplicates, seeds).
 * Unlike {@code ProductIntegrationTest}, this suite keeps Flyway enabled and
 * {@code ddl-auto=validate} so the schema under test is the one built by the real
 * migration scripts in {@code src/main/resources/db/migration}, not one inferred by Hibernate.
 * The container is started once and shared (singleton pattern) so subclasses reuse
 * the same Spring context instead of re-running migrations per class.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractDataTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("datatestdb")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
