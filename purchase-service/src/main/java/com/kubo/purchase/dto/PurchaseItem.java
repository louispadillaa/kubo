package com.kubo.purchase.dto;

import jakarta.validation.constraints.NotBlank;

public record PurchaseItem(
        @NotBlank String productName,
        int quantity,
        String unit
) {
}
