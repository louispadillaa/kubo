package com.kubo.search.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CompareRequest(

        //Request del frontend
        UUID listId,
        @NotEmpty(message = "Debes agregar al menos un producto")
        List<String> productNames,
        List<String> stores // null = busca en todas
) {
}
