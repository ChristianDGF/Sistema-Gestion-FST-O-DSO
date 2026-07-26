package proyecto.sistemaGestion.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.ProductRequest;
import proyecto.sistemaGestion.dto.ProductResponse;
import proyecto.sistemaGestion.entity.Product;
import proyecto.sistemaGestion.enums.ProductStatus;
import proyecto.sistemaGestion.exception.BusinessException;
import proyecto.sistemaGestion.exception.ResourceNotFoundException;
import proyecto.sistemaGestion.repository.ProductRepository;
import proyecto.sistemaGestion.repository.StockMovementRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final MeterRegistry meterRegistry;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(int page, int size, String sortBy, String sortDir,
                                                  String search, String category, ProductStatus status,
                                                  Boolean lowStock) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String normalizedSearch = (search == null || search.isBlank()) ? "" : search.trim();
        Page<Product> productPage = productRepository.findByFilters(normalizedSearch, category, status, lowStock, pageable);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<ProductResponse>builder()
                .content(content)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("Ya existe un producto con el SKU: " + request.getSku());
        }

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .minStock(request.getMinStock())
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE)
                .build();

        product = productRepository.save(product);
        meterRegistry.counter("inventory.products.creations").increment();
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("Ya existe un producto con el SKU: " + request.getSku());
        }

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setMinStock(request.getMinStock());
        product.setStatus(request.getStatus() != null ? request.getStatus() : product.getStatus());

        product = productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
        }
        if (stockMovementRepository.existsByProductId(id)) {
            throw new BusinessException("No se puede eliminar el producto porque tiene movimientos de stock asociados");
        }
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findLowStock(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Product> productPage = productRepository.findLowStockProducts(pageable);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<ProductResponse>builder()
                .content(content)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .minStock(product.getMinStock())
                .status(product.getStatus())
                .lowStock(product.getQuantity() <= product.getMinStock())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
