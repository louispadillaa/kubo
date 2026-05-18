package com.kubo.product.productSnapshot;

import com.kubo.product.product.ProductCurrency;
import com.kubo.product.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "product_snapshots",
        indexes = {
                // Índice principal — consulta más frecuente del sistema:
                // "precio más reciente de producto X en tienda Y"
                @Index(
                        name       = "idx_snapshots_product_store_time",
                        columnList = "product_id, store, scraped_at DESC"
                ),
                @Index(name = "idx_snapshots_store",      columnList = "store"),
                @Index(name = "idx_snapshots_scraped_at", columnList = "scraped_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_raw", nullable = false, length = 500)
    private String nameRaw;

    @Column(nullable = false, length = 50)
    private String store;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProductCurrency currency = ProductCurrency.COP;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(length = 1000)
    private String url;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "scraped_at", nullable = false, updatable = false)
    private Instant scrapedAt;

    @PrePersist
    protected void onCreate() {
        this.scrapedAt = Instant.now();
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
