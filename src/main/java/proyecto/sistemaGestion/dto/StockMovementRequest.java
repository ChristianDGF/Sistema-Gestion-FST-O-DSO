package proyecto.sistemaGestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import proyecto.sistemaGestion.enums.MovementType;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovementRequest {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productId;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private MovementType movementType;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor que cero")
    private Integer quantity;

    @NotBlank(message = "El ID del usuario es obligatorio")
    private String userId;

    private String observations;
}
