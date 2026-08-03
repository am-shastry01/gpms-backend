package com.gpms.backend.gatepass.application;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GatePassNumberGeneratorTest {

    @Test
    void shouldFormatRequestNumber() {
        String requestNumber = GatePassNumberGenerator.formatRequestNumber(145L);
        assertTrue(requestNumber.startsWith("REQ-"));
        assertTrue(requestNumber.endsWith("000145"));
    }

    @Test
    void shouldFormatGatePassNumber() {
        String gatePassNumber = GatePassNumberGenerator.formatGatePassNumber(9L);
        assertTrue(gatePassNumber.startsWith("GP-"));
        assertTrue(gatePassNumber.endsWith("000009"));
    }
}
