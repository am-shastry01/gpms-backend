package com.gpms.backend.superadmin.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The read models the super-admin pages render. */
public final class SuperAdminView {

    private SuperAdminView() {
    }

    public record CompanyRow(
            UUID id,
            String name,

            /** The prefix on every number this company issues. */
            String code,
            String email,
            String phoneNumber,
            String gstNumber,
            boolean active,
            long warehouses,
            long people,
            long gatePasses,
            Instant createdAt
    ) {
    }

    public record WarehouseRow(
            UUID id,
            String code,
            String name,
            String city,
            boolean active,
            long managers,
            long employees,
            long gatePasses
    ) {
    }

    public record PersonRow(
            UUID id,
            String fullName,
            String email,
            String mobileNumber,
            String role,
            String warehouseName,
            boolean active,
            boolean mustChangePassword
    ) {
    }

    public record CompanyDetail(
            UUID id,
            String name,
            String code,
            String gstNumber,
            String phoneNumber,
            String email,
            String website,
            String address,
            boolean active,
            Instant createdAt,
            long gatePasses,
            List<WarehouseRow> warehouses,
            List<PersonRow> people
    ) {
    }
}
