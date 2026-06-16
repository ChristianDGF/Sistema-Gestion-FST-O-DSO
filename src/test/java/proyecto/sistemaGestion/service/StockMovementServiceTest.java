package proyecto.sistemaGestion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import proyecto.sistemaGestion.dto.StockMovementRequest;
import proyecto.sistemaGestion.dto.StockMovementResponse;
import proyecto.sistemaGestion.entity.Product;
import proyecto.sistemaGestion.entity.StockMovement;
import proyecto.sistemaGestion.enums.MovementType;
import proyecto.sistemaGestion.enums.ProductStatus;
import proyecto.sistemaGestion.exception.BusinessException;
import proyecto.sistemaGestion.exception.ResourceNotFoundException;
import proyecto.sistemaGestion.repository.ProductRepository;
import proyecto.sistemaGestion.repository.StockMovementRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockMovementService stockMovementService;

    private Product createProduct(Long id, int quantity) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .sku("SKU-" + id)
                .description("Description")
                .category("Category")
                .price(BigDecimal.valueOf(50))
                .quantity(quantity)
                .minStock(5)
                .status(ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void registerMovement_shouldIncreaseStockOnIn() {
        Product product = createProduct(1L, 10);
        StockMovementRequest request = StockMovementRequest.builder()
                .productId(1L).movementType(MovementType.IN).quantity(5)
                .userId("user1").observations("Restock").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponse result = stockMovementService.registerMovement(request);

        assertNotNull(result);
        assertEquals(15, result.getNewQuantity());
        assertEquals(10, result.getPreviousQuantity());
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    void registerMovement_shouldDecreaseStockOnOut() {
        Product product = createProduct(1L, 10);
        StockMovementRequest request = StockMovementRequest.builder()
                .productId(1L).movementType(MovementType.OUT).quantity(3)
                .userId("user1").observations("Sale").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponse result = stockMovementService.registerMovement(request);

        assertEquals(7, result.getNewQuantity());
        assertEquals(10, result.getPreviousQuantity());
    }

    @Test
    void registerMovement_shouldThrowException_whenInsufficientStock() {
        Product product = createProduct(1L, 2);
        StockMovementRequest request = StockMovementRequest.builder()
                .productId(1L).movementType(MovementType.OUT).quantity(5)
                .userId("user1").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(BusinessException.class, () -> stockMovementService.registerMovement(request));
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void registerMovement_shouldSetExactQuantityOnAdjustment() {
        Product product = createProduct(1L, 10);
        StockMovementRequest request = StockMovementRequest.builder()
                .productId(1L).movementType(MovementType.ADJUSTMENT).quantity(25)
                .userId("user1").observations("Inventory count").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponse result = stockMovementService.registerMovement(request);

        assertEquals(25, result.getNewQuantity());
        assertEquals(10, result.getPreviousQuantity());
    }

    @Test
    void registerMovement_shouldThrowException_onNegativeAdjustment() {
        Product product = createProduct(1L, 10);
        StockMovementRequest request = StockMovementRequest.builder()
                .productId(1L).movementType(MovementType.ADJUSTMENT).quantity(-5)
                .userId("user1").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(BusinessException.class, () -> stockMovementService.registerMovement(request));
    }

    @Test
    void registerMovement_shouldThrowException_whenProductNotFound() {
        StockMovementRequest request = StockMovementRequest.builder()
                .productId(99L).movementType(MovementType.IN).quantity(5)
                .userId("user1").build();

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> stockMovementService.registerMovement(request));
    }

    @Test
    void findByProductId_shouldReturnMovements_whenProductExists() {
        Product product = createProduct(1L, 10);
        StockMovement movement = StockMovement.builder()
                .id(1L).product(product).movementType(MovementType.IN)
                .quantity(5).previousQuantity(10).newQuantity(15)
                .userId("user1").createdAt(LocalDateTime.now()).build();

        when(productRepository.existsById(1L)).thenReturn(true);
        when(stockMovementRepository.findByProductIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movement)));

        var result = stockMovementService.findByProductId(1L, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(MovementType.IN, result.getContent().getFirst().getMovementType());
    }
}
