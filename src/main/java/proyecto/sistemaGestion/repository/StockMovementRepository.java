package proyecto.sistemaGestion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.sistemaGestion.entity.StockMovement;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    boolean existsByProductId(Long productId);

    @Query("SELECT m FROM StockMovement m WHERE m.createdAt BETWEEN :start AND :end " +
           "AND (:productId IS NULL OR m.product.id = :productId) " +
           "AND (:category IS NULL OR m.product.category = :category) " +
           "ORDER BY m.createdAt DESC")
    List<StockMovement> findByDateRange(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end,
                                         @Param("productId") Long productId,
                                         @Param("category") String category);
}
