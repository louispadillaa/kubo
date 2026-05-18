package com.kubo.product.product.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record PriceHistoryResponse(
        UUID productId,
        String productName,
        String store,
        List<PricePoint> history,
        BigDecimal historicalMin,
        BigDecimal historicalMax,
        BigDecimal currentPrice
) {
}
