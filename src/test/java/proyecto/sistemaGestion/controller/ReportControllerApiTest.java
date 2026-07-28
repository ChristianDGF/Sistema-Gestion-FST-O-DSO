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
import proyecto.sistemaGestion.dto.InventoryValuationReportDTO;
import proyecto.sistemaGestion.dto.StockMovementReportDTO;
import proyecto.sistemaGestion.service.ReportService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ReportControllerApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private InventoryValuationReportDTO valuationReport() {
        return InventoryValuationReportDTO.builder()
                .totalProducts(3).activeProducts(2).inactiveProducts(1).lowStockCount(1)
                .totalInventoryValue(BigDecimal.valueOf(5470)).byCategory(List.of()).build();
    }

    private StockMovementReportDTO movementReport() {
        return StockMovementReportDTO.builder()
                .totalMovements(3).byType(List.of()).movements(List.of()).build();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void getInventoryValuation_shouldReturn200() throws Exception {
        when(reportService.getInventoryValuationReport()).thenReturn(valuationReport());

        mockMvc.perform(get("/api/v1/reports/inventory-valuation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(3));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_stock:view")
    void getInventoryValuation_shouldReturn403_whenInsufficientPermission() throws Exception {
        mockMvc.perform(get("/api/v1/reports/inventory-valuation"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void exportInventoryValuation_shouldReturnCsv() throws Exception {
        when(reportService.getInventoryValuationReport()).thenReturn(valuationReport());
        when(reportService.toInventoryValuationCsv(any())).thenReturn("Categoria,Total\nElectronics,100\n");

        mockMvc.perform(get("/api/v1/reports/inventory-valuation/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("valuacion-inventario.csv")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Electronics")));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void getStockMovements_shouldReturn200() throws Exception {
        when(reportService.getStockMovementReport(any(), any(), any(), any())).thenReturn(movementReport());

        mockMvc.perform(get("/api/v1/reports/stock-movements")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMovements").value(3));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void getStockMovements_shouldReturn400_whenDatesMissing() throws Exception {
        mockMvc.perform(get("/api/v1/reports/stock-movements"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void exportStockMovements_shouldReturnCsv() throws Exception {
        when(reportService.getStockMovementReport(any(), any(), any(), any())).thenReturn(movementReport());
        when(reportService.toStockMovementCsv(any())).thenReturn("Fecha,Tipo\n2026-01-01,IN\n");

        mockMvc.perform(get("/api/v1/reports/stock-movements/export")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("movimientos-stock.csv")));
    }
}
