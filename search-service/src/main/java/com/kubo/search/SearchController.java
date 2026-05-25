package com.kubo.search;

import com.kubo.search.dto.CompareInitiated;
import com.kubo.search.dto.CompareRequest;
import com.kubo.search.dto.ProductSuggestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/suggest")
    public ResponseEntity<List<ProductSuggestResponse>> suggest(
            @RequestParam(name = "q") String query,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // Validación básica defensiva
        if (query == null || query.trim().length() < 3) {
            return ResponseEntity.badRequest().build();
        }

        // Ejecutamos la orquestación del servicio
        List<ProductSuggestResponse> responses = searchService.suggest(query);
        return ResponseEntity.ok(responses);
    }

    // 2. EL NUEVO ENDPOINT: Para la búsqueda profunda (Al dar Enter o clic en Buscar)
    @GetMapping // Al no ponerle un path interno, escucha directamente en "/api/search"
    public ResponseEntity<List<ProductSuggestResponse>> search(
            @RequestParam(name = "q") String query) {

        // Validación defensiva idéntica
        if (query == null || query.trim().length() < 3) {
            return ResponseEntity.badRequest().build();
        }

        // Reutilizamos tu potente orquestación distribuida
        List<ProductSuggestResponse> responses = searchService.suggest(query);
        return ResponseEntity.ok(responses);
    }

    /**
     * POST /api/search/compare
     *
     * El Gateway ya validó el JWT y propagó X-User-Id en el header.
     * Responde inmediatamente con jobId. Los resultados llegan por WebSocket.
     */
    @PostMapping("/compare")
    public ResponseEntity<CompareInitiated> compare(
            @Valid @RequestBody CompareRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String effectiveUserId = (userId != null) ? userId : "anonymous";
        return ResponseEntity.ok(searchService.compare(request, effectiveUserId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("search-service UP");
    }
}
