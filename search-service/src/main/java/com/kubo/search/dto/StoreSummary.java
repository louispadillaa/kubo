package com.kubo.search.dto;

import java.math.BigDecimal;

public record StoreSummary(
        String store,
        BigDecimal total,
        int productsFound,
        int productsTotal
) {
}
