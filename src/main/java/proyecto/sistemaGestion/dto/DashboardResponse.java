package proyecto.sistemaGestion.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardResponse {
    private long totalProducts;
    private long activeProducts;
    private long lowStockProducts;
    private long totalMovements;
    private List<ProductResponse> criticalProducts;
    private List<StockMovementResponse> recentMovements;
}
