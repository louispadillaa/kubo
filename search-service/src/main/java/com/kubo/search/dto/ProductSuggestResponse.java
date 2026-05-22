package com.kubo.search.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSuggestResponse(
        UUID id,
        String nameNormalized,
        String brand,
        String category,
        String imageUrl,
        int storeCount,
        BigDecimal referencePrice
) {}
