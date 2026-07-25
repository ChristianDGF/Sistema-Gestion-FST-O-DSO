package proyecto.sistemaGestion.contract;

import io.restassured.RestAssured;


import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import proyecto.sistemaGestion.config.TestSecurityConfig;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.StockMovementResponse;
import proyecto.sistemaGestion.enums.MovementType;
import proyecto.sistemaGestion.service.StockMovementService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@ActiveProfiles({"test", "contract-test"})
@Import(TestSecurityConfig.class)
public abstract class ExternalStockContractBase {

    @MockitoBean
    private StockMovementService stockMovementService;

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setup() {
        RestAssured.baseURI = "http://localhost:" + port;
        

        StockMovementResponse defaultResponse = StockMovementResponse.builder()
                .id(1L).productId(1L).productName("Laptop").productSku("LAP-001")
                .movementType(MovementType.IN).quantity(5).previousQuantity(10)
                .newQuantity(15).userId("user1").observations("Restock")
                .createdAt(LocalDateTime.of(2023, 1, 1, 12, 0))
                .build();

        when(stockMovementService.findByProductId(eq(1L), anyInt(), anyInt()))
                .thenReturn(PageResponse.<StockMovementResponse>builder()
                        .content(List.of(defaultResponse))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());
    }
}

















