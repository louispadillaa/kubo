package com.kubo.search;

import com.kubo.search.dto.ProductSnapshotBulkCommand;
import com.kubo.search.dto.ProductSuggestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service", url = "http://localhost:9091")
public interface ProductClient {

    @GetMapping("/api/products/product")
    List<com.kubo.search.dto.ProductSuggestResponse> getProcessedSuggestions(@RequestParam("q") String query);

    @PostMapping("/api/products/snapshots/bulk")
    void saveBulkSnapshots(@RequestBody List<ProductSnapshotBulkCommand> snapshots);

}
