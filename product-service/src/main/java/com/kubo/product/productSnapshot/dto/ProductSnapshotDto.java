package com.kubo.product.productSnapshot.dto;

import java.math.BigDecimal;

public record ProductSnapshotDto(
        String nameRaw,
        String store,
        BigDecimal price
){
}
