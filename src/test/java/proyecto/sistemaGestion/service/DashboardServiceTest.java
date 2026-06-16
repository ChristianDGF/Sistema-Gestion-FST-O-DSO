package proyecto.sistemaGestion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import proyecto.sistemaGestion.dto.DashboardResponse;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboard_shouldReturnMetrics() {
        Product activeProduct = Product.builder()
                .id(1L).name("Laptop").sku("LAP-001")
                .category("Electronics").price(BigDecimal.valueOf(1000))
                .quantity(10).minStock(5).status(ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).build();

        Product inactiveProduct = Product.builder()
                .id(2L).name("Tablet").sku("TAB-001")
                .category("Electronics").price(BigDecimal.valueOf(500))
                .quantity(2).minStock(5).status(ProductStatus.INACTIVE)
                .createdAt(LocalDateTime.now()).build();

        StockMovement movement = StockMovement.builder()
                .id(1L).product(activeProduct).movementType(MovementType.IN)
                .quantity(5).previousQuantity(5).newQuantity(10)
                .userId("user1").createdAt(LocalDateTime.now()).build();

        when(productRepository.findAll()).thenReturn(List.of(activeProduct, inactiveProduct));
        when(stockMovementRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(movement)));
        when(stockMovementRepository.count()).thenReturn(5L);

        DashboardResponse result = dashboardService.getDashboard();

        assertNotNull(result);
        assertEquals(2, result.getTotalProducts());
        assertEquals(1, result.getActiveProducts());
        assertEquals(1, result.getLowStockProducts());
        assertEquals(5, result.getTotalMovements());
        assertEquals(1, result.getCriticalProducts().size());
        assertEquals(1, result.getRecentMovements().size());
    }

    @Test
    void getDashboard_shouldReturnZeros_whenNoData() {
        when(productRepository.findAll()).thenReturn(List.of());
        when(stockMovementRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(stockMovementRepository.count()).thenReturn(0L);

        DashboardResponse result = dashboardService.getDashboard();

        assertNotNull(result);
        assertEquals(0, result.getTotalProducts());
        assertEquals(0, result.getActiveProducts());
        assertEquals(0, result.getLowStockProducts());
        assertEquals(0, result.getTotalMovements());
        assertTrue(result.getCriticalProducts().isEmpty());
        assertTrue(result.getRecentMovements().isEmpty());
    }
}
