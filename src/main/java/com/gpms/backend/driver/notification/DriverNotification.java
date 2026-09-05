package com.gpms.backend.driver.notification;

import com.gpms.backend.common.domain.BaseEntity;
import com.gpms.backend.driver.domain.Driver;
import com.gpms.backend.gatepass.domain.GatePassRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A record of one SMS sent to a truck driver.
 *
 * Drivers are not application users, so these cannot live in the
 * notifications table (its recipient_user_id is NOT NULL).
 */
@Entity
@Table(name = "driver_notifications")
public class DriverNotification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gate_pass_request_id", nullable = false)
    private GatePassRequest gatePassRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "link", length = 500)
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DriverNotificationStatus status;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_message_id", length = 120)
    private String providerMessageId;

    @Column(name = "error_detail", length = 1000)
    private String errorDetail;

    public GatePassRequest getGatePassRequest() {
        return gatePassRequest;
    }

    public void setGatePassRequest(GatePassRequest gatePassRequest) {
        this.gatePassRequest = gatePassRequest;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public DriverNotificationStatus getStatus() {
        return status;
    }

    public void setStatus(DriverNotificationStatus status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }
}
