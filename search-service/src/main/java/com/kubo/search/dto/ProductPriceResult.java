package com.kubo.search.dto;

import java.math.BigDecimal;

public record ProductPriceResult(
        String productName,
        String nameNormalized,
        BigDecimal price,
        Boolean available,
        String url,
        String imageUrl
) {
}
