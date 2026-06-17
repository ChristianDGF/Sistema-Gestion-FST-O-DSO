package proyecto.sistemaGestion.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.sistemaGestion.dto.AuditRevisionDTO;
import proyecto.sistemaGestion.dto.AuditStatsDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final EntityManager entityManager;

    private static final ZoneId ZONE = ZoneId.of("America/Santo_Domingo");

    private static LocalDateTime epochToLDT(Object epochMilli) {
        if (epochMilli == null) return null;
        return Instant.ofEpochMilli(((Number) epochMilli).longValue())
                .atZone(ZONE).toLocalDateTime();
    }

    private static String mapRevType(Object revtype) {
        if (revtype == null) return "UNKNOWN";
        int v = ((Number) revtype).intValue();
        return switch (v) {
            case 0 -> "ADD";
            case 1 -> "MOD";
            case 2 -> "DEL";
            default -> "UNKNOWN";
        };
    }


    @Transactional(readOnly = true)
    public List<AuditRevisionDTO> getProductRevisions(int page, int size, Long entityId) {
        String sql = """
            SELECT pa.id, ri.rev, ri.revtstmp, pa.revtype,
                   pa.name, pa.sku, pa.category, pa.price,
                   pa.quantity, pa.min_stock, pa.status
            FROM products_aud pa
            JOIN revinfo ri ON pa.rev = ri.rev
            WHERE (CAST(:entityId AS BIGINT) IS NULL OR pa.id = CAST(:entityId AS BIGINT))
            ORDER BY ri.rev DESC
            LIMIT :size OFFSET :offset
            """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("entityId", entityId);
        q.setParameter("size", size);
        q.setParameter("offset", (long) page * size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        List<AuditRevisionDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(AuditRevisionDTO.builder()
                    .entityId(row[0] != null ? ((Number) row[0]).longValue() : null)
                    .revNumber(row[1] != null ? ((Number) row[1]).intValue() : null)
                    .revTimestamp(epochToLDT(row[2]))
                    .entityType("PRODUCT")
                    .revType(mapRevType(row[3]))
                    .productName(row[4] != null ? row[4].toString() : null)
                    .productSku(row[5] != null ? row[5].toString() : null)
                    .productCategory(row[6] != null ? row[6].toString() : null)
                    .productPrice(row[7] != null ? new BigDecimal(row[7].toString()) : null)
                    .productQuantity(row[8] != null ? ((Number) row[8]).intValue() : null)
                    .productMinStock(row[9] != null ? ((Number) row[9]).intValue() : null)
                    .productStatus(row[10] != null ? row[10].toString() : null)
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public long countProductRevisions(Long entityId) {
        String sql = """
            SELECT COUNT(*) FROM products_aud pa
            JOIN revinfo ri ON pa.rev = ri.rev
            WHERE (CAST(:entityId AS BIGINT) IS NULL OR pa.id = CAST(:entityId AS BIGINT))
            """;
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("entityId", entityId);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Transactional(readOnly = true)
    public List<AuditRevisionDTO> getMovementRevisions(int page, int size, Long entityId) {
        String sql = """
            SELECT sma.id, ri.rev, ri.revtstmp, sma.revtype,
                   sma.product_id, p.name,
                   sma.movement_type, sma.quantity,
                   sma.previous_quantity, sma.new_quantity,
                   sma.user_id, sma.observations
            FROM stock_movements_aud sma
            JOIN revinfo ri ON sma.rev = ri.rev
            LEFT JOIN products p ON sma.product_id = p.id
            WHERE (CAST(:entityId AS BIGINT) IS NULL OR sma.id = CAST(:entityId AS BIGINT))
            ORDER BY ri.rev DESC
            LIMIT :size OFFSET :offset
            """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("entityId", entityId);
        q.setParameter("size", size);
        q.setParameter("offset", (long) page * size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        List<AuditRevisionDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(AuditRevisionDTO.builder()
                    .entityId(row[0] != null ? ((Number) row[0]).longValue() : null)
                    .revNumber(row[1] != null ? ((Number) row[1]).intValue() : null)
                    .revTimestamp(epochToLDT(row[2]))
                    .entityType("MOVEMENT")
                    .revType(mapRevType(row[3]))
                    .movementProductId(row[4] != null ? ((Number) row[4]).longValue() : null)
                    .movementProductName(row[5] != null ? row[5].toString() : null)
                    .movementType(row[6] != null ? row[6].toString() : null)
                    .movementQuantity(row[7] != null ? ((Number) row[7]).intValue() : null)
                    .movementPreviousQuantity(row[8] != null ? ((Number) row[8]).intValue() : null)
                    .movementNewQuantity(row[9] != null ? ((Number) row[9]).intValue() : null)
                    .movementUserId(row[10] != null ? row[10].toString() : null)
                    .movementObservations(row[11] != null ? row[11].toString() : null)
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public long countMovementRevisions(Long entityId) {
        String sql = """
            SELECT COUNT(*) FROM stock_movements_aud sma
            JOIN revinfo ri ON sma.rev = ri.rev
            WHERE (CAST(:entityId AS BIGINT) IS NULL OR sma.id = CAST(:entityId AS BIGINT))
            """;
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("entityId", entityId);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Transactional(readOnly = true)
    public AuditStatsDTO getAuditStats() {

        Map<String, Long> productByType = countByType("products_aud");

        Map<String, Long> movementByType = countByType("stock_movements_aud");

        long totalProduct  = productByType.values().stream().mapToLong(Long::longValue).sum();
        long totalMovement = movementByType.values().stream().mapToLong(Long::longValue).sum();

        long now  = Instant.now().toEpochMilli();
        long h24  = now - 24L  * 3600 * 1000;
        long d7   = now - 7L   * 86400 * 1000;
        long d30  = now - 30L  * 86400 * 1000;

        long rev24h = countRevisionsAfter(h24);
        long rev7d  = countRevisionsAfter(d7);
        long rev30d = countRevisionsAfter(d30);

        return AuditStatsDTO.builder()
                .totalProductRevisions(totalProduct)
                .totalMovementRevisions(totalMovement)
                .totalRevisions(totalProduct + totalMovement)
                .productRevisionsByType(productByType)
                .movementRevisionsByType(movementByType)
                .revisionsLast24h(rev24h)
                .revisionsLast7d(rev7d)
                .revisionsLast30d(rev30d)
                .build();
    }

    private Map<String, Long> countByType(String table) {
        String sql = "SELECT revtype, COUNT(*) FROM " + table + " GROUP BY revtype";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();

        Map<String, Long> map = new LinkedHashMap<>();
        map.put("ADD", 0L);
        map.put("MOD", 0L);
        map.put("DEL", 0L);
        for (Object[] row : rows) {
            map.put(mapRevType(row[0]), ((Number) row[1]).longValue());
        }
        return map;
    }

    private long countRevisionsAfter(long epochMilli) {
        String sql = """
            SELECT COUNT(*) FROM revinfo
            WHERE revtstmp >= :ts
            """;
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("ts", epochMilli);
        return ((Number) q.getSingleResult()).longValue();
    }
}
