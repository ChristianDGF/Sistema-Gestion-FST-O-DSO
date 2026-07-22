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
import proyecto.sistemaGestion.dto.ProductResponse;
import proyecto.sistemaGestion.enums.ProductStatus;
import proyecto.sistemaGestion.exception.ResourceNotFoundException;
import proyecto.sistemaGestion.service.ProductService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ExternalProductControllerApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private ProductResponse createResponse(Long id, String name, String sku) {
        return ProductResponse.builder()
                .id(id).name(name).sku(sku).description("Test")
                .category("Electronics").price(BigDecimal.valueOf(100))
                .quantity(10).minStock(5).status(ProductStatus.ACTIVE)
                .lowStock(false).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_external:product:read")
    void findAll_shouldReturn200_withExternalScope() throws Exception {
        when(productService.findAll(anyInt(), anyInt(), anyString(), anyString(), any(), any(), any(), isNull()))
                .thenReturn(PageResponse.<ProductResponse>builder()
                        .content(List.of(createResponse(1L, "Laptop", "LAP-001")))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());

        mockMvc.perform(get("/api/external/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("LAP-001"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:view")
    void findAll_shouldReturn403_withInternalScopeOnly() throws Exception {
        mockMvc.perform(get("/api/external/v1/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    void findAll_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/external/v1/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_external:product:read")
    void findById_shouldReturn200_whenProductExists() throws Exception {
        when(productService.findById(1L)).thenReturn(createResponse(1L, "Laptop", "LAP-001"));

        mockMvc.perform(get("/api/external/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("LAP-001"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_external:product:read")
    void findById_shouldReturn404_whenNotFound() throws Exception {
        when(productService.findById(99L)).thenThrow(new ResourceNotFoundException("Producto no encontrado"));

        mockMvc.perform(get("/api/external/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_external:stock:read")
    void findLowStock_shouldReturn200_withStockScope() throws Exception {
        when(productService.findLowStock(anyInt(), anyInt()))
                .thenReturn(PageResponse.<ProductResponse>builder()
                        .content(List.of(createResponse(1L, "Low Item", "LOW-001")))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());

        mockMvc.perform(get("/api/external/v1/products/low-stock"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_external:product:read")
    void findLowStock_shouldReturn403_withProductScopeOnly() throws Exception {
        mockMvc.perform(get("/api/external/v1/products/low-stock"))
                .andExpect(status().isForbidden());
    }
}
