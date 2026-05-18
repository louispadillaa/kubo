package com.kubo.product.productSnapshot;

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
public interface ProductSnapshotRepository extends JpaRepository<ProductSnapshot, Long> {

    //Busca en la tabla de snapshots y te dice cuánto cuesta el producto hoy en el Éxito, cuánto en Carulla, cuánto en Olímpica, etc.
    @Query(value = """
        SELECT DISTINT ON (ps.store) ps. *
        FROM product_snapshots ps
        WHERE ps.product_id = :productId
            AND ps.available = true
        ORDER BY ps.store, ps.scraped_at DESC
        """, nativeQuery = true)
    List<ProductSnapshot> findLatestProductPerStore(@Param("productId") UUID productId);

    //Busca en la tabla de snapshots y te dice cuánto cuesta el producto hoy en una tienda
    @Query("""
        SELECT ps FROM ProductSnapshot ps
        WHERE ps.product.id = :productId
          AND ps.store = :store
        ORDER BY ps.scrapedAt DESC
        LIMIT 1
        """)
    Optional<ProductSnapshot> findLatestByProductAndStore(
            @Param("productId") UUID productId,
            @Param("store")     String store
    );


    //HISTORY

    //Extrae todos los puntos de datos (fecha y precio) de una tienda específica desde una fecha determinada.
    @Query(value = """
        SELECT * FROM product_snapshots
        WHERE product_id = :productId
          AND store      = :store
          AND scraped_at >= :from
        ORDER BY scraped_at ASC
        """, nativeQuery = true)
    List<ProductSnapshot> findHistoryByProductAndStore(
            @Param("productId") UUID productId,
            @Param("store")     String store,
            @Param("from") Instant from
    );

    // Estadísticas históricas (JPQL)
    @Query("SELECT MIN(ps.price) FROM ProductSnapshot ps WHERE ps.product.id = :productId AND ps.available = true")
    BigDecimal findHistoricalMin(@Param("productId") UUID productId);

    @Query("SELECT MAX(ps.price) FROM ProductSnapshot ps WHERE ps.product.id = :productId AND ps.available = true")
    BigDecimal findHistoricalMax(@Param("productId") UUID productId);
}
