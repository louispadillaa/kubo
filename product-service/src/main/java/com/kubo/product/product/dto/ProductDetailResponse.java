package com.kubo.product.product.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record ProductDetailResponse(
        UUID id,
        String name,
        String brand,
        String category,
        String unit,
        BigDecimal unitValue,
        String imageUrl,
        List<StorePriceDto> storePrices
) {}
