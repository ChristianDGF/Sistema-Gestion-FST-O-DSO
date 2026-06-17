package proyecto.sistemaGestion.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditRevisionDTO {

    private Integer revNumber;
    private LocalDateTime revTimestamp;
    private String entityType;
    private Long entityId;
    private String revType;

    private String productName;
    private String productSku;
    private String productCategory;
    private java.math.BigDecimal productPrice;
    private Integer productQuantity;
    private Integer productMinStock;
    private String productStatus;

    private Long movementProductId;
    private String movementProductName;
    private String movementType;
    private Integer movementQuantity;
    private Integer movementPreviousQuantity;
    private Integer movementNewQuantity;
    private String movementUserId;
    private String movementObservations;

    public static String mapRevType(short revtype) {
        return switch (revtype) {
            case 0 -> "ADD";
            case 1 -> "MOD";
            case 2 -> "DEL";
            default -> "UNKNOWN";
        };
    }

    public static LocalDateTime mapTimestamp(long epochMilli) {
        return Instant.ofEpochMilli(epochMilli)
                .atZone(ZoneId.of("America/Santo_Domingo"))
                .toLocalDateTime();
    }
}
