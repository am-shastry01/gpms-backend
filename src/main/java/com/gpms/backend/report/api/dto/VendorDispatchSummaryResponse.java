package com.gpms.backend.report.api.dto;

public record VendorDispatchSummaryResponse(
        String vendorName,
        long dispatchCount
) {
}
