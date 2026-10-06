package com.gpms.backend.destination.domain;

import com.gpms.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import com.gpms.backend.company.domain.Company;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * A place a consignment can be dispatched to.
 *
 * Destination used to be free text typed into every request, which
 * meant "Pune", "pune" and "Pune " were three different places as
 * far as any report was concerned. It is a managed list now.
 */
@Entity
@Table(name = "destinations")
public class Destination extends BaseEntity {

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

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
