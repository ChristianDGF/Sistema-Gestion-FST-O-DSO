package proyecto.sistemaGestion.dto;

import lombok.*;

import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditStatsDTO {

    private long totalProductRevisions;
    private long totalMovementRevisions;
    private long totalRevisions;

    private Map<String, Long> productRevisionsByType;
    private Map<String, Long> movementRevisionsByType;

    private long revisionsLast24h;
    private long revisionsLast7d;
    private long revisionsLast30d;
}
