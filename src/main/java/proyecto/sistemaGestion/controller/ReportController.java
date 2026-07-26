package proyecto.sistemaGestion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import proyecto.sistemaGestion.dto.InventoryValuationReportDTO;
import proyecto.sistemaGestion.dto.StockMovementReportDTO;
import proyecto.sistemaGestion.service.ReportService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping(value = "/api/v1/reports", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "API de reportes de inventario y movimientos de stock")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('SCOPE_report:view')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/inventory-valuation")
    @Operation(summary = "Reporte de valuación de inventario",
               description = "Valor total del inventario, desglosado por categoría")
    public ResponseEntity<InventoryValuationReportDTO> getInventoryValuation() {
        return ResponseEntity.ok(reportService.getInventoryValuationReport());
    }

    @GetMapping(value = "/inventory-valuation/export", produces = "text/csv")
    @Operation(summary = "Exportar reporte de valuación de inventario a CSV")
    public ResponseEntity<byte[]> exportInventoryValuation() {
        InventoryValuationReportDTO report = reportService.getInventoryValuationReport();
        return csvResponse(reportService.toInventoryValuationCsv(report), "valuacion-inventario.csv");
    }

    @GetMapping("/stock-movements")
    @Operation(summary = "Reporte de movimientos de stock",
               description = "Totales de entradas/salidas/ajustes en un rango de fechas")
    public ResponseEntity<StockMovementReportDTO> getStockMovements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(reportService.getStockMovementReport(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX), productId, category));
    }

    @GetMapping(value = "/stock-movements/export", produces = "text/csv")
    @Operation(summary = "Exportar reporte de movimientos de stock a CSV")
    public ResponseEntity<byte[]> exportStockMovements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String category) {
        StockMovementReportDTO report = reportService.getStockMovementReport(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX), productId, category);
        return csvResponse(reportService.toStockMovementCsv(report), "movimientos-stock.csv");
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
