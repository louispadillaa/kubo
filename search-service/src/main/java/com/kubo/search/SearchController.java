package com.kubo.search;

import com.kubo.search.dto.CompareInitiated;
import com.kubo.search.dto.CompareRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * POST /api/search/compare
     *
     * El Gateway ya validó el JWT y propagó X-User-Id en el header.
     * Este servicio NO valida JWT — solo lee el header para identificar al usuario.
     *
     * Responde inmediatamente con jobId.
     * Los resultados llegan al frontend por WebSocket: /user/queue/results/{jobId}
     */
    @PostMapping("/compare")
    public ResponseEntity<CompareInitiated> compare(
            @Valid @RequestBody CompareRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // En desarrollo sin Gateway, usamos un userId por defecto
        String effectiveUserId = (userId != null) ? userId : "anonymous";
        return ResponseEntity.ok(searchService.compare(request, effectiveUserId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("search-service UP");
    }
}
