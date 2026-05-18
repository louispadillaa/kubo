package com.kubo.product.product;

import com.kubo.product.product.dto.PriceHistoryResponse;
import com.kubo.product.product.dto.ProductDetailResponse;
import com.kubo.product.product.dto.ProductSuggestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/suggest")
    public ResponseEntity<List<ProductSuggestResponse>> suggest(
            @RequestParam(name = "q") String query) {
        return ResponseEntity.ok(productService.suggest(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getDetail(
            @PathVariable UUID id) {
        return ResponseEntity.ok(productService.getDetail(id));
    }

    /**
     * GET /api/products/{id}/history?store=EXITO&days=90
     * Historial de precios para el gráfico de evolución y predicción Prophet.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<PriceHistoryResponse> getPriceHistory(
            @PathVariable UUID id,
            @RequestParam String store,
            @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(productService.getPriceHistory(id, store, days));
    }
}
