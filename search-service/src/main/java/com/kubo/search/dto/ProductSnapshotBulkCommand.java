package com.kubo.search.dto;

import java.math.BigDecimal;

public record ProductSnapshotBulkCommand(
        String nameRaw,
        String store,
        BigDecimal price,
        String url,
        String imageUrl,
        String baseProductQuery
) {
}
