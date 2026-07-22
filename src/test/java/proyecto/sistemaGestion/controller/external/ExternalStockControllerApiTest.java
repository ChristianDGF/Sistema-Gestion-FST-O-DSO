package proyecto.sistemaGestion.controller.external;

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
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.StockMovementResponse;
import proyecto.sistemaGestion.enums.MovementType;
import proyecto.sistemaGestion.service.StockMovementService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ExternalStockControllerApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private StockMovementService stockMovementService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private StockMovementResponse createMovementResponse(Long id, MovementType type, int qty, int prev, int next) {
        return StockMovementResponse.builder()
                .id(id).productId(1L).productName("Test").productSku("TST")
                .movementType(type).quantity(qty).previousQuantity(prev)
                .newQuantity(next).userId("internal-user-1").observations("Test")
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_external:stock:read")
    void findByProductId_shouldReturn200_withExternalScope() throws Exception {
        when(stockMovementService.findByProductId(eq(1L), anyInt(), anyInt()))
                .thenReturn(PageResponse.<StockMovementResponse>builder()
                        .content(List.of(createMovementResponse(1L, MovementType.IN, 5, 10, 15)))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());

        mockMvc.perform(get("/api/external/v1/products/1/stock-movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].movementType").value("IN"))
                .andExpect(jsonPath("$.content[0].userId").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_stock:view")
    void findByProductId_shouldReturn403_withInternalScopeOnly() throws Exception {
        mockMvc.perform(get("/api/external/v1/products/1/stock-movements"))
                .andExpect(status().isForbidden());
    }

    @Test
    void findByProductId_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/external/v1/products/1/stock-movements"))
                .andExpect(status().isUnauthorized());
    }
}
