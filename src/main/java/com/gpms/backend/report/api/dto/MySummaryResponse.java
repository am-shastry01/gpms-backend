package com.gpms.backend.report.api.dto;

/**
 * The four counters an employee sees on their own dashboard.
 *
 * Without this the app had to call the list endpoint four times and
 * count the rows client-side just to render four numbers.
 */
public record MySummaryResponse(
        long pending,
        long approved,
        long rejected,
        long completed
) {
}
