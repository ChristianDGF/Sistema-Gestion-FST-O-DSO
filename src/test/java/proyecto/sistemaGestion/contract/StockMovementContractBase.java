package proyecto.sistemaGestion.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.StockMovementRequest;
import proyecto.sistemaGestion.dto.StockMovementResponse;
import proyecto.sistemaGestion.enums.MovementType;
import proyecto.sistemaGestion.exception.BusinessException;
import proyecto.sistemaGestion.service.StockMovementService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public abstract class StockMovementContractBase {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private StockMovementService stockMovementService;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.webAppContextSetup(context);

        StockMovementResponse defaultResponse = StockMovementResponse.builder()
                .id(1L).productId(1L).productName("Laptop").productSku("LAP-001")
                .movementType(MovementType.IN).quantity(5).previousQuantity(10)
                .newQuantity(15).userId("user1").observations("Restock")
                .createdAt(LocalDateTime.of(2023, 1, 1, 12, 0))
                .build();

        when(stockMovementService.registerMovement(any(StockMovementRequest.class)))
                .thenAnswer(invocation -> {
                    StockMovementRequest req = invocation.getArgument(0);
                    if (req.getQuantity() > 100) {
                        throw new BusinessException("Stock insuficiente");
                    }
                    return defaultResponse;
                });

        when(stockMovementService.findByProductId(eq(1L), anyInt(), anyInt()))
                .thenReturn(PageResponse.<StockMovementResponse>builder()
                        .content(List.of(defaultResponse))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());
    }
}
