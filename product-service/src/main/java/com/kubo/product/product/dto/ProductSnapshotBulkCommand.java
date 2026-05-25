package com.kubo.product.product.dto;

import java.math.BigDecimal;

public record ProductSnapshotBulkCommand(
        String nameRaw,
        String store,
        BigDecimal price,
        String url,
        String imageUrl,
        String brand,
        String category,
        String baseProductQuery

) {
}
