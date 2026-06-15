package proyecto.sistemaGestion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.StockMovementRequest;
import proyecto.sistemaGestion.dto.StockMovementResponse;
import proyecto.sistemaGestion.service.StockMovementService;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/stock-movements", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Movimientos de Stock", description = "API para el control de movimientos de inventario")
@SecurityRequirement(name = "bearerAuth")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @PostMapping
    @Operation(summary = "Registrar movimiento", description = "Registra una entrada, salida o ajuste de stock")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Movimiento registrado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Stock insuficiente o datos inválidos")
    })
    @PreAuthorize("hasAuthority('SCOPE_stock:manage')")
    public ResponseEntity<StockMovementResponse> registerMovement(@Valid @RequestBody StockMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockMovementService.registerMovement(request));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Historial por producto", description = "Obtiene el historial paginado de movimientos de un producto")
    @PreAuthorize("hasAuthority('SCOPE_stock:view')")
    public ResponseEntity<PageResponse<StockMovementResponse>> findByProductId(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(stockMovementService.findByProductId(productId, page, size));
    }

    @GetMapping("/product/{productId}/all")
    @Operation(summary = "Todo el historial por producto", description = "Obtiene todo el historial de movimientos de un producto")
    @PreAuthorize("hasAuthority('SCOPE_stock:view')")
    public ResponseEntity<List<StockMovementResponse>> findAllByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(stockMovementService.findAllByProductId(productId));
    }
}
