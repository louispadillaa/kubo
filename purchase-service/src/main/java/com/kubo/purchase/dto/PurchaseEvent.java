package com.kubo.purchase.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento WebSocket emitido progresivamente mientras Playwright navega.
 * Tipos: NAVIGATING | PRODUCT_FOUND | ADDED_TO_CART | PRODUCT_NOT_FOUND | CART_READY | FAILED
 */
public record PurchaseEvent(
        UUID sessionId,
        String type,
        String productName,
        BigDecimal price,
        int itemsProcessed,
        int itemsTotal,
        String checkoutUrl,  // Solo cuando type = CART_READY
        String message
) {
}
