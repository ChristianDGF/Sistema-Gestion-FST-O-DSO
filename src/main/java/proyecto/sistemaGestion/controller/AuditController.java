package proyecto.sistemaGestion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import proyecto.sistemaGestion.dto.AuditRevisionDTO;
import proyecto.sistemaGestion.dto.AuditStatsDTO;
import proyecto.sistemaGestion.service.AuditService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/audit", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "API para consultar el historial de auditoría de Hibernate Envers")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('SCOPE_audit:view')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/stats")
    @Operation(summary = "Estadísticas de auditoría",
               description = "Retorna totales, desglose por tipo de operación y revisiones recientes")
    public ResponseEntity<AuditStatsDTO> getStats() {
        return ResponseEntity.ok(auditService.getAuditStats());
    }

    @GetMapping("/products")
    @Operation(summary = "Historial de revisiones de productos",
               description = "Retorna revisiones paginadas de todas las entidades Product auditadas por Envers")
    public ResponseEntity<Map<String, Object>> getProductRevisions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) Long entityId) {

        List<AuditRevisionDTO> content = auditService.getProductRevisions(page, size, entityId);
        long total = auditService.countProductRevisions(entityId);

        return ResponseEntity.ok(Map.of(
                "content", content,
                "page", page,
                "size", size,
                "totalElements", total,
                "totalPages", (int) Math.ceil((double) total / size)
        ));
    }

    @GetMapping("/movements")
    @Operation(summary = "Historial de revisiones de movimientos",
               description = "Retorna revisiones paginadas de todas las entidades StockMovement auditadas por Envers")
    public ResponseEntity<Map<String, Object>> getMovementRevisions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) Long entityId) {

        List<AuditRevisionDTO> content = auditService.getMovementRevisions(page, size, entityId);
        long total = auditService.countMovementRevisions(entityId);

        return ResponseEntity.ok(Map.of(
                "content", content,
                "page", page,
                "size", size,
                "totalElements", total,
                "totalPages", (int) Math.ceil((double) total / size)
        ));
    }
}
