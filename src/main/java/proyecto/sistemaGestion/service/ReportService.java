package proyecto.sistemaGestion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.sistemaGestion.dto.CategoryValuationDTO;
import proyecto.sistemaGestion.dto.InventoryValuationReportDTO;
import proyecto.sistemaGestion.dto.MovementTypeSummaryDTO;
import proyecto.sistemaGestion.dto.StockMovementReportDTO;
import proyecto.sistemaGestion.dto.StockMovementResponse;
import proyecto.sistemaGestion.entity.Product;
import proyecto.sistemaGestion.entity.StockMovement;
import proyecto.sistemaGestion.enums.ProductStatus;
import proyecto.sistemaGestion.repository.ProductRepository;
import proyecto.sistemaGestion.repository.StockMovementRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public InventoryValuationReportDTO getInventoryValuationReport() {
        List<Product> products = productRepository.findAll();

        Map<String, List<Product>> byCategory = products.stream()
                .collect(Collectors.groupingBy(Product::getCategory));

        List<CategoryValuationDTO> categoryBreakdown = byCategory.entrySet().stream()
                .map(entry -> CategoryValuationDTO.builder()
                        .category(entry.getKey())
                        .productCount(entry.getValue().size())
                        .totalQuantity(entry.getValue().stream().mapToInt(Product::getQuantity).sum())
                        .totalValue(entry.getValue().stream()
                                .map(this::productValue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build())
                .sorted(Comparator.comparing(CategoryValuationDTO::getCategory))
                .toList();

        BigDecimal totalValue = products.stream()
                .map(this::productValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InventoryValuationReportDTO.builder()
                .totalProducts(products.size())
                .activeProducts(products.stream().filter(p -> p.getStatus() == ProductStatus.ACTIVE).count())
                .inactiveProducts(products.stream().filter(p -> p.getStatus() == ProductStatus.INACTIVE).count())
                .lowStockCount(products.stream().filter(p -> p.getQuantity() <= p.getMinStock()).count())
                .totalInventoryValue(totalValue)
                .byCategory(categoryBreakdown)
                .build();
    }

    @Transactional(readOnly = true)
    public StockMovementReportDTO getStockMovementReport(LocalDateTime start, LocalDateTime end,
                                                           Long productId, String category) {
        List<StockMovement> movements = stockMovementRepository.findByDateRange(start, end, productId, category);

        Map<String, List<StockMovement>> byType = movements.stream()
                .collect(Collectors.groupingBy(m -> m.getMovementType().name()));

        List<MovementTypeSummaryDTO> typeBreakdown = byType.entrySet().stream()
                .map(entry -> MovementTypeSummaryDTO.builder()
                        .movementType(entry.getKey())
                        .count(entry.getValue().size())
                        .totalQuantity(entry.getValue().stream().mapToInt(StockMovement::getQuantity).sum())
                        .build())
                .sorted(Comparator.comparing(MovementTypeSummaryDTO::getMovementType))
                .toList();

        return StockMovementReportDTO.builder()
                .startDate(start)
                .endDate(end)
                .totalMovements(movements.size())
                .byType(typeBreakdown)
                .movements(movements.stream().map(this::toMovementResponse).toList())
                .build();
    }

    public String toInventoryValuationCsv(InventoryValuationReportDTO report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Categoria,Cantidad de Productos,Cantidad Total en Stock,Valor Total\n");
        for (CategoryValuationDTO row : report.getByCategory()) {
            sb.append(csvField(row.getCategory())).append(',')
              .append(row.getProductCount()).append(',')
              .append(row.getTotalQuantity()).append(',')
              .append(row.getTotalValue()).append('\n');
        }
        sb.append("TOTAL,").append(report.getTotalProducts()).append(",,")
          .append(report.getTotalInventoryValue()).append('\n');
        return sb.toString();
    }

    public String toStockMovementCsv(StockMovementReportDTO report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fecha,Producto,SKU,Tipo,Cantidad,Stock Anterior,Stock Nuevo,Usuario,Observaciones\n");
        for (StockMovementResponse m : report.getMovements()) {
            sb.append(m.getCreatedAt()).append(',')
              .append(csvField(m.getProductName())).append(',')
              .append(csvField(m.getProductSku())).append(',')
              .append(m.getMovementType()).append(',')
              .append(m.getQuantity()).append(',')
              .append(m.getPreviousQuantity()).append(',')
              .append(m.getNewQuantity()).append(',')
              .append(csvField(m.getUserId())).append(',')
              .append(csvField(m.getObservations())).append('\n');
        }
        return sb.toString();
    }

    private BigDecimal productValue(Product product) {
        return product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity()));
    }

    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private StockMovementResponse toMovementResponse(StockMovement movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .productId(movement.getProduct().getId())
                .productName(movement.getProduct().getName())
                .productSku(movement.getProduct().getSku())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .previousQuantity(movement.getPreviousQuantity())
                .newQuantity(movement.getNewQuantity())
                .userId(movement.getUserId())
                .observations(movement.getObservations())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
