package com.kubo.purchase.dto;

import java.util.UUID;

public record PurchaseInitiated(
        UUID sessionId,
        String store,
        String message,
        String disclaimer  // Recordatorio: el pago lo hace el usuario directamente
) {
}
