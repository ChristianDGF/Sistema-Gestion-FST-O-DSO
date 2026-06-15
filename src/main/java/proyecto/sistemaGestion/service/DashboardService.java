package proyecto.sistemaGestion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.sistemaGestion.dto.DashboardResponse;
import proyecto.sistemaGestion.dto.ProductResponse;
import proyecto.sistemaGestion.dto.StockMovementResponse;
import proyecto.sistemaGestion.entity.Product;
import proyecto.sistemaGestion.entity.StockMovement;
import proyecto.sistemaGestion.enums.ProductStatus;
import proyecto.sistemaGestion.repository.ProductRepository;
import proyecto.sistemaGestion.repository.StockMovementRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        List<Product> allProducts = productRepository.findAll();
        List<Product> lowStock = allProducts.stream()
                .filter(p -> p.getQuantity() <= p.getMinStock())
                .toList();
        List<Product> critical = lowStock.stream()
                .sorted((a, b) -> Integer.compare(a.getQuantity(), b.getQuantity()))
                .limit(10)
                .toList();

        List<StockMovement> recent = stockMovementRepository.findAll(
                PageRequest.of(0, 10)).stream().toList();

        return DashboardResponse.builder()
                .totalProducts(allProducts.size())
                .activeProducts(allProducts.stream()
                        .filter(p -> p.getStatus() == ProductStatus.ACTIVE).count())
                .lowStockProducts(lowStock.size())
                .totalMovements(stockMovementRepository.count())
                .criticalProducts(critical.stream().map(this::toProductResponse).toList())
                .recentMovements(recent.stream().map(this::toMovementResponse).toList())
                .build();
    }

    private ProductResponse toProductResponse(Product product) {
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

    private StockMovementResponse toMovementResponse(StockMovement movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .productId(movement.getProduct().getId())
                .productName(movement.getProduct().getName())
                .productSku(movement.getProduct().getSku())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .previousQuantity(movement.getPreviousQuantity())
                .newQuantity(movement.getNewQuantity())
                .userId(movement.getUserId())
                .observations(movement.getObservations())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
