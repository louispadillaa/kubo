package com.kubo.product.product.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ProductSuggestResponse(
        UUID id,
        String nameNormalized,
        String brand,
        String category,
        String imageUrl,
        int storeCount,
        BigDecimal referencePrice
) { }
