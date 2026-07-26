package proyecto.sistemaGestion.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Proves that the invariants added in V4__add_data_constraints.sql are enforced by
 * the database itself, not only by Jakarta Bean Validation on the entities.
 * Inserts go through raw JDBC (bypassing ProductService/StockMovementService) so
 * these tests fail if a constraint is ever dropped from the schema, even if the
 * application layer still validates correctly.
 */
class SchemaConstraintsTest extends AbstractDataTest {

    private static final String SKU_PREFIX = "CHK-";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM stock_movements WHERE user_id LIKE ?", SKU_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM products WHERE sku LIKE ?", SKU_PREFIX + "%");
    }

    private void insertProduct(String sku, String name, double price, int quantity, int minStock, String status) {
        jdbcTemplate.update(
                "INSERT INTO products (name, sku, category, price, quantity, min_stock, status) " +
                        "VALUES (?, ?, 'Test', ?, ?, ?, ?)",
                name, sku, price, quantity, minStock, status);
    }

    @Test
    void rejectsNegativePrice() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertProduct(SKU_PREFIX + "NEG-PRICE", "Neg Price", -10.0, 1, 0, "ACTIVE"));
    }

    @Test
    void rejectsZeroPrice() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertProduct(SKU_PREFIX + "ZERO-PRICE", "Zero Price", 0.0, 1, 0, "ACTIVE"));
    }

    @Test
    void rejectsNegativeQuantity() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertProduct(SKU_PREFIX + "NEG-QTY", "Neg Qty", 10.0, -1, 0, "ACTIVE"));
    }

    @Test
    void rejectsNegativeMinStock() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertProduct(SKU_PREFIX + "NEG-MIN", "Neg Min", 10.0, 1, -1, "ACTIVE"));
    }

    @Test
    void rejectsBlankSku() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertProduct("   ", "Blank Sku", 10.0, 1, 0, "ACTIVE"));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertProduct(SKU_PREFIX + "BLANK-NAME", "   ", 10.0, 1, 0, "ACTIVE"));
    }

    @Test
    void rejectsInvalidStatus() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertProduct(SKU_PREFIX + "BAD-STATUS", "Bad Status", 10.0, 1, 0, "DELETED"));
    }

    @Test
    void rejectsStockMovementWithNonPositiveQuantity() {
        insertProduct(SKU_PREFIX + "MOV-BASE-1", "Movement Base", 10.0, 5, 0, "ACTIVE");
        Long productId = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE sku = ?", Long.class, SKU_PREFIX + "MOV-BASE-1");

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO stock_movements (product_id, movement_type, quantity, previous_quantity, new_quantity, user_id) " +
                        "VALUES (?, 'IN', 0, 5, 5, ?)",
                productId, SKU_PREFIX + "tester"));
    }

    @Test
    void rejectsStockMovementWithInvalidType() {
        insertProduct(SKU_PREFIX + "MOV-BASE-2", "Movement Base 2", 10.0, 5, 0, "ACTIVE");
        Long productId = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE sku = ?", Long.class, SKU_PREFIX + "MOV-BASE-2");

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO stock_movements (product_id, movement_type, quantity, previous_quantity, new_quantity, user_id) " +
                        "VALUES (?, 'TRANSFER', 1, 5, 6, ?)",
                productId, SKU_PREFIX + "tester"));
    }

    @Test
    void rejectsStockMovementForNonExistentProduct() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO stock_movements (product_id, movement_type, quantity, previous_quantity, new_quantity, user_id) " +
                        "VALUES (999999, 'IN', 1, 0, 1, ?)",
                SKU_PREFIX + "tester"));
    }
}
