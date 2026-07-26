package proyecto.sistemaGestion.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import proyecto.sistemaGestion.dto.ProductRequest;
import proyecto.sistemaGestion.dto.ProductResponse;
import proyecto.sistemaGestion.dto.StockMovementRequest;
import proyecto.sistemaGestion.enums.MovementType;
import proyecto.sistemaGestion.exception.BusinessException;
import proyecto.sistemaGestion.exception.ResourceNotFoundException;
import proyecto.sistemaGestion.service.ProductService;
import proyecto.sistemaGestion.service.StockMovementService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates referential integrity between products and stock_movements:
 * a product with movement history must never be orphaned or leave dangling rows behind,
 * whether the deletion is attempted through the service layer or directly against the database.
 */
class DataIntegrityTest extends AbstractDataTest {

    private static final String SKU_PREFIX = "INTEG-";

    @Autowired
    private ProductService productService;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM stock_movements WHERE product_id IN (SELECT id FROM products WHERE sku LIKE ?)", SKU_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM products WHERE sku LIKE ?", SKU_PREFIX + "%");
    }

    private ProductResponse createProduct(String sku) {
        ProductRequest request = ProductRequest.builder()
                .name("Integrity Product").sku(sku)
                .category("Test").price(BigDecimal.TEN)
                .quantity(10).minStock(1).build();
        return productService.create(request);
    }

    @Test
    void deletingProductWithMovements_isRejectedWithBusinessException() {
        ProductResponse product = createProduct(SKU_PREFIX + "WITH-MOV");
        stockMovementService.registerMovement(StockMovementRequest.builder()
                .productId(product.getId()).movementType(MovementType.IN)
                .quantity(5).userId("tester").build());

        assertThrows(BusinessException.class, () -> productService.delete(product.getId()));

        // neither the product nor its movement history should have been removed
        assertNotNull(productService.findById(product.getId()));
        Integer movementCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_movements WHERE product_id = ?", Integer.class, product.getId());
        assertEquals(1, movementCount);
    }

    @Test
    void deletingProductWithoutMovements_succeeds() {
        ProductResponse product = createProduct(SKU_PREFIX + "NO-MOV");

        productService.delete(product.getId());

        assertThrows(ResourceNotFoundException.class, () -> productService.findById(product.getId()));
    }

    @Test
    void directDatabaseDeleteBypassingService_isStillRejectedByForeignKey() {
        ProductResponse product = createProduct(SKU_PREFIX + "FK-BYPASS");
        stockMovementService.registerMovement(StockMovementRequest.builder()
                .productId(product.getId()).movementType(MovementType.IN)
                .quantity(3).userId("tester").build());

        // even bypassing ProductService's own existsByProductId check, the FK itself must block the delete
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update("DELETE FROM products WHERE id = ?", product.getId()));
    }
}
