package proyecto.sistemaGestion.data;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the reference seed data added in V5__seed_reference_data.sql: it must
 * be present after migration, satisfy the same CHECK constraints as real data,
 * and never be duplicated if the seed statement runs more than once.
 */
class SeedDataTest extends AbstractDataTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int countSeedProducts() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE sku LIKE 'SEED-%'", Integer.class);
    }

    @Test
    void seedProductsExistAfterMigrations() {
        assertEquals(3, countSeedProducts());

        BigDecimal price = jdbcTemplate.queryForObject(
                "SELECT price FROM products WHERE sku = 'SEED-LAP-001'", BigDecimal.class);
        assertEquals(0, BigDecimal.valueOf(899.99).compareTo(price));
    }

    @Test
    void seedProductsSatisfyTheSameCheckConstraintsAsRealData() {
        Integer violating = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE sku LIKE 'SEED-%' " +
                        "AND (price <= 0 OR quantity < 0 OR min_stock < 0 OR btrim(name) = '' OR btrim(sku) = '')",
                Integer.class);
        assertEquals(0, violating);
    }

    @Test
    void reRunningSeedStatement_doesNotDuplicateRows() {
        int before = countSeedProducts();

        jdbcTemplate.update(
                "INSERT INTO products (name, sku, description, category, price, quantity, min_stock, status) " +
                        "VALUES ('Laptop Demo 14\"', 'SEED-LAP-001', 'Producto semilla para ambiente demo', " +
                        "'Electronics', 899.99, 25, 5, 'ACTIVE') ON CONFLICT (sku) DO NOTHING");

        assertEquals(before, countSeedProducts(),
                "ON CONFLICT (sku) DO NOTHING must keep the seed statement idempotent");
        assertTrue(before > 0);
    }
}
