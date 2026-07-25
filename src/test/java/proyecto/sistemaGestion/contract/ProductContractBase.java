package proyecto.sistemaGestion.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.ProductRequest;
import proyecto.sistemaGestion.dto.ProductResponse;
import proyecto.sistemaGestion.enums.ProductStatus;
import proyecto.sistemaGestion.exception.ResourceNotFoundException;
import proyecto.sistemaGestion.service.ProductService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public abstract class ProductContractBase {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private ProductService productService;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.webAppContextSetup(context);

        ProductResponse defaultResponse = ProductResponse.builder()
                .id(1L).name("Laptop").sku("LAP-001").description("Test")
                .category("Electronics").price(BigDecimal.valueOf(1000))
                .quantity(10).minStock(5).status(ProductStatus.ACTIVE)
                .lowStock(false).createdAt(LocalDateTime.of(2023, 1, 1, 12, 0))
                .build();

        when(productService.findAll(anyInt(), anyInt(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(PageResponse.<ProductResponse>builder()
                        .content(List.of(defaultResponse))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());

        when(productService.findById(1L)).thenReturn(defaultResponse);
        when(productService.findById(99L)).thenThrow(new ResourceNotFoundException("Product not found"));

        when(productService.create(any(ProductRequest.class))).thenReturn(defaultResponse);
        when(productService.update(eq(1L), any(ProductRequest.class))).thenReturn(defaultResponse);

        doNothing().when(productService).delete(1L);
    }
}
