package com.kubo.search.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CompareInitiated(
        //RESPUESTA INMEDIATA (el jobId)
        UUID jobId,
        String message,
        int storesCount
) {
}
