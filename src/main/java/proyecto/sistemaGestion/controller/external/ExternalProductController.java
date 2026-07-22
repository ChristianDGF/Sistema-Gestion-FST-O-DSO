package proyecto.sistemaGestion.controller.external;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.ProductResponse;
import proyecto.sistemaGestion.dto.external.ExternalProductResponse;
import proyecto.sistemaGestion.enums.ProductStatus;
import proyecto.sistemaGestion.service.ProductService;

@RestController
@RequestMapping(value = "/api/external/v1/products", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "External - Productos", description = "API de solo lectura para socios externos (B2B)")
@SecurityRequirement(name = "oauth2ClientCredentials")
public class ExternalProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Listar productos", description = "Obtiene una lista paginada de productos con filtros")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de productos obtenida exitosamente")
    })
    @PreAuthorize("hasAuthority('SCOPE_external:product:read')")
    public ResponseEntity<PageResponse<ExternalProductResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status) {
        PageResponse<ProductResponse> internal =
                productService.findAll(page, size, sortBy, sortDir, search, category, status, null);
        return ResponseEntity.ok(toExternalPage(internal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID", description = "Obtiene un producto por su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto encontrado"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @PreAuthorize("hasAuthority('SCOPE_external:product:read')")
    public ResponseEntity<ExternalProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ExternalProductResponse.from(productService.findById(id)));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Productos con stock bajo", description = "Obtiene productos cuyo stock está por debajo del mínimo")
    @PreAuthorize("hasAuthority('SCOPE_external:stock:read')")
    public ResponseEntity<PageResponse<ExternalProductResponse>> findLowStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(toExternalPage(productService.findLowStock(page, size)));
    }

    private PageResponse<ExternalProductResponse> toExternalPage(
            PageResponse<ProductResponse> internal) {
        return PageResponse.<ExternalProductResponse>builder()
                .content(internal.getContent().stream().map(ExternalProductResponse::from).toList())
                .page(internal.getPage())
                .size(internal.getSize())
                .totalElements(internal.getTotalElements())
                .totalPages(internal.getTotalPages())
                .last(internal.isLast())
                .build();
    }
}
