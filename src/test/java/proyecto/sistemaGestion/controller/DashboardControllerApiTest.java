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
import proyecto.sistemaGestion.dto.DashboardResponse;
import proyecto.sistemaGestion.service.DashboardService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class DashboardControllerApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_report:view")
    void getDashboard_shouldReturn200() throws Exception {
        when(dashboardService.getDashboard()).thenReturn(DashboardResponse.builder()
                .totalProducts(10).activeProducts(8).lowStockProducts(2).totalMovements(50)
                .criticalProducts(List.of()).recentMovements(List.of()).build());

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(10))
                .andExpect(jsonPath("$.lowStockProducts").value(2));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_stock:view")
    void getDashboard_shouldReturn403_whenInsufficientPermission() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboard_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized());
    }
}
