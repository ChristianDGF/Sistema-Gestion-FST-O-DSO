package proyecto.sistemaGestion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.sistemaGestion.entity.Product;
import proyecto.sistemaGestion.enums.ProductStatus;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:lowStock IS NULL OR (:lowStock = true AND p.quantity <= p.minStock))")
    Page<Product> findByFilters(@Param("search") String search,
                                @Param("category") String category,
                                @Param("status") ProductStatus status,
                                @Param("lowStock") Boolean lowStock,
                                Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.quantity <= p.minStock")
    Page<Product> findLowStockProducts(Pageable pageable);
}
