package com.gpms.backend.superadmin.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * What the super admin fills in to put a new client on the system.
 *
 * Warehouses arrive as two parallel lists - a name and a number for
 * each row - bound from repeated inputs rather than indexed ones
 * (warehouseName=..., warehouseName=...). That way the page can add and
 * remove rows without renumbering anything, and a row the user left
 * blank is simply a blank pair the service drops.
 */
public class CompanyOnboardingForm {

    @NotBlank(message = "Company name is required")
    private String name = "";

    private String gstNumber = "";

    private String phoneNumber = "";

    @Email(message = "That company email is not valid")
    private String email = "";

    private String website = "";

    private String addressLine1 = "";

    private String addressLine2 = "";

    private String city = "";

    private String state = "";

    private String postalCode = "";

    @NotBlank(message = "A short code is required - it goes in front of every gate pass number")
    @Size(max = 12, message = "The code can be at most 12 characters")
    private String code = "";

    private List<String> warehouseName = new ArrayList<>();

    private List<String> warehouseCode = new ArrayList<>();

    @NotBlank(message = "The admin's name is required")
    private String adminFullName = "";

    @Email(message = "That admin email is not valid")
    @NotBlank(message = "The admin's email is required - it is how they sign in")
    private String adminEmail = "";

    private String adminMobile = "";

    @NotBlank(message = "A first password is required")
    @Size(min = 6, message = "The password needs at least 6 characters")
    private String adminPassword = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<String> getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(List<String> warehouseName) {
        this.warehouseName = warehouseName;
    }

    public List<String> getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(List<String> warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    /*
     * The rows with something in them, as (name, number) pairs. A row
     * where both boxes are empty is one the user added and did not use,
     * and is not an error.
     */
    public List<String[]> warehouseRows() {

        List<String[]> rows = new ArrayList<>();

        int count = Math.max(
                warehouseName == null ? 0 : warehouseName.size(),
                warehouseCode == null ? 0 : warehouseCode.size());

        for (int i = 0; i < count; i++) {

            String name = warehouseName != null && i < warehouseName.size()
                    ? warehouseName.get(i).trim() : "";

            String number = warehouseCode != null && i < warehouseCode.size()
                    ? warehouseCode.get(i).trim() : "";

            if (name.isEmpty() && number.isEmpty()) {
                continue;
            }

            rows.add(new String[]{name, number});
        }

        return rows;
    }

    public String getAdminFullName() {
        return adminFullName;
    }

    public void setAdminFullName(String adminFullName) {
        this.adminFullName = adminFullName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminMobile() {
        return adminMobile;
    }

    public void setAdminMobile(String adminMobile) {
        this.adminMobile = adminMobile;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}
