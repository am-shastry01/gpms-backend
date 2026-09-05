package com.gpms.backend.gatepass.domain;

public enum GatePassStatus {
    DRAFT,
    PENDING,
    APPROVED,
    GATE_GENERATED,
    REJECTED,
    /**
     * The truck has been checked in at the gate by security but has
     * not left yet. Sits between GATE_GENERATED and EXITED.
     */
    ENTERED,
    EXITED,
    COMPLETED,
    /**
     * Withdrawn by the requester before any approval decision.
     */
    CANCELLED
}
