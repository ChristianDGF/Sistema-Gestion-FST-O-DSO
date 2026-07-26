package proyecto.sistemaGestion.data;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates that the Flyway scripts in db/migration apply cleanly against a real
 * Postgres instance and that the resulting schema matches the JPA entities
 * (enforced by ddl-auto=validate in AbstractDataTest, which fails context startup on drift).
 */
class FlywayMigrationTest extends AbstractDataTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationContextStarts_meaningSchemaMatchesEntities() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("entityManagerFactory"));
    }

    @Test
    void allMigrationsWereAppliedAndNoneArePending() {
        MigrationInfo[] applied = flyway.info().applied();
        MigrationInfo[] pending = flyway.info().pending();

        assertEquals(0, pending.length, "There should be no pending migrations after context startup");
        assertTrue(applied.length >= 5, "Expected at least V1..V5 to have been applied");
    }

    @Test
    void rerunningMigrateIsIdempotent() {
        MigrateResult result = flyway.migrate();

        assertEquals(0, result.migrationsExecuted,
                "Re-running migrate() against an up-to-date schema must not re-apply any migration");
    }
}
