package com.gpms.backend.user.domain;

import com.gpms.backend.common.domain.BaseEntity;
import com.gpms.backend.company.domain.Company;
import com.gpms.backend.warehouse.domain.Warehouse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    /*
     * A copy of the email for anyone created through the app, because
     * the email is the identity. Still its own column: it is the JWT
     * subject, and the seeded accounts sign in under short handles.
     */
    @Column(name = "username", nullable = false, unique = true, length = 150)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "mobile_number", length = 30)
    private String mobileNumber;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /*
     * True while the account still carries the password an admin
     * typed for this person. Cleared the first time they set one of
     * their own.
     */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    /*
     * The moment the password last changed.
     *
     * A token issued before it is refused, which is what makes changing
     * a password actually end the sessions that were using the old one.
     */
    @Column(name = "password_changed_at")
    private java.time.Instant passwordChangedAt;

    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    /*
     * The company this person works for. Not derived from the
     * warehouse, because an admin runs the whole company and is not
     * posted to one site.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /*
     * The site they work at, or null for an admin who covers all of
     * them. It used to be mandatory, which forced an admin into one
     * warehouse and made "all my warehouses" impossible to express.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public java.time.Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(java.time.Instant passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    /*
     * The company id without touching the lazy proxy's other fields -
     * which is all most scoping checks need.
     */
    public java.util.UUID companyId() {
        return company == null ? null : company.getId();
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
