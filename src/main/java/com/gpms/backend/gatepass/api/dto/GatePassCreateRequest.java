package com.gpms.backend.gatepass.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GatePassCreateRequest(
        UUID warehouseId,
        /*
         * Optional. A pass with no consignment on it names no vendor,
         * because there is nobody whose goods are being carried.
         */
        UUID vendorId,
        UUID driverId,
        String driverName,
        String driverMobile,
        String driverLicenseNumber,
        UUID vehicleId,
        String truckNumber,
        String vehicleType,
        /*
         * Both optional, and both only used by a caller that sends no
         * items[]: they are the old flat shape, where a pass carried
         * one description and one count instead of lines. A pass with
         * no consignment sends neither, and gets no lines.
         */
        @PositiveOrZero(message = "Package count cannot be negative")
        Integer packageCount,
        String packageDescription,
        @NotBlank(message = "Destination is required")
        String destination,
        @NotNull(message = "Dispatch date is required")
        LocalDate dispatchDate,

        /*
         * Optional. A pass raised in a hurry at the gate may not
         * have the invoice to hand, and refusing the pass over a
         * missing invoice number would stop the truck rather than
         * record it.
         */
        String transportName,
        String invoiceNumber,
        LocalDate invoiceDate,

        String remarks,
        @Valid
        List<GatePassItemRequest> items
) {
}
