package proyecto.sistemaGestion.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryValuationDTO {
    private String category;
    private long productCount;
    private int totalQuantity;
    private BigDecimal totalValue;
}
