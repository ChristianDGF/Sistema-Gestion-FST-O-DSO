package proyecto.sistemaGestion.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.sistemaGestion.dto.AuditRevisionDTO;
import proyecto.sistemaGestion.dto.AuditStatsDTO;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @InjectMocks
    private AuditService auditService;

    @Test
    void getProductRevisions_shouldReturnList() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        Object[] row = new Object[]{
                1L, 1, 1700000000000L, 0,
                "Laptop", "LAP-001", "Electronics", BigDecimal.valueOf(1000),
                10, 5, "ACTIVE"
        };
        when(query.getResultList()).thenReturn(Collections.singletonList(row));

        List<AuditRevisionDTO> result = auditService.getProductRevisions(0, 15, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        AuditRevisionDTO dto = result.getFirst();
        assertEquals(1L, dto.getEntityId());
        assertEquals("PRODUCT", dto.getEntityType());
        assertEquals("ADD", dto.getRevType());
        assertEquals("Laptop", dto.getProductName());
        assertEquals("LAP-001", dto.getProductSku());
        assertEquals("Electronics", dto.getProductCategory());
        assertEquals(BigDecimal.valueOf(1000), dto.getProductPrice());
        assertEquals(10, dto.getProductQuantity());
        assertEquals(5, dto.getProductMinStock());
        assertEquals("ACTIVE", dto.getProductStatus());
        assertNotNull(dto.getRevTimestamp());
    }

    @Test
    void getProductRevisions_shouldReturnEmptyList_whenNoData() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        List<AuditRevisionDTO> result = auditService.getProductRevisions(0, 15, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getProductRevisions_shouldFilterByEntityId() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        Object[] row = new Object[]{
                1L, 1, 1700000000000L, 1,
                "Laptop", "LAP-001", "Electronics", BigDecimal.valueOf(1000),
                10, 5, "ACTIVE"
        };
        when(query.getResultList()).thenReturn(Collections.singletonList(row));

        List<AuditRevisionDTO> result = auditService.getProductRevisions(0, 15, 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getEntityId());
        assertEquals("MOD", result.getFirst().getRevType());
    }

    @Test
    void countProductRevisions_shouldReturnCount() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(5L);

        long count = auditService.countProductRevisions(null);

        assertEquals(5L, count);
    }

    @Test
    void getMovementRevisions_shouldReturnList() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        Object[] row = new Object[]{
                1L, 1, 1700000000000L, 0,
                1L, "Laptop",
                "IN", 5,
                10, 15,
                "user1", "Restock"
        };
        when(query.getResultList()).thenReturn(Collections.singletonList(row));

        List<AuditRevisionDTO> result = auditService.getMovementRevisions(0, 15, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        AuditRevisionDTO dto = result.getFirst();
        assertEquals("MOVEMENT", dto.getEntityType());
        assertEquals("ADD", dto.getRevType());
        assertEquals(1L, dto.getMovementProductId());
        assertEquals("Laptop", dto.getMovementProductName());
        assertEquals("IN", dto.getMovementType());
        assertEquals(5, dto.getMovementQuantity());
        assertEquals(10, dto.getMovementPreviousQuantity());
        assertEquals(15, dto.getMovementNewQuantity());
        assertEquals("user1", dto.getMovementUserId());
        assertEquals("Restock", dto.getMovementObservations());
        assertNotNull(dto.getRevTimestamp());
    }

    @Test
    void getMovementRevisions_shouldReturnEmptyList_whenNoData() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        List<AuditRevisionDTO> result = auditService.getMovementRevisions(0, 15, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getMovementRevisions_shouldFilterByEntityId() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        Object[] row = new Object[]{
                1L, 1, 1700000000000L, 2,
                1L, "Laptop",
                "OUT", 3,
                10, 7,
                "user1", "Sale"
        };
        when(query.getResultList()).thenReturn(Collections.singletonList(row));

        List<AuditRevisionDTO> result = auditService.getMovementRevisions(0, 15, 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("DEL", result.getFirst().getRevType());
        assertEquals("OUT", result.getFirst().getMovementType());
    }

    @Test
    void countMovementRevisions_shouldReturnCount() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(3L);

        long count = auditService.countMovementRevisions(null);

        assertEquals(3L, count);
    }

    @Test
    void getAuditStats_shouldReturnStats() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        Object[] productRow = new Object[]{0, 5L};
        Object[] movementRow = new Object[]{1, 3L};
        when(query.getResultList())
                .thenReturn(Collections.singletonList(productRow))
                .thenReturn(Collections.singletonList(movementRow));

        when(query.setParameter(eq("ts"), anyLong())).thenReturn(query);
        when(query.getSingleResult())
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(3L);

        AuditStatsDTO stats = auditService.getAuditStats();

        assertNotNull(stats);
        assertEquals(5L, stats.getTotalProductRevisions());
        assertEquals(3L, stats.getTotalMovementRevisions());
        assertEquals(8L, stats.getTotalRevisions());
        assertEquals(1L, stats.getRevisionsLast24h());
        assertEquals(2L, stats.getRevisionsLast7d());
        assertEquals(3L, stats.getRevisionsLast30d());
        assertNotNull(stats.getProductRevisionsByType());
        assertNotNull(stats.getMovementRevisionsByType());
    }

    @Test
    void getAuditStats_shouldReturnZeros_whenNoData() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        when(query.setParameter(eq("ts"), anyLong())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(0L);

        AuditStatsDTO stats = auditService.getAuditStats();

        assertNotNull(stats);
        assertEquals(0L, stats.getTotalProductRevisions());
        assertEquals(0L, stats.getTotalMovementRevisions());
        assertEquals(0L, stats.getTotalRevisions());
        assertEquals(0L, stats.getRevisionsLast24h());
        assertEquals(0L, stats.getRevisionsLast7d());
        assertEquals(0L, stats.getRevisionsLast30d());
    }

    @Test
    void getAuditStats_shouldReturnStats_withAllRevTypes() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        Object[] addRow = new Object[]{0, 5L};
        Object[] modRow = new Object[]{1, 3L};
        Object[] delRow = new Object[]{2, 1L};
        when(query.getResultList())
                .thenReturn(List.of(addRow, modRow, delRow))
                .thenReturn(Collections.singletonList(addRow));

        when(query.setParameter(eq("ts"), anyLong())).thenReturn(query);
        when(query.getSingleResult())
                .thenReturn(1L)
                .thenReturn(5L)
                .thenReturn(9L);

        AuditStatsDTO stats = auditService.getAuditStats();

        assertNotNull(stats);
        assertEquals(9L, stats.getTotalProductRevisions());
        assertEquals(5L, stats.getTotalMovementRevisions());
        assertEquals(14L, stats.getTotalRevisions());
        assertEquals(1L, stats.getRevisionsLast24h());
        assertEquals(5L, stats.getRevisionsLast7d());
        assertEquals(9L, stats.getRevisionsLast30d());
    }
}
