package com.kubo.product.product.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record PricePoint(
        Instant scrapedAt,
        BigDecimal price,
        Boolean available
) { }
