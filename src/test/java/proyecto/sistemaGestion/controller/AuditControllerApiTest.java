package proyecto.sistemaGestion.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import proyecto.sistemaGestion.dto.AuditRevisionDTO;
import proyecto.sistemaGestion.dto.AuditStatsDTO;
import proyecto.sistemaGestion.service.AuditService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AuditControllerApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private AuditStatsDTO createStats() {
        return AuditStatsDTO.builder()
                .totalProductRevisions(10L)
                .totalMovementRevisions(5L)
                .totalRevisions(15L)
                .productRevisionsByType(Map.of("ADD", 5L, "MOD", 3L, "DEL", 2L))
                .movementRevisionsByType(Map.of("ADD", 3L, "MOD", 1L, "DEL", 1L))
                .revisionsLast24h(2L)
                .revisionsLast7d(8L)
                .revisionsLast30d(12L)
                .build();
    }

    private AuditRevisionDTO createProductRevision(Long id) {
        return AuditRevisionDTO.builder()
                .revNumber(1)
                .revTimestamp(LocalDateTime.now())
                .entityType("PRODUCT")
                .entityId(id)
                .revType("ADD")
                .productName("Laptop")
                .productSku("LAP-001")
                .productCategory("Electronics")
                .productPrice(BigDecimal.valueOf(1000))
                .productQuantity(10)
                .productMinStock(5)
                .productStatus("ACTIVE")
                .build();
    }

    private AuditRevisionDTO createMovementRevision(Long id) {
        return AuditRevisionDTO.builder()
                .revNumber(1)
                .revTimestamp(LocalDateTime.now())
                .entityType("MOVEMENT")
                .entityId(id)
                .revType("ADD")
                .movementProductId(1L)
                .movementProductName("Laptop")
                .movementType("IN")
                .movementQuantity(5)
                .movementPreviousQuantity(10)
                .movementNewQuantity(15)
                .movementUserId("user1")
                .movementObservations("Restock")
                .build();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void getStats_shouldReturn200() throws Exception {
        when(auditService.getAuditStats()).thenReturn(createStats());

        mockMvc.perform(get("/api/v1/audit/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductRevisions").value(10))
                .andExpect(jsonPath("$.totalMovementRevisions").value(5))
                .andExpect(jsonPath("$.totalRevisions").value(15))
                .andExpect(jsonPath("$.revisionsLast24h").value(2))
                .andExpect(jsonPath("$.revisionsLast7d").value(8))
                .andExpect(jsonPath("$.revisionsLast30d").value(12));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void getProductRevisions_shouldReturn200() throws Exception {
        when(auditService.getProductRevisions(anyInt(), anyInt(), isNull()))
                .thenReturn(List.of(createProductRevision(1L)));
        when(auditService.countProductRevisions(isNull())).thenReturn(1L);

        mockMvc.perform(get("/api/v1/audit/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.content[0].productSku").value("LAP-001"))
                .andExpect(jsonPath("$.content[0].entityType").value("PRODUCT"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(15))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void getProductRevisions_shouldReturn200_withEntityId() throws Exception {
        when(auditService.getProductRevisions(anyInt(), anyInt(), eq(1L)))
                .thenReturn(List.of(createProductRevision(1L)));
        when(auditService.countProductRevisions(eq(1L))).thenReturn(1L);

        mockMvc.perform(get("/api/v1/audit/products?entityId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void getProductRevisions_shouldReturn200_whenEmpty() throws Exception {
        when(auditService.getProductRevisions(anyInt(), anyInt(), isNull()))
                .thenReturn(List.of());
        when(auditService.countProductRevisions(isNull())).thenReturn(0L);

        mockMvc.perform(get("/api/v1/audit/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void getMovementRevisions_shouldReturn200() throws Exception {
        when(auditService.getMovementRevisions(anyInt(), anyInt(), isNull()))
                .thenReturn(List.of(createMovementRevision(1L)));
        when(auditService.countMovementRevisions(isNull())).thenReturn(1L);

        mockMvc.perform(get("/api/v1/audit/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].movementType").value("IN"))
                .andExpect(jsonPath("$.content[0].entityType").value("MOVEMENT"))
                .andExpect(jsonPath("$.content[0].movementProductName").value("Laptop"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(15))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void getMovementRevisions_shouldReturn200_withEntityId() throws Exception {
        when(auditService.getMovementRevisions(anyInt(), anyInt(), eq(1L)))
                .thenReturn(List.of(createMovementRevision(1L)));
        when(auditService.countMovementRevisions(eq(1L))).thenReturn(1L);

        mockMvc.perform(get("/api/v1/audit/movements?entityId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].movementType").value("IN"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void getMovementRevisions_shouldReturn200_whenEmpty() throws Exception {
        when(auditService.getMovementRevisions(anyInt(), anyInt(), isNull()))
                .thenReturn(List.of());
        when(auditService.countMovementRevisions(isNull())).thenReturn(0L);

        mockMvc.perform(get("/api/v1/audit/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:view")
    void getStats_shouldReturn403_whenNoPermission() throws Exception {
        mockMvc.perform(get("/api/v1/audit/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:view")
    void getProductRevisions_shouldReturn403_whenNoPermission() throws Exception {
        mockMvc.perform(get("/api/v1/audit/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:view")
    void getMovementRevisions_shouldReturn403_whenNoPermission() throws Exception {
        mockMvc.perform(get("/api/v1/audit/movements"))
                .andExpect(status().isForbidden());
    }
}
