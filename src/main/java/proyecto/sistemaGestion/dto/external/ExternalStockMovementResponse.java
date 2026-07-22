package proyecto.sistemaGestion.dto.external;

import lombok.*;
import proyecto.sistemaGestion.dto.StockMovementResponse;
import proyecto.sistemaGestion.enums.MovementType;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalStockMovementResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private MovementType movementType;
    private Integer quantity;
    private Integer previousQuantity;
    private Integer newQuantity;
    private LocalDateTime createdAt;

    public static ExternalStockMovementResponse from(StockMovementResponse movement) {
        return ExternalStockMovementResponse.builder()
                .id(movement.getId())
                .productId(movement.getProductId())
                .productName(movement.getProductName())
                .productSku(movement.getProductSku())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .previousQuantity(movement.getPreviousQuantity())
                .newQuantity(movement.getNewQuantity())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
