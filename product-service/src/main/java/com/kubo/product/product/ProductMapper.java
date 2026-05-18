package com.kubo.product.product;

import com.kubo.product.product.dto.*;
import com.kubo.product.productSnapshot.ProductSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ProductMapper {

    public ProductSuggestResponse toSuggestResponse(Product product, List<ProductSnapshot> snapshots) {
        // Extraemos la lógica de cálculo aquí
        BigDecimal minPrice = snapshots.stream()
                .filter(ProductSnapshot::getAvailable)
                .map(ProductSnapshot::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);

        int activeStores = (int) snapshots.stream()
                .filter(ProductSnapshot::getAvailable)
                .count();

        // 3. Selección SEGURA de la imagen
        // Intentamos sacar la imagen del primer snapshot disponible, si no hay, ponemos null
        String imageUrl = snapshots.stream()
                .map(ProductSnapshot::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);

        return ProductSuggestResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .category(product.getCategory())
                .imageUrl(imageUrl)
                .storeCount(activeStores)
                .referencePrice(minPrice)
                .build();
    }

    // --- Detalle del Producto ---
    public ProductDetailResponse toDetailResponse(Product product, List<ProductSnapshot> latestSnapshots) {
        List<StorePriceDto> storePrices = latestSnapshots.stream()
                .map(s -> new StorePriceDto(
                        s.getStore(),
                        s.getPrice(),
                        s.getAvailable(),
                        s.getUrl(),
                        s.getScrapedAt()
                )).toList();

        String imageUrl = latestSnapshots.stream()
                .map(ProductSnapshot::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .category(product.getCategory())
                .unit(product.getUnit())
                .unitValue(product.getUnitValue())
                .imageUrl(imageUrl)
                .storePrices(storePrices)
                .build();
    }

    // --- Historial de Precios ---
    public PriceHistoryResponse toPriceHistoryResponse(
            Product product,
            String store,
            List<ProductSnapshot> history,
            BigDecimal min,
            BigDecimal max) {

        List<PricePoint> points = history.stream()
                .map(s -> new PricePoint(s.getScrapedAt(), s.getPrice(), s.getAvailable()))
                .toList();

        BigDecimal current = history.isEmpty() ? null
                : history.get(history.size() - 1).getPrice();

        return PriceHistoryResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .store(store)
                .history(points)
                .historicalMin(min)
                .historicalMax(max)
                .currentPrice(current)
                .build();
    }
}
