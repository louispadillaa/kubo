package com.kubo.product.product;

import com.kubo.product.product.dto.*;
import com.kubo.product.productSnapshot.ProductSnapshot;
import com.kubo.product.productSnapshot.ProductSnapshotRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRespository productRepository;
    private final ProductSnapshotRepository snapshotRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<ProductSuggestResponse> findRecentSnapshotsByProduct(String query) {
        // 1. Busca si el producto base existe en tu catálogo (por ejemplo por nombre aproximado o exacto)
        // 2. Si existe, recupera sus snapshots asociados, mapea todo a tu DTO maduro (ProductSuggestResponse) y devuélvelo.
        // 3. Si no hay registros o están obsoletos, devuelve una lista vacía para que Search sepa que debe activar FastAPI.

        // NOTA: Implementa tu query de repositorio aquí.
        // Si retorna vacío, Search-service automáticamente activará el fallback de Playwright.
        return Collections.emptyList();
    }

    @Transactional
    public void persistBulkSnapshots(List<ProductSnapshotBulkCommand> commands) {
        if (commands == null || commands.isEmpty()) return;

        String queryClave = commands.get(0).baseProductQuery();

        // 1. Buscar o Crear el Producto Base en tu catálogo para agrupar los snapshots
        Product product = productRepository.findByNameNormalizedIgnoreCase(queryClave.trim())
                .orElseGet(() -> productRepository.save(
                        Product.builder()
                                .nameNormalized(queryClave.trim())
                                .brand(commands.get(0).brand() == null ? "Genérico" : commands.get(0).brand())       // <-- AGREGAR
                                .category(commands.get(0).category() == null ? "General" : commands.get(0).category()) // <-- AGREGAR
                                .createdAt(Instant.now())
                                .build()
                ));

        // 2. Mapear cada oferta cruda enviada por Search a tu entidad ProductSnapshot de JPA
        List<ProductSnapshot> snapshots = commands.stream()
                .map(cmd -> ProductSnapshot.builder()
                        .nameRaw(cmd.nameRaw())
                        .store(cmd.store())
                        .price(cmd.price())
                        .url(cmd.url())
                        .imageUrl(cmd.imageUrl())
                        .product(product) // Vinculación @ManyToOne obligatoria en tu BD
                        .build())
                .toList();

        // 3. Guardar todo en lote en PostgreSQL
        snapshotRepository.saveAll(snapshots);
    }



    @Transactional(readOnly = true)
    public List<ProductSuggestResponse> suggest(String query) {
        if (query == null || query.trim().length() < 2) return List.of();

        return productRepository.searchByName(query.trim(), 8).stream()
                .map(product -> {
                    List<ProductSnapshot> snapshots = snapshotRepository.findLatestProductPerStore(product.getId());
                    return productMapper.toSuggestResponse(product, snapshots);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getDetail(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + productId));

        List<ProductSnapshot> latestSnapshots = snapshotRepository.findLatestProductPerStore(productId);

        return productMapper.toDetailResponse(product, latestSnapshots);
    }

    @Transactional(readOnly = true)
    public PriceHistoryResponse getPriceHistory(UUID productId, String store, int days) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        List<ProductSnapshot> history = snapshotRepository.findHistoryByProductAndStore(productId, store, from);

        BigDecimal min = snapshotRepository.findHistoricalMin(productId);
        BigDecimal max = snapshotRepository.findHistoricalMax(productId);

        return productMapper.toPriceHistoryResponse(product, store, history, min, max);
    }


}
