package com.kubo.purchase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record StartPurchaseRequest(
        UUID jobId,
        @NotBlank String store,
        @NotEmpty List<PurchaseItem> items
) {
}
