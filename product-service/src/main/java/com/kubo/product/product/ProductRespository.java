package com.kubo.product.product;

import com.kubo.product.productSnapshot.ProductSnapshot;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRespository extends JpaRepository<Product, UUID> {


    // Catalog

    //Busca un producto por nombre
    @Query(value = """
        SELECT * FROM products
        WHERE to_tsvector('spanish', name_normalized) @@ plainto_tsquery('spanish', :query)
        ORDER BY ts_rank(to_tsvector('spanish', name_normalized), plainto_tsquery('spanish', :query)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Product> searchByName(@Param("query") String query, @Param("limit") int limit);

    //Buscar nombre por categoría
    List<Product> findByCategoryIgnoreCaseOrderByNameNormalized(String category);

    boolean existsByNameNormalized(String nameNormalized);


    Optional<Product> findByNameNormalizedIgnoreCase(String trim);
}
