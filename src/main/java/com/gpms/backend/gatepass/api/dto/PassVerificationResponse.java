package com.gpms.backend.gatepass.api.dto;

/**
 * What the guard's app gets back after scanning a driver's QR.
 *
 * The outcome is an explicit enum rather than an HTTP status so the
 * app can show the right message for each case instead of a generic
 * failure.
 */
public record PassVerificationResponse(
        VerificationOutcome outcome,
        String message,
        GatePassResponse gatePass
) {

    public enum VerificationOutcome {

        /** Approved, in date, not used - let the truck in. */
        VALID,

        /** The QR was already scanned at the gate. */
        ALREADY_USED,

        /** The link has passed its validity window. */
        EXPIRED,

        /** Cancelled, rejected, or not approved yet. */
        NOT_APPROVED,

        /** The truck has already been let out on this pass. */
        ALREADY_EXITED,

        /** This pass belongs to a different warehouse. */
        WRONG_WAREHOUSE,

        /** No pass matches the scanned code. */
        NOT_FOUND
    }

    public static PassVerificationResponse of(
            VerificationOutcome outcome,
            String message,
            GatePassResponse gatePass
    ) {
        return new PassVerificationResponse(outcome, message, gatePass);
    }
}
