package com.kubo.search.dto;

import java.math.BigDecimal;
import java.util.List;

public record JobCompletedEvent(
        String jobId,
        String status,        // COMPLETED
        String bestStore,
        BigDecimal savings,
        List<StoreSummary> storeSummaries
) {
}
