package proyecto.sistemaGestion.dto;

import lombok.*;
import proyecto.sistemaGestion.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductResponse {

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
