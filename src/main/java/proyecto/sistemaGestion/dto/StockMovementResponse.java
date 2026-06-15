package proyecto.sistemaGestion.dto;

import lombok.*;
import proyecto.sistemaGestion.enums.MovementType;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovementResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private MovementType movementType;
    private Integer quantity;
    private Integer previousQuantity;
    private Integer newQuantity;
    private String userId;
    private String observations;
    private LocalDateTime createdAt;
}
