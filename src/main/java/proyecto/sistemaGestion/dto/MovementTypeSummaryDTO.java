package proyecto.sistemaGestion.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovementTypeSummaryDTO {
    private String movementType;
    private long count;
    private long totalQuantity;
}
