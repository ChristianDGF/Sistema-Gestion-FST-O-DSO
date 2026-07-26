package proyecto.sistemaGestion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.sistemaGestion.dto.InventoryValuationReportDTO;
import proyecto.sistemaGestion.dto.StockMovementReportDTO;
import proyecto.sistemaGestion.entity.Product;
import proyecto.sistemaGestion.entity.StockMovement;
import proyecto.sistemaGestion.enums.MovementType;
import proyecto.sistemaGestion.enums.ProductStatus;
import proyecto.sistemaGestion.repository.ProductRepository;
import proyecto.sistemaGestion.repository.StockMovementRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private ReportService reportService;

    private Product product(String category, BigDecimal price, int quantity, int minStock, ProductStatus status) {
        return Product.builder()
                .id(1L).name("Producto").sku("SKU-" + category)
                .category(category).price(price).quantity(quantity).minStock(minStock)
                .status(status).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void getInventoryValuationReport_aggregatesByCategory() {
        Product laptop = product("Electronics", BigDecimal.valueOf(1000), 5, 2, ProductStatus.ACTIVE);
        Product mouse = product("Electronics", BigDecimal.valueOf(20), 1, 5, ProductStatus.ACTIVE); // low stock
        Product chair = product("Furniture", BigDecimal.valueOf(150), 3, 1, ProductStatus.INACTIVE);

        when(productRepository.findAll()).thenReturn(List.of(laptop, mouse, chair));

        InventoryValuationReportDTO report = reportService.getInventoryValuationReport();

        assertEquals(3, report.getTotalProducts());
        assertEquals(2, report.getActiveProducts());
        assertEquals(1, report.getInactiveProducts());
        assertEquals(1, report.getLowStockCount());
        // 1000*5 + 20*1 + 150*3 = 5000 + 20 + 450 = 5470
        assertEquals(0, BigDecimal.valueOf(5470).compareTo(report.getTotalInventoryValue()));
        assertEquals(2, report.getByCategory().size());
        var electronics = report.getByCategory().stream()
                .filter(c -> c.getCategory().equals("Electronics")).findFirst().orElseThrow();
        assertEquals(2, electronics.getProductCount());
        assertEquals(0, BigDecimal.valueOf(5020).compareTo(electronics.getTotalValue()));
    }

    @Test
    void getInventoryValuationReport_returnsZeros_whenNoProducts() {
        when(productRepository.findAll()).thenReturn(List.of());

        InventoryValuationReportDTO report = reportService.getInventoryValuationReport();

        assertEquals(0, report.getTotalProducts());
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getTotalInventoryValue()));
        assertTrue(report.getByCategory().isEmpty());
    }

    @Test
    void getStockMovementReport_aggregatesByType() {
        Product product = product("Electronics", BigDecimal.TEN, 10, 2, ProductStatus.ACTIVE);
        StockMovement in1 = StockMovement.builder().id(1L).product(product).movementType(MovementType.IN)
                .quantity(5).previousQuantity(0).newQuantity(5).userId("admin").createdAt(LocalDateTime.now()).build();
        StockMovement in2 = StockMovement.builder().id(2L).product(product).movementType(MovementType.IN)
                .quantity(3).previousQuantity(5).newQuantity(8).userId("admin").createdAt(LocalDateTime.now()).build();
        StockMovement out1 = StockMovement.builder().id(3L).product(product).movementType(MovementType.OUT)
                .quantity(2).previousQuantity(8).newQuantity(6).userId("admin").createdAt(LocalDateTime.now()).build();

        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        when(stockMovementRepository.findByDateRange(eq(start), eq(end), any(), any()))
                .thenReturn(List.of(in1, in2, out1));

        StockMovementReportDTO report = reportService.getStockMovementReport(start, end, null, null);

        assertEquals(3, report.getTotalMovements());
        assertEquals(2, report.getByType().size());
        var inSummary = report.getByType().stream().filter(t -> t.getMovementType().equals("IN")).findFirst().orElseThrow();
        assertEquals(2, inSummary.getCount());
        assertEquals(8, inSummary.getTotalQuantity());
        assertEquals(3, report.getMovements().size());
    }

    @Test
    void toInventoryValuationCsv_containsHeaderAndTotalRow() {
        when(productRepository.findAll()).thenReturn(List.of(
                product("Electronics", BigDecimal.valueOf(100), 2, 1, ProductStatus.ACTIVE)));

        InventoryValuationReportDTO report = reportService.getInventoryValuationReport();
        String csv = reportService.toInventoryValuationCsv(report);

        assertTrue(csv.startsWith("Categoria,Cantidad de Productos,Cantidad Total en Stock,Valor Total\n"));
        assertTrue(csv.contains("Electronics"));
        assertTrue(csv.contains("TOTAL,1"));
    }

    @Test
    void toStockMovementCsv_escapesCommasInObservations() {
        Product product = product("Electronics", BigDecimal.TEN, 10, 2, ProductStatus.ACTIVE);
        StockMovement movement = StockMovement.builder().id(1L).product(product).movementType(MovementType.IN)
                .quantity(1).previousQuantity(0).newQuantity(1).userId("admin")
                .observations("nota, con coma").createdAt(LocalDateTime.now()).build();

        when(stockMovementRepository.findByDateRange(any(), any(), any(), any())).thenReturn(List.of(movement));

        StockMovementReportDTO report = reportService.getStockMovementReport(
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), null, null);
        String csv = reportService.toStockMovementCsv(report);

        assertTrue(csv.contains("\"nota, con coma\""));
    }
}
