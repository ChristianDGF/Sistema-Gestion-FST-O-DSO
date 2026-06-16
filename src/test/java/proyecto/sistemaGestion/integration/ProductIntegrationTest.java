package proyecto.sistemaGestion.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import proyecto.sistemaGestion.dto.ProductRequest;
import proyecto.sistemaGestion.dto.ProductResponse;
import proyecto.sistemaGestion.dto.StockMovementRequest;
import proyecto.sistemaGestion.dto.StockMovementResponse;
import proyecto.sistemaGestion.enums.MovementType;
import proyecto.sistemaGestion.enums.ProductStatus;
import proyecto.sistemaGestion.exception.BusinessException;
import proyecto.sistemaGestion.exception.ResourceNotFoundException;
import proyecto.sistemaGestion.service.ProductService;
import proyecto.sistemaGestion.service.StockMovementService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ProductIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private StockMovementService stockMovementService;

    @Test
    void shouldCreateProduct() {
        ProductRequest request = ProductRequest.builder()
                .name("Integration Laptop").sku("INT-LAP-001")
                .category("Electronics").price(BigDecimal.valueOf(999.99))
                .quantity(10).minStock(3).build();

        ProductResponse result = productService.create(request);

        assertNotNull(result.getId());
        assertEquals("Integration Laptop", result.getName());
        assertEquals(ProductStatus.ACTIVE, result.getStatus());
    }

    @Test
    void shouldFindProductById() {
        ProductRequest request = ProductRequest.builder()
                .name("Findable Product").sku("FIND-001")
                .category("Test").price(BigDecimal.valueOf(50))
                .quantity(5).minStock(1).build();
        ProductResponse created = productService.create(request);

        ProductResponse found = productService.findById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Findable Product", found.getName());
    }

    @Test
    void shouldThrowException_whenProductNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> productService.findById(9999L));
    }

    @Test
    void shouldUpdateProduct() {
        ProductRequest request = ProductRequest.builder()
                .name("Original").sku("UPD-001")
                .category("Test").price(BigDecimal.valueOf(100))
                .quantity(10).minStock(2).build();
        ProductResponse created = productService.create(request);

        ProductRequest updateRequest = ProductRequest.builder()
                .name("Updated Name").sku("UPD-001")
                .category("Test").price(BigDecimal.valueOf(150))
                .quantity(20).minStock(5).build();
        ProductResponse updated = productService.update(created.getId(), updateRequest);

        assertEquals("Updated Name", updated.getName());
        assertEquals(0, BigDecimal.valueOf(150).compareTo(updated.getPrice()));
        assertEquals(20, updated.getQuantity());
    }

    @Test
    void shouldDeleteProduct() {
        ProductRequest request = ProductRequest.builder()
                .name("Deletable").sku("DEL-001")
                .category("Test").price(BigDecimal.valueOf(25))
                .quantity(1).minStock(0).build();
        ProductResponse created = productService.create(request);

        productService.delete(created.getId());

        assertThrows(ResourceNotFoundException.class, () -> productService.findById(created.getId()));
    }

    @Test
    void shouldThrowException_whenCreatingDuplicateSku() {
        ProductRequest request = ProductRequest.builder()
                .name("First").sku("DUP-001")
                .category("Test").price(BigDecimal.TEN)
                .quantity(5).minStock(1).build();
        productService.create(request);

        ProductRequest duplicate = ProductRequest.builder()
                .name("Second").sku("DUP-001")
                .category("Test").price(BigDecimal.TEN)
                .quantity(5).minStock(1).build();
        assertThrows(BusinessException.class, () -> productService.create(duplicate));
    }

    @Test
    void shouldRegisterInMovement() {
        ProductRequest request = ProductRequest.builder()
                .name("Stock Test").sku("STK-IN-001")
                .category("Test").price(BigDecimal.valueOf(30))
                .quantity(10).minStock(2).build();
        ProductResponse product = productService.create(request);

        StockMovementRequest movement = StockMovementRequest.builder()
                .productId(product.getId()).movementType(MovementType.IN)
                .quantity(5).userId("tester").observations("Integration test IN").build();

        StockMovementResponse result = stockMovementService.registerMovement(movement);

        assertNotNull(result.getId());
        assertEquals(MovementType.IN, result.getMovementType());
        assertEquals(10, result.getPreviousQuantity());
        assertEquals(15, result.getNewQuantity());
    }

    @Test
    void shouldThrowException_whenInsufficientStockForOutMovement() {
        ProductRequest request = ProductRequest.builder()
                .name("Low Stock").sku("STK-OUT-001")
                .category("Test").price(BigDecimal.valueOf(30))
                .quantity(3).minStock(1).build();
        ProductResponse product = productService.create(request);

        StockMovementRequest movement = StockMovementRequest.builder()
                .productId(product.getId()).movementType(MovementType.OUT)
                .quantity(10).userId("tester").build();

        assertThrows(BusinessException.class, () -> stockMovementService.registerMovement(movement));
    }
}
