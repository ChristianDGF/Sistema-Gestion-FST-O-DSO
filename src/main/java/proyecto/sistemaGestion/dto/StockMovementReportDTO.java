package proyecto.sistemaGestion.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovementReportDTO {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private long totalMovements;
    private List<MovementTypeSummaryDTO> byType;
    private List<StockMovementResponse> movements;
}
