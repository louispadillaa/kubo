package com.kubo.purchase;

import com.kubo.purchase.dto.PurchaseInitiated;
import com.kubo.purchase.dto.StartPurchaseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    /**
     * POST /api/purchase/start
     *
     * Inicia el flujo de compra automatizada con Playwright.
     * El Gateway verifica que el usuario sea PREMIUM antes de enrutar aquí.
     *
     * NOTA LEGAL: el proceso se detiene en checkout, nunca automatiza el pago.
     */
    @PostMapping("/start")
    public ResponseEntity<PurchaseInitiated> startPurchase(
            @Valid @RequestBody StartPurchaseRequest request,
            @RequestHeader(value = "X-User-Id",   required = false) String userId,
            @RequestHeader(value = "X-User-Plan", required = false) String userPlan) {

        // En desarrollo sin Gateway podemos probar sin restricción de plan
        String effectiveUserId = (userId != null) ? userId : "dev-user";

        return ResponseEntity.ok(purchaseService.startPurchase(request, effectiveUserId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("purchase-service UP");
    }
}
