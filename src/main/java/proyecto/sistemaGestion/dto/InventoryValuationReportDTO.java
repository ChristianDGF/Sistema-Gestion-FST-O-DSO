package proyecto.sistemaGestion.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryValuationReportDTO {
    private long totalProducts;
    private long activeProducts;
    private long inactiveProducts;
    private long lowStockCount;
    private BigDecimal totalInventoryValue;
    private List<CategoryValuationDTO> byCategory;
}
