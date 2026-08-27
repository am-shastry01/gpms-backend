package com.gpms.backend.gatepass.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record GatePassItemRequest(
        @NotBlank(message = "Item description is required")
        String itemDescription,
        @Positive(message = "Quantity must be greater than zero")
        Integer quantity,
        String unitOfMeasure
) {
}
