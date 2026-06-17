package proyecto.sistemaGestion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.sistemaGestion.entity.StockMovement;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    boolean existsByProductId(Long productId);
}
