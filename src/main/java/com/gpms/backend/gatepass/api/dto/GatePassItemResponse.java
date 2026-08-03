package com.gpms.backend.gatepass.api.dto;

public record GatePassItemResponse(
        Integer lineNumber,
        String itemDescription,
        Integer quantity,
        String unitOfMeasure
) {
}
