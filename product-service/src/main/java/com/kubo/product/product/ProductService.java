package com.kubo.product.product;

import com.kubo.product.product.dto.PriceHistoryResponse;
import com.kubo.product.product.dto.PricePoint;
import com.kubo.product.product.dto.ProductDetailResponse;
import com.kubo.product.product.dto.ProductSuggestResponse;
import com.kubo.product.productSnapshot.ProductSnapshot;
import com.kubo.product.productSnapshot.ProductSnapshotRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRespository productRepository;
    private final ProductSnapshotRepository snapshotRepository;
    private final ProductMapper productMapper;


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
