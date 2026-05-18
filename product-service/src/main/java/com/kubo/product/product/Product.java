package com.kubo.product.product;

import com.kubo.product.productSnapshot.ProductSnapshot;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String brand;
    private String category;
    private String unit;

    @Column(name = "unit_value", precision = 10, scale = 3)
    private BigDecimal unitValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void OnCreate(){
        this.createdAt = Instant.now();
    }

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )

    @Builder.Default
    private List<ProductSnapshot> productSnapshots = new ArrayList<>();
}
