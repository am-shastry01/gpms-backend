package com.gpms.backend.transport.domain;

import com.gpms.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import com.gpms.backend.company.domain.Company;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * A transport company that can be selected on a gate pass.
 *
 * Deliberately its own table rather than a reuse of vendors: a
 * vendor is who the goods belong to, a transport is who moves
 * them, and conflating the two makes both lists wrong.
 */
@Entity
@Table(name = "transports")
public class Transport extends BaseEntity {

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

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
