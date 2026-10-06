package com.gpms.backend.company.domain;

import com.gpms.backend.common.domain.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;

/**
 * One client of this system.
 *
 * Everything a client owns hangs off this row: its warehouses, the
 * people who sign in, and through them every gate pass. Nothing in the
 * tenant-facing API may read across two of these - a company's admin is
 * the top of their own tree and sees no further.
 *
 * Created only from the super-admin site, never from the app.
 */
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /*
     * The short code in front of every number this company issues -
     * ACME-GP-2026-000001. Unique across companies, so two clients'
     * documents can never read the same.
     */
    @Column(name = "code", nullable = false, length = 12)
    private String code;

    @Column(name = "gst_number", length = 30)
    private String gstNumber;

    @Column(name = "phone_number", length = 40)
    private String phoneNumber;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "website", length = 200)
    private String website;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "state", length = 120)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "logo_content_type", length = 120)
    private String logoContentType;

    /*
     * Plain byte[], NOT @Lob.
     *
     * On PostgreSQL, Hibernate maps @Lob byte[] to a large-object OID
     * rather than to bytea, which needs the row read inside the same
     * transaction that opened the LOB and breaks the moment the bytes
     * are touched outside it. bytea is what the migration creates and
     * what a few-kilobyte logo wants.
     *
     * Lazy so the ordinary company queries - the super-admin list, the
     * admin's own profile - do not drag the image along.
     */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "logo_data")
    private byte[] logoData;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
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

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getLogoContentType() {
        return logoContentType;
    }

    public void setLogoContentType(String logoContentType) {
        this.logoContentType = logoContentType;
    }

    public byte[] getLogoData() {
        return logoData;
    }

    public void setLogoData(byte[] logoData) {
        this.logoData = logoData;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** The address as it prints on a gate pass, one line. */
    public String addressOneLine() {
        StringBuilder builder = new StringBuilder();
        for (String part : new String[]{addressLine1, addressLine2, city, state, postalCode}) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(part.trim());
        }
        return builder.toString();
    }
}
