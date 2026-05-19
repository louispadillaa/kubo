package com.kubo.search.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ScrapingJobRequest(
        String jobId,
        String userId,
        List<String> productNames,
        List<String> stores
) {
}
