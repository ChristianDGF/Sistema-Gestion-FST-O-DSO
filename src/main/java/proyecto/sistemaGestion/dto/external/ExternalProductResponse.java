package proyecto.sistemaGestion.dto.external;

import lombok.*;
import proyecto.sistemaGestion.dto.ProductResponse;
import proyecto.sistemaGestion.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalProductResponse {

    private Long id;
    private String name;
    private String sku;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer quantity;
    private Integer minStock;
    private ProductStatus status;
    private boolean lowStock;
    private LocalDateTime updatedAt;

    public static ExternalProductResponse from(ProductResponse product) {
        return ExternalProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .minStock(product.getMinStock())
                .status(product.getStatus())
                .lowStock(product.isLowStock())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
