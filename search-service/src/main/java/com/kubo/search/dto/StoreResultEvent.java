package com.kubo.search.dto;

import java.math.BigDecimal;
import java.util.List;

public record StoreResultEvent(
        String jobId,
        String store,
        String status,        // PARTIAL | COMPLETED | FAILED
        List<ProductPriceResult> products,
        BigDecimal storeTotal,
        String errorMessage
) {
}
