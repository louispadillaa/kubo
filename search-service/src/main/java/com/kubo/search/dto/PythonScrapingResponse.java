package com.kubo.search.dto;

import java.math.BigDecimal;

public record PythonScrapingResponse(
        String nameRaw,
        String store,
        BigDecimal price,
        String url,
        String imageUrl
) {
}
