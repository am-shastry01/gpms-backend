package com.gpms.backend.gatepass.domain;

public enum GatePassStatus {
    DRAFT,
    PENDING,
    APPROVED,
    GATE_GENERATED,
    REJECTED,
    COMPLETED,
    /**
     * Withdrawn by the requester before any approval decision.
     */
    CANCELLED
}
