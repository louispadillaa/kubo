package com.kubo.search.dto;

public record ScrapingJobResponse(
        String jobId,
        String status,
        String message
) {
}
