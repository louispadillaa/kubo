package com.kubo.product.product.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record StorePriceDto(
        String store,
        BigDecimal price,
        Boolean available,
        String url,
        Instant lastUpdated
) {}
