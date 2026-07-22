package proyecto.sistemaGestion.controller.external;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.StockMovementResponse;
import proyecto.sistemaGestion.dto.external.ExternalStockMovementResponse;
import proyecto.sistemaGestion.service.StockMovementService;

@RestController
@RequestMapping(value = "/api/external/v1/products/{productId}/stock-movements", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "External - Stock", description = "API de solo lectura para socios externos (B2B)")
@SecurityRequirement(name = "oauth2ClientCredentials")
@PreAuthorize("hasAuthority('SCOPE_external:stock:read')")
public class ExternalStockController {

    private final StockMovementService stockMovementService;

    @GetMapping
    @Operation(summary = "Historial de stock por producto", description = "Obtiene el historial paginado de movimientos de un producto")
    public ResponseEntity<PageResponse<ExternalStockMovementResponse>> findByProductId(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<StockMovementResponse> internal =
                stockMovementService.findByProductId(productId, page, size);
        return ResponseEntity.ok(PageResponse.<ExternalStockMovementResponse>builder()
                .content(internal.getContent().stream().map(ExternalStockMovementResponse::from).toList())
                .page(internal.getPage())
                .size(internal.getSize())
                .totalElements(internal.getTotalElements())
                .totalPages(internal.getTotalPages())
                .last(internal.isLast())
                .build());
    }
}
