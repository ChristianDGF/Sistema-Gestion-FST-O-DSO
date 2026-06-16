package proyecto.sistemaGestion.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.StockMovementRequest;
import proyecto.sistemaGestion.dto.StockMovementResponse;
import proyecto.sistemaGestion.enums.MovementType;
import proyecto.sistemaGestion.exception.BusinessException;
import proyecto.sistemaGestion.exception.ResourceNotFoundException;
import proyecto.sistemaGestion.service.StockMovementService;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class StockMovementControllerApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                .newQuantity(next).userId("user1").observations("Test")
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_stock:manage")
    void registerMovement_shouldReturn201_whenValid() throws Exception {
        StockMovementRequest request = StockMovementRequest.builder()
                .productId(1L).movementType(MovementType.IN).quantity(5)
                .userId("user1").observations("Restock").build();

        when(stockMovementService.registerMovement(any(StockMovementRequest.class)))
                .thenReturn(createMovementResponse(1L, MovementType.IN, 5, 10, 15));

        mockMvc.perform(post("/api/v1/stock-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementType").value("IN"))
                .andExpect(jsonPath("$.newQuantity").value(15));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_stock:manage")
    void registerMovement_shouldReturn400_whenInsufficientStock() throws Exception {
        StockMovementRequest request = StockMovementRequest.builder()
                .productId(1L).movementType(MovementType.OUT).quantity(999)
                .userId("user1").build();

        when(stockMovementService.registerMovement(any(StockMovementRequest.class)))
                .thenThrow(new BusinessException("Stock insuficiente"));

        mockMvc.perform(post("/api/v1/stock-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_stock:manage")
    void registerMovement_shouldReturn400_whenInvalidData() throws Exception {
        mockMvc.perform(post("/api/v1/stock-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":-1,"userId":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_stock:view")
    void findByProductId_shouldReturn200() throws Exception {
        when(stockMovementService.findByProductId(eq(1L), anyInt(), anyInt()))
                .thenReturn(PageResponse.<StockMovementResponse>builder()
                        .content(List.of(createMovementResponse(1L, MovementType.IN, 5, 10, 15)))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());

        mockMvc.perform(get("/api/v1/stock-movements/product/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_stock:view")
    void findAllByProductId_shouldReturn200() throws Exception {
        when(stockMovementService.findAllByProductId(1L))
                .thenReturn(List.of(createMovementResponse(1L, MovementType.IN, 5, 10, 15)));

        mockMvc.perform(get("/api/v1/stock-movements/product/1/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementType").value("IN"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_stock:manage")
    void registerMovement_shouldReturn404_whenProductNotFound() throws Exception {
        StockMovementRequest request = StockMovementRequest.builder()
                .productId(99L).movementType(MovementType.IN).quantity(5)
                .userId("user1").build();

        when(stockMovementService.registerMovement(any(StockMovementRequest.class)))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado"));

        mockMvc.perform(post("/api/v1/stock-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
