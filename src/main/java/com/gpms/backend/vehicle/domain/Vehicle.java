package com.gpms.backend.vehicle.domain;

import com.gpms.backend.common.domain.BaseEntity;
import com.gpms.backend.vendor.domain.Vendor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.gpms.backend.company.domain.Company;

@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {

    /*
     * The client this record belongs to. Every list, lookup and
     * uniqueness check is fenced by it: two companies may both have a
     * vendor called Ekart, and neither may see the other's.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    @Column(name = "registration_number", nullable = false, unique = true, length = 30)
    private String registrationNumber;

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType;

    @Column(name = "capacity", length = 50)
    private String capacity;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }
}
