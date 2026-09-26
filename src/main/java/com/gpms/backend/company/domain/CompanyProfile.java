package com.gpms.backend.company.domain;

import com.gpms.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The company whose name and logo head every printed gate pass.
 *
 * A singleton, enforced by a unique constant {@code code}. Without
 * that guard a second profile could quietly appear and the PDF would
 * render whichever row the database happened to return first.
 */
@Entity
@Table(name = "company_profile")
public class CompanyProfile extends BaseEntity {

    public static final String SINGLETON_CODE = "DEFAULT";

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code = SINGLETON_CODE;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

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

    @Column(name = "phone_number", length = 40)
    private String phoneNumber;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "gst_number", length = 30)
    private String gstNumber;

    @Column(name = "logo_content_type", length = 120)
    private String logoContentType;

    /*
     * Plain byte[], NOT @Lob.
     *
     * On PostgreSQL, Hibernate maps @Lob byte[] to a large-object
     * OID rather than to bytea, which needs the row to be read
     * inside the same transaction that opened the LOB and breaks as
     * soon as the bytes are touched outside it. bytea is what the
     * migration creates and what a few-kilobyte logo wants.
     */
    @Column(name = "logo_data")
    private byte[] logoData;

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

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
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
}
