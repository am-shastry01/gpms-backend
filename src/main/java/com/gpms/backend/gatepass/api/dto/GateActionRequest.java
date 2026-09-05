package com.gpms.backend.gatepass.api.dto;

/**
 * Body for the security gate actions (entry and exit) and for
 * cancelling a request. All of them carry nothing but an optional
 * free-text note, so they share one shape.
 */
public record GateActionRequest(
        String remarks
) {
}
