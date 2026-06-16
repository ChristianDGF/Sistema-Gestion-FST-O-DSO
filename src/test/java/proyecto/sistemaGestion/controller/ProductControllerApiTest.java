package proyecto.sistemaGestion.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.ProductRequest;
import proyecto.sistemaGestion.dto.ProductResponse;
import proyecto.sistemaGestion.enums.ProductStatus;
import proyecto.sistemaGestion.exception.BusinessException;
import proyecto.sistemaGestion.exception.ResourceNotFoundException;
import proyecto.sistemaGestion.service.ProductService;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ProductControllerApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                .lowStock(false).createdAt(LocalDateTime.now()).build();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:view")
    void findAll_shouldReturn200() throws Exception {
        when(productService.findAll(anyInt(), anyInt(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(PageResponse.<ProductResponse>builder()
                        .content(List.of(createResponse(1L, "Laptop", "LAP-001")))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Laptop"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:view")
    void findById_shouldReturn200_whenProductExists() throws Exception {
        when(productService.findById(1L)).thenReturn(createResponse(1L, "Laptop", "LAP-001"));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("LAP-001"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:view")
    void findById_shouldReturn404_whenNotFound() throws Exception {
        when(productService.findById(99L)).thenThrow(new ResourceNotFoundException("Producto no encontrado"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:manage")
    void create_shouldReturn201_whenValid() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("New Product").sku("NEW-001").category("Electronics")
                .price(BigDecimal.valueOf(99.99)).quantity(10).minStock(3).build();

        when(productService.create(any(ProductRequest.class)))
                .thenReturn(createResponse(1L, "New Product", "NEW-001"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Product"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:manage")
    void create_shouldReturn400_whenDuplicateSku() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("New Product").sku("EXISTING").category("Electronics")
                .price(BigDecimal.valueOf(99.99)).quantity(10).minStock(3).build();

        when(productService.create(any(ProductRequest.class)))
                .thenThrow(new BusinessException("Ya existe un producto con el SKU: EXISTING"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:manage")
    void create_shouldReturn400_whenInvalidData() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","sku":"","category":"","price":-1,"quantity":-1,"minStock":-1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:manage")
    void delete_shouldReturn204_whenDeleted() throws Exception {
        doNothing().when(productService).delete(1L);

        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:manage")
    void delete_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Producto no encontrado")).when(productService).delete(99L);

        mockMvc.perform(delete("/api/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_stock:view")
    void findLowStock_shouldReturn200() throws Exception {
        when(productService.findLowStock(anyInt(), anyInt()))
                .thenReturn(PageResponse.<ProductResponse>builder()
                        .content(List.of(createResponse(1L, "Low Item", "LOW-001")))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());

        mockMvc.perform(get("/api/v1/products/low-stock"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_product:manage")
    void update_shouldReturn200_whenValid() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("Updated").sku("UPD-001").category("Electronics")
                .price(BigDecimal.valueOf(150)).quantity(20).minStock(5).build();

        when(productService.update(eq(1L), any(ProductRequest.class)))
                .thenReturn(createResponse(1L, "Updated", "UPD-001"));

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_stock:view")
    void create_shouldReturn403_whenInsufficientPermission() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test","sku":"TST","category":"Test","price":10,"quantity":1,"minStock":0}
                                """))
                .andExpect(status().isForbidden());
    }
}
