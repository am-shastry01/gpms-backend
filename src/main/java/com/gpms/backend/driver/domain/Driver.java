package com.gpms.backend.driver.domain;

import com.gpms.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import com.gpms.backend.company.domain.Company;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "drivers")
public class Driver extends BaseEntity {

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

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "mobile_number", nullable = false, unique = true, length = 30)
    private String mobileNumber;

    @Column(name = "license_number", unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
