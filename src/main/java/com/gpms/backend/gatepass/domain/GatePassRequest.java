package com.gpms.backend.gatepass.domain;

import com.gpms.backend.common.domain.BaseEntity;
import com.gpms.backend.signature.domain.DigitalSignature;
import com.gpms.backend.driver.domain.Driver;
import com.gpms.backend.user.domain.User;
import com.gpms.backend.vendor.domain.Vendor;
import com.gpms.backend.vehicle.domain.Vehicle;
import com.gpms.backend.warehouse.domain.Warehouse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "gate_pass_requests")
public class GatePassRequest extends BaseEntity {

    @Column(name = "request_number", nullable = false, unique = true, length = 50)
    private String requestNumber;

    @Column(name = "gate_pass_number", unique = true, length = 50)
    private String gatePassNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    /**
     * Whoever approved FIRST. Kept so the approval trail and the
     * reports carry on working; the role-specific pairs below are
     * what the signature blocks on the PDF are drawn from.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    /*
     * Manager and admin approve independently, on their own dates.
     *
     * A pass created on the 1st and approved by a manager on the 6th
     * carries the 6th against the manager's signature - the approval
     * date is the date the action happened, never the date the pass
     * was raised.
     */
    /*
     * The employee block.
     *
     * On an employee-raised pass this is the requester, stamped at
     * creation. On a manager- or admin-raised one it stays blank
     * until an employee signs - the three blocks are three separate
     * authorisations, not three descriptions of one.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_approved_by_user_id")
    private User employeeApprovedBy;

    @Column(name = "employee_approved_at")
    private Instant employeeApprovedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_approved_by_user_id")
    private User managerApprovedBy;

    @Column(name = "manager_approved_at")
    private Instant managerApprovedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_approved_by_user_id")
    private User adminApprovedBy;

    @Column(name = "admin_approved_at")
    private Instant adminApprovedAt;

    /*
     * The exact signature VERSION applied at approval, not a pointer
     * to "this person's signature".
     *
     * If a manager uploads a new signature next month, the pass they
     * signed today still renders with the one they actually signed
     * it with. Resolving the image through the user at render time
     * would silently restyle every historical document.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_signature_id")
    private DigitalSignature requesterSignature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_signature_id")
    private DigitalSignature employeeSignature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_signature_id")
    private DigitalSignature managerSignature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_signature_id")
    private DigitalSignature adminSignature;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GatePassStatus status;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @Column(name = "package_count", nullable = false)
    private Integer packageCount;

    @Column(name = "package_description", nullable = false, length = 500)
    private String packageDescription;

    @Column(name = "destination", nullable = false, length = 255)
    private String destination;

    /*
     * The transporter carrying the consignment.
     *
     * Free text rather than a link to the transport master: the
     * master table exists, but passes are raised at a gate against
     * whoever turned up, and a foreign key would mean refusing a
     * pass because a carrier had not been set up yet.
     */
    @Column(name = "transport_name", length = 255)
    private String transportName;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    /*
     * The date ON the invoice, which is often days before the truck
     * leaves - so it is its own column rather than being inferred
     * from the dispatch date.
     */
    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "manager_comments", length = 1000)
    private String managerComments;

    @Column(name = "approval_time")
    private Instant approvalTime;

    /*
     * Addresses the driver's public pass link. Not the primary key,
     * so it can expire, be revoked and be used once.
     */
    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public String getGatePassNumber() {
        return gatePassNumber;
    }

    public void setGatePassNumber(String gatePassNumber) {
        this.gatePassNumber = gatePassNumber;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public User getEmployeeApprovedBy() {
        return employeeApprovedBy;
    }

    public void setEmployeeApprovedBy(User employeeApprovedBy) {
        this.employeeApprovedBy = employeeApprovedBy;
    }

    public Instant getEmployeeApprovedAt() {
        return employeeApprovedAt;
    }

    public void setEmployeeApprovedAt(Instant employeeApprovedAt) {
        this.employeeApprovedAt = employeeApprovedAt;
    }

    public DigitalSignature getEmployeeSignature() {
        return employeeSignature;
    }

    public void setEmployeeSignature(DigitalSignature employeeSignature) {
        this.employeeSignature = employeeSignature;
    }

    public User getManagerApprovedBy() {
        return managerApprovedBy;
    }

    public void setManagerApprovedBy(User managerApprovedBy) {
        this.managerApprovedBy = managerApprovedBy;
    }

    public Instant getManagerApprovedAt() {
        return managerApprovedAt;
    }

    public void setManagerApprovedAt(Instant managerApprovedAt) {
        this.managerApprovedAt = managerApprovedAt;
    }

    public User getAdminApprovedBy() {
        return adminApprovedBy;
    }

    public void setAdminApprovedBy(User adminApprovedBy) {
        this.adminApprovedBy = adminApprovedBy;
    }

    public Instant getAdminApprovedAt() {
        return adminApprovedAt;
    }

    public void setAdminApprovedAt(Instant adminApprovedAt) {
        this.adminApprovedAt = adminApprovedAt;
    }

    public DigitalSignature getRequesterSignature() {
        return requesterSignature;
    }

    public void setRequesterSignature(DigitalSignature requesterSignature) {
        this.requesterSignature = requesterSignature;
    }

    public DigitalSignature getManagerSignature() {
        return managerSignature;
    }

    public void setManagerSignature(DigitalSignature managerSignature) {
        this.managerSignature = managerSignature;
    }

    public DigitalSignature getAdminSignature() {
        return adminSignature;
    }

    public void setAdminSignature(DigitalSignature adminSignature) {
        this.adminSignature = adminSignature;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public GatePassStatus getStatus() {
        return status;
    }

    public void setStatus(GatePassStatus status) {
        this.status = status;
    }

    public LocalDate getDispatchDate() {
        return dispatchDate;
    }

    public void setDispatchDate(LocalDate dispatchDate) {
        this.dispatchDate = dispatchDate;
    }

    public Integer getPackageCount() {
        return packageCount;
    }

    public void setPackageCount(Integer packageCount) {
        this.packageCount = packageCount;
    }

    public String getPackageDescription() {
        return packageDescription;
    }

    public void setPackageDescription(String packageDescription) {
        this.packageDescription = packageDescription;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTransportName() {
        return transportName;
    }

    public void setTransportName(String transportName) {
        this.transportName = transportName;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getManagerComments() {
        return managerComments;
    }

    public void setManagerComments(String managerComments) {
        this.managerComments = managerComments;
    }

    public Instant getApprovalTime() {
        return approvalTime;
    }

    public void setApprovalTime(Instant approvalTime) {
        this.approvalTime = approvalTime;
    }

}
