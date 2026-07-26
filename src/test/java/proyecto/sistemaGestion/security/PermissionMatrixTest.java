package proyecto.sistemaGestion.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import proyecto.sistemaGestion.dto.*;
import proyecto.sistemaGestion.enums.MovementType;
import proyecto.sistemaGestion.enums.ProductStatus;
import proyecto.sistemaGestion.service.AuditService;
import proyecto.sistemaGestion.service.DashboardService;
import proyecto.sistemaGestion.service.ProductService;
import proyecto.sistemaGestion.service.StockMovementService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Encodes the "Matriz minima de permisos" from the project spec as executable tests: every
 * critical endpoint must reject a caller who lacks the listed permission (403) and accept one
 * who holds exactly that permission. This is what caught AuditController requiring
 * report:view instead of audit:view.
 */
@SpringBootTest
@ActiveProfiles("test")
class PermissionMatrixTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;
    @MockitoBean
    private StockMovementService stockMovementService;
    @MockitoBean
    private DashboardService dashboardService;
    @MockitoBean
    private AuditService auditService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        ProductResponse product = ProductResponse.builder()
                .id(1L).name("Laptop").sku("LAP-001").description("Test")
                .category("Electronics").price(BigDecimal.valueOf(100))
                .quantity(10).minStock(5).status(ProductStatus.ACTIVE)
                .lowStock(false).createdAt(LocalDateTime.now()).build();
        StockMovementResponse movement = StockMovementResponse.builder()
                .id(1L).productId(1L).productName("Laptop").productSku("LAP-001")
                .movementType(MovementType.IN).quantity(5).previousQuantity(10)
                .newQuantity(15).userId("user1").createdAt(LocalDateTime.now()).build();

        lenient().when(productService.findAll(anyInt(), anyInt(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(PageResponse.<ProductResponse>builder().content(List.of(product))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());
        lenient().when(productService.findById(1L)).thenReturn(product);
        lenient().when(productService.create(any())).thenReturn(product);
        lenient().when(productService.update(any(), any())).thenReturn(product);
        lenient().when(productService.findLowStock(anyInt(), anyInt()))
                .thenReturn(PageResponse.<ProductResponse>builder().content(List.of(product))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());

        lenient().when(stockMovementService.registerMovement(any())).thenReturn(movement);
        lenient().when(stockMovementService.findByProductId(any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.<StockMovementResponse>builder().content(List.of(movement))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());
        lenient().when(stockMovementService.findAllByProductId(any())).thenReturn(List.of(movement));

        lenient().when(dashboardService.getDashboard()).thenReturn(DashboardResponse.builder()
                .totalProducts(1).activeProducts(1).lowStockProducts(0).totalMovements(1)
                .criticalProducts(List.of()).recentMovements(List.of()).build());

        lenient().when(auditService.getAuditStats()).thenReturn(AuditStatsDTO.builder()
                .totalProductRevisions(1L).totalMovementRevisions(1L).totalRevisions(2L)
                .productRevisionsByType(Map.of()).movementRevisionsByType(Map.of())
                .revisionsLast24h(0L).revisionsLast7d(0L).revisionsLast30d(0L).build());
        lenient().when(auditService.getProductRevisions(anyInt(), anyInt(), any())).thenReturn(List.of());
        lenient().when(auditService.countProductRevisions(any())).thenReturn(0L);
        lenient().when(auditService.getMovementRevisions(anyInt(), anyInt(), any())).thenReturn(List.of());
        lenient().when(auditService.countMovementRevisions(any())).thenReturn(0L);
    }

    private static final String PRODUCT_PAYLOAD = """
            {"name":"Test","sku":"TST-001","category":"Test","price":10,"quantity":1,"minStock":0}
            """;
    private static final String MOVEMENT_PAYLOAD = """
            {"productId":1,"movementType":"IN","quantity":5,"userId":"user1"}
            """;

    private static Stream<EndpointCase> endpoints() {
        return Stream.of(
                new EndpointCase("GET /products", () -> get("/api/v1/products"), "product:view", 200),
                new EndpointCase("GET /products/{id}", () -> get("/api/v1/products/1"), "product:view", 200),
                new EndpointCase("POST /products",
                        () -> post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(PRODUCT_PAYLOAD),
                        "product:manage", 201),
                new EndpointCase("PUT /products/{id}",
                        () -> put("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON).content(PRODUCT_PAYLOAD),
                        "product:manage", 200),
                new EndpointCase("DELETE /products/{id}", () -> delete("/api/v1/products/1"), "product:manage", 204),
                new EndpointCase("GET /products/low-stock", () -> get("/api/v1/products/low-stock"), "stock:view", 200),
                new EndpointCase("POST /stock-movements",
                        () -> post("/api/v1/stock-movements").contentType(MediaType.APPLICATION_JSON).content(MOVEMENT_PAYLOAD),
                        "stock:manage", 201),
                new EndpointCase("GET /stock-movements/product/{id}", () -> get("/api/v1/stock-movements/product/1"), "stock:view", 200),
                new EndpointCase("GET /stock-movements/product/{id}/all", () -> get("/api/v1/stock-movements/product/1/all"), "stock:view", 200),
                new EndpointCase("GET /dashboard", () -> get("/api/v1/dashboard"), "report:view", 200),
                new EndpointCase("GET /audit/stats", () -> get("/api/v1/audit/stats"), "audit:view", 200),
                new EndpointCase("GET /audit/products", () -> get("/api/v1/audit/products"), "audit:view", 200),
                new EndpointCase("GET /audit/movements", () -> get("/api/v1/audit/movements"), "audit:view", 200)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void endpointGrantsAccess_onlyWithRequiredPermission(EndpointCase testCase) throws Exception {
        mockMvc.perform(testCase.request.get()
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + testCase.requiredScope))))
                .andExpect(status().is(testCase.expectedStatus));

        mockMvc.perform(testCase.request.get()
                        .with(jwt().authorities()))
                .andExpect(status().isForbidden());
    }

    private record EndpointCase(String description, Supplier<MockHttpServletRequestBuilder> request,
                                 String requiredScope, int expectedStatus) {
        @Override
        public String toString() {
            return description + " requires SCOPE_" + requiredScope;
        }
    }
}
