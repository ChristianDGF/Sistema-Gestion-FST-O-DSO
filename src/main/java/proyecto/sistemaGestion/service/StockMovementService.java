package proyecto.sistemaGestion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.StockMovementRequest;
import proyecto.sistemaGestion.dto.StockMovementResponse;
import proyecto.sistemaGestion.entity.Product;
import proyecto.sistemaGestion.entity.StockMovement;
import proyecto.sistemaGestion.enums.MovementType;
import proyecto.sistemaGestion.exception.BusinessException;
import proyecto.sistemaGestion.exception.ResourceNotFoundException;
import proyecto.sistemaGestion.repository.ProductRepository;
import proyecto.sistemaGestion.repository.StockMovementRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;

    @Transactional
    public StockMovementResponse registerMovement(StockMovementRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + request.getProductId()));

        int previousQuantity = product.getQuantity();
        int newQuantity;

        switch (request.getMovementType()) {
            case IN:
                newQuantity = previousQuantity + request.getQuantity();
                break;
            case OUT:
                if (previousQuantity < request.getQuantity()) {
                    throw new BusinessException("Stock insuficiente. Disponible: " + previousQuantity
                            + ", solicitado: " + request.getQuantity());
                }
                newQuantity = previousQuantity - request.getQuantity();
                break;
            case ADJUSTMENT:
                newQuantity = request.getQuantity();
                if (newQuantity < 0) {
                    throw new BusinessException("El ajuste no puede resultar en stock negativo");
                }
                break;
            default:
                throw new BusinessException("Tipo de movimiento no válido");
        }

        product.setQuantity(newQuantity);
        productRepository.save(product);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .movementType(request.getMovementType())
                .quantity(request.getMovementType() == MovementType.ADJUSTMENT
                        ? Math.abs(newQuantity - previousQuantity)
                        : request.getQuantity())
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .userId(request.getUserId())
                .observations(request.getObservations())
                .build();

        movement = stockMovementRepository.save(movement);
        return toResponse(movement);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> findByProductId(Long productId, int page, int size) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + productId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StockMovement> movementPage = stockMovementRepository
                .findByProductIdOrderByCreatedAtDesc(productId, pageable);

        List<StockMovementResponse> content = movementPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<StockMovementResponse>builder()
                .content(content)
                .page(movementPage.getNumber())
                .size(movementPage.getSize())
                .totalElements(movementPage.getTotalElements())
                .totalPages(movementPage.getTotalPages())
                .last(movementPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> findAllByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + productId);
        }

        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private StockMovementResponse toResponse(StockMovement movement) {
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
