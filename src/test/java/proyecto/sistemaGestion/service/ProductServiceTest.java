package proyecto.sistemaGestion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.ProductRequest;
import proyecto.sistemaGestion.dto.ProductResponse;
import proyecto.sistemaGestion.entity.Product;
import proyecto.sistemaGestion.enums.ProductStatus;
import proyecto.sistemaGestion.exception.BusinessException;
import proyecto.sistemaGestion.exception.ResourceNotFoundException;
import proyecto.sistemaGestion.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product createProduct(Long id, String name, String sku, int quantity, int minStock) {
        return Product.builder()
                .id(id)
                .name(name)
                .sku(sku)
                .description("Description")
                .category("Electronics")
                .price(BigDecimal.valueOf(100))
                .quantity(quantity)
                .minStock(minStock)
                .status(ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ProductRequest createRequest(String name, String sku, int quantity, int minStock) {
        return ProductRequest.builder()
                .name(name)
                .sku(sku)
                .description("Description")
                .category("Electronics")
                .price(BigDecimal.valueOf(100))
                .quantity(quantity)
                .minStock(minStock)
                .build();
    }

    @Test
    void findAll_shouldReturnPagedResults() {
        Product product = createProduct(1L, "Laptop", "LAP-001", 10, 5);
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findByFilters(any(), any(), any(), any(), any())).thenReturn(page);

        PageResponse<ProductResponse> result = productService.findAll(0, 20, "name", "asc", null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Laptop", result.getContent().getFirst().getName());
        verify(productRepository).findByFilters(any(), any(), any(), any(), any());
    }

    @Test
    void findById_shouldReturnProduct_whenExists() {
        Product product = createProduct(1L, "Laptop", "LAP-001", 10, 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse result = productService.findById(1L);

        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        assertEquals("LAP-001", result.getSku());
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.findById(99L));
    }

    @Test
    void create_shouldSaveProduct_whenSkuIsUnique() {
        ProductRequest request = createRequest("Laptop", "LAP-001", 10, 5);
        Product savedProduct = createProduct(1L, "Laptop", "LAP-001", 10, 5);
        when(productRepository.existsBySku("LAP-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse result = productService.create(request);

        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_shouldThrowException_whenSkuExists() {
        ProductRequest request = createRequest("Laptop", "LAP-001", 10, 5);
        when(productRepository.existsBySku("LAP-001")).thenReturn(true);

        assertThrows(BusinessException.class, () -> productService.create(request));
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateProduct_whenValid() {
        Product existing = createProduct(1L, "Old Name", "OLD-001", 10, 5);
        ProductRequest request = createRequest("New Name", "OLD-001", 20, 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(existing);

        ProductResponse result = productService.update(1L, request);

        assertNotNull(result);
        assertEquals("New Name", result.getName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void update_shouldThrowException_whenSkuAlreadyTaken() {
        Product existing = createProduct(1L, "Laptop", "LAP-001", 10, 5);
        ProductRequest request = createRequest("Laptop", "LAP-002", 10, 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySku("LAP-002")).thenReturn(true);

        assertThrows(BusinessException.class, () -> productService.update(1L, request));
        verify(productRepository, never()).save(any());
    }

    @Test
    void delete_shouldDeleteProduct_whenExists() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        productService.delete(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> productService.delete(99L));
        verify(productRepository, never()).deleteById(any());
    }
}
