package com.gpms.backend.gatepass.application;

import com.gpms.backend.audit.application.AuditLogService;
import com.gpms.backend.common.exception.BusinessException;
import com.gpms.backend.company.domain.Company;
import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.common.exception.UnauthorizedOperationException;
import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.driver.domain.Driver;
import com.gpms.backend.driver.infrastructure.DriverRepository;
import com.gpms.backend.gatepass.api.dto.ApprovalActionRequest;
import com.gpms.backend.gatepass.api.dto.ApprovalTrailResponse;
import com.gpms.backend.gatepass.api.dto.AttachmentResponse;
import com.gpms.backend.gatepass.api.dto.GatePassCreateRequest;
import com.gpms.backend.gatepass.api.dto.GatePassItemRequest;
import com.gpms.backend.gatepass.api.dto.GatePassItemResponse;
import com.gpms.backend.gatepass.api.dto.GatePassResponse;
import com.gpms.backend.gatepass.domain.Approval;
import com.gpms.backend.gatepass.domain.ApprovalAction;
import com.gpms.backend.gatepass.domain.Attachment;
import com.gpms.backend.gatepass.domain.GatePassItem;
import com.gpms.backend.gatepass.domain.GatePassRequest;
import com.gpms.backend.gatepass.domain.GatePassStatus;
import com.gpms.backend.gatepass.infrastructure.ApprovalRepository;
import com.gpms.backend.gatepass.infrastructure.AttachmentRepository;
import com.gpms.backend.gatepass.infrastructure.GatePassItemRepository;
import com.gpms.backend.gatepass.infrastructure.GatePassRequestRepository;
import com.gpms.backend.notification.application.NotificationService;
import com.gpms.backend.signature.application.DigitalSignatureService;
import com.gpms.backend.storage.application.ObjectStorageService;
import com.gpms.backend.user.domain.Role;
import com.gpms.backend.user.domain.User;
import com.gpms.backend.user.infrastructure.UserRepository;
import com.gpms.backend.vendor.domain.Vendor;
import com.gpms.backend.vendor.infrastructure.VendorRepository;
import com.gpms.backend.vehicle.domain.Vehicle;
import com.gpms.backend.vehicle.infrastructure.VehicleRepository;
import com.gpms.backend.warehouse.domain.Warehouse;
import com.gpms.backend.warehouse.infrastructure.WarehouseRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class GatePassRequestService {

    private final GatePassRequestRepository gatePassRequestRepository;
    private final GatePassItemRepository gatePassItemRepository;
    private final ApprovalRepository approvalRepository;
    private final AttachmentRepository attachmentRepository;
    private final VendorRepository vendorRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final GatePassNumberGenerator gatePassNumberGenerator;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ObjectStorageService objectStorageService;
    private final DigitalSignatureService digitalSignatureService;
    private final GatePassPdfService gatePassPdfService;

    public GatePassRequestService(
            GatePassRequestRepository gatePassRequestRepository,
            GatePassItemRepository gatePassItemRepository,
            ApprovalRepository approvalRepository,
            AttachmentRepository attachmentRepository,
            VendorRepository vendorRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            WarehouseRepository warehouseRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            GatePassNumberGenerator gatePassNumberGenerator,
            NotificationService notificationService,
            AuditLogService auditLogService,
            ObjectStorageService objectStorageService,
            DigitalSignatureService digitalSignatureService,
            GatePassPdfService gatePassPdfService
    ) {
        this.gatePassRequestRepository = gatePassRequestRepository;
        this.gatePassItemRepository = gatePassItemRepository;
        this.approvalRepository = approvalRepository;
        this.attachmentRepository = attachmentRepository;
        this.vendorRepository = vendorRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.gatePassNumberGenerator = gatePassNumberGenerator;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.objectStorageService = objectStorageService;
        this.digitalSignatureService = digitalSignatureService;
        this.gatePassPdfService = gatePassPdfService;
    }

    public GatePassResponse create(GatePassCreateRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        Warehouse warehouse = resolveWarehouse(currentUser, request.warehouseId());
        Company company = currentUser.getCompany();
        Vendor vendor = vendorRepository
                .findByIdAndCompanyIdAndDeletedFalse(request.vendorId(), company.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        Driver driver = resolveDriver(request, company);
        Vehicle vehicle = resolveVehicle(request, vendor, company);

        GatePassRequest gatePassRequest = new GatePassRequest();
        gatePassRequest.setRequestNumber(gatePassNumberGenerator.nextRequestNumber(company));
        gatePassRequest.setWarehouse(warehouse);
        gatePassRequest.setRequestedBy(currentUser);
        gatePassRequest.setVendor(vendor);
        gatePassRequest.setDriver(driver);
        gatePassRequest.setVehicle(vehicle);

        /*
         * The gate pass number is minted HERE, not on approval.
         *
         * Under the client's flow the pass is a document the
         * requester walks away with immediately; approval is a
         * signature added to it later. So every request that is
         * raised is printable from the moment it is saved, whoever
         * raised it.
         *
         * The status still says PENDING because the REQUEST is
         * pending approval - that is what the approval queue reads.
         * A pass with a number and a PENDING status is exactly the
         * normal state of affairs now, not a half-finished one.
         */
        gatePassRequest.setGatePassNumber(gatePassNumberGenerator.nextGatePassNumber(company));
        gatePassRequest.setStatus(GatePassStatus.PENDING);
        gatePassRequest.setDispatchDate(request.dispatchDate());
        gatePassRequest.setPackageCount(request.packageCount());
        gatePassRequest.setPackageDescription(request.packageDescription().trim());
        gatePassRequest.setDestination(request.destination().trim());
        gatePassRequest.setTransportName(trimToNull(request.transportName()));
        gatePassRequest.setInvoiceNumber(trimToNull(request.invoiceNumber()));
        gatePassRequest.setInvoiceDate(request.invoiceDate());
        gatePassRequest.setRemarks(request.remarks());

        /*
         * The requester's signature is snapshotted HERE, at creation.
         *
         * The pass is printed and carried away immediately, so the
         * copy in someone's hand has to be signed for by whoever
         * raised it. Snapshotting the version rather than pointing at
         * the person means a later signature change cannot alter a
         * document that has already been printed and filed.
         */
        digitalSignatureService.findCurrentFor(currentUser.getId())
                .ifPresent(gatePassRequest::setRequesterSignature);

        /*
         * A pass needs a MANAGER signature and an ADMIN signature.
         * Whoever raised it has already signed - so their own block
         * is filled here, at creation, and only the other one is left
         * outstanding.
         *
         *   employee raises -> manager AND admin still to sign
         *   manager raises  -> admin only
         *   admin raises    -> manager only
         *
         * Nobody is ever asked to counter-sign their own document,
         * and an approver never has to "approve" something they
         * authored just to clear it off their queue.
         */
        applyCreatorAuthorisation(gatePassRequest, currentUser);

        GatePassRequest saved = gatePassRequestRepository.save(gatePassRequest);
        saveItems(saved, request.items(), request.packageDescription(), request.packageCount());

        auditLogService.record(
                "GatePassRequest",
                saved.getId().toString(),
                "CREATED",
                null,
                saved.getStatus().name(),
                "{\"requestNumber\":\"" + saved.getRequestNumber() + "\"}",
                currentUser
        );

        notifyPassRaised(saved);

        return toResponse(saved);
    }

    public Page<GatePassResponse> list(
            String query,
            GatePassStatus status,
            UUID warehouseId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        User currentUser = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        UUID effectiveWarehouseId = effectiveWarehouseId(currentUser, warehouseId);
        Specification<GatePassRequest> specification = Specification.where(GatePassSpecifications.notDeleted())
                /*
                 * First and unconditionally: this company only. An admin
                 * with no warehouse filter means "all of MY warehouses",
                 * which without this line would have meant all of
                 * everybody's.
                 */
                .and(GatePassSpecifications.companyEquals(currentUser.companyId()))
                .and(GatePassSpecifications.hasStatus(status))
                .and(GatePassSpecifications.warehouseEquals(effectiveWarehouseId))
                .and(GatePassSpecifications.dispatchDateBetween(fromDate, toDate))
                .and(GatePassSpecifications.textSearch(query));
        /*
         * Every role lists the whole warehouse, for the same reason the
         * detail view does: an employee who cannot see a pass cannot
         * sign it, and all three roles sign every pass. The employee
         * used to see only their own, which is why a pass raised by the
         * admin never appeared in their history.
         *
         * The exception is an account that is neither an admin nor
         * posted to a site. A null warehouse filter means "all of them",
         * so such an account would have seen every warehouse in the
         * company - more than anybody intended when it was created
         * without one. It sees nothing until it is given a warehouse.
         */
        if (!hasRole(currentUser, "ADMIN") && effectiveWarehouseId == null) {
            specification = specification.and(GatePassSpecifications.nothing());
        }

        return gatePassRequestRepository.findAll(specification, pageable).map(this::toResponse);
    }

    /**
     * The printable pass.
     *
     * Runs inside a read-only transaction so the lazy associations
     * the document needs - vehicle, driver, transport, both
     * approvers and their signature images - can still be loaded
     * while it renders. Outside one this would fail on the first
     * association the PDF touched.
     */
    @Transactional(readOnly = true)
    public byte[] renderPdf(UUID requestId) {
        return gatePassPdfService.render(findAccessibleRequest(requestId));
    }

    public GatePassResponse getById(UUID id) {
        GatePassRequest gatePassRequest = findAccessibleRequest(id);
        return toResponse(gatePassRequest);
    }

    /*
     * What a consignment photo is allowed to be.
     *
     * The stored object is handed back to clients as a URL, so an
     * uploaded text/html or SVG file would run as a page on the storage
     * origin. Only the formats a photo actually arrives in are
     * accepted, and the type is taken from this list rather than from
     * whatever the client claimed.
     */
    private static final java.util.Map<String, String> ALLOWED_ATTACHMENT_TYPES =
            java.util.Map.of(
                    "image/png", "image/png",
                    "image/jpeg", "image/jpeg",
                    "image/jpg", "image/jpeg",
                    "image/webp", "image/webp",
                    "application/pdf", "application/pdf");

    private static final long MAX_ATTACHMENT_BYTES = 8L * 1024 * 1024;

    public AttachmentResponse addAttachment(UUID requestId, MultipartFile file) {
        GatePassRequest gatePassRequest = findAccessibleRequest(requestId);

        if (file == null || file.isEmpty()) {
            throw new BusinessException("A file is required");
        }
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new BusinessException("The file must be 8 MB or smaller");
        }

        String claimedType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase();

        String safeType = ALLOWED_ATTACHMENT_TYPES.get(claimedType);

        if (safeType == null) {
            throw new BusinessException(
                    "Only PNG, JPEG, WebP images or a PDF can be attached");
        }

        User currentUser = currentUserService.requireCurrentUser();
        String objectKey = "warehouses/" + gatePassRequest.getWarehouse().getCode()
                + "/gate-passes/" + gatePassRequest.getId()
                + "/" + Instant.now().toEpochMilli() + "-" + sanitize(file.getOriginalFilename());
        ObjectStorageService.StoredObject storedObject = objectStorageService.upload(objectKey, file);

        Attachment attachment = new Attachment();
        attachment.setGatePassRequest(gatePassRequest);
        attachment.setUploadedBy(currentUser);
        attachment.setFileName(storedObject.fileName());
        /*
         * The vetted type from the allow-list, not the client's string -
         * so nothing can be served back as a type it is not.
         */
        attachment.setContentType(safeType);
        attachment.setObjectKey(storedObject.objectKey());
        attachment.setObjectUrl(storedObject.objectUrl());
        attachment.setSizeBytes(storedObject.sizeBytes());
        Attachment saved = attachmentRepository.save(attachment);

        return new AttachmentResponse(
                saved.getId(),
                saved.getFileName(),
                saved.getContentType(),
                saved.getObjectKey(),
                saved.getObjectUrl(),
                saved.getSizeBytes(),
                saved.getCreatedAt()
        );
    }

    /**
     * Applies one approver's signature to a pass that already exists.
     *
     * A manager and an admin approve independently: either may go
     * first, neither waits for the other, and each is stamped with
     * the date THEY acted on. A pass raised on the 1st and approved
     * by a manager on the 6th carries the 6th in the manager's
     * block - the five day gap changes nothing else about it.
     *
     * Nothing but the approval fields is written here. The vehicle,
     * driver, destination, consignment lines and requester are the
     * values captured at creation and they stay that way; approving
     * late must never rewrite the pass.
     */
    /**
     * Everything still waiting on the signed-in approver's own
     * signature.
     *
     * An admin sees passes with an empty admin block, a manager sees
     * passes with an empty manager block. The two queues are
     * independent, so a pass the manager has already signed stays in
     * the admin's list until the admin signs it too - which is the
     * point, since both signatures are wanted for audit and they can
     * arrive days apart.
     */
    @Transactional(readOnly = true)
    public List<GatePassResponse> awaitingMySignature() {

        User currentUser = currentUserService.requireCurrentUser();

        UUID warehouseId = effectiveWarehouseId(currentUser, null);

        List<GatePassStatus> excluded =
                List.of(GatePassStatus.REJECTED, GatePassStatus.CANCELLED);

        /*
         * Whichever block is mine, and still blank. An admin who is
         * also a manager is asked for the admin signature first,
         * because that is the one that closes the document.
         */
        List<GatePassRequest> pending;

        UUID companyId = currentUser.companyId();

        /*
         * Same guard as the list: no warehouse and not an admin means no
         * queue, rather than everybody's queue.
         */
        if (!hasRole(currentUser, "ADMIN") && warehouseId == null) {
            return List.of();
        }

        if (hasRole(currentUser, "ADMIN")) {
            pending = gatePassRequestRepository
                    .findAwaitingAdminSignature(excluded, companyId, warehouseId);
        } else if (hasRole(currentUser, "MANAGER")) {
            pending = gatePassRequestRepository
                    .findAwaitingManagerSignature(excluded, companyId, warehouseId);
        } else {
            pending = gatePassRequestRepository
                    .findAwaitingEmployeeSignature(excluded, companyId, warehouseId);
        }

        return pending.stream().map(this::toResponse).toList();
    }

    public GatePassResponse approve(UUID requestId, ApprovalActionRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        GatePassRequest gatePassRequest = findAccessibleRequest(requestId);

        if (gatePassRequest.getStatus() == GatePassStatus.REJECTED
                || gatePassRequest.getStatus() == GatePassStatus.CANCELLED) {
            throw new BusinessException("A rejected or cancelled gate pass cannot be approved");
        }

        String previousStatus = gatePassRequest.getStatus().name();
        Instant actionTime = Instant.now();

        /*
         * One signature per role, and you sign your own block.
         *
         * Signing twice in the same role would overwrite a date that
         * has already been printed, so it is refused rather than
         * silently re-stamped.
         */
        if (hasRole(currentUser, "ADMIN")) {

            if (gatePassRequest.getAdminApprovedAt() != null) {
                throw new ConflictException("This gate pass has already been signed by an admin");
            }
            gatePassRequest.setAdminApprovedBy(currentUser);
            gatePassRequest.setAdminApprovedAt(actionTime);
            digitalSignatureService.findCurrentFor(currentUser.getId())
                    .ifPresent(gatePassRequest::setAdminSignature);

        } else if (hasRole(currentUser, "MANAGER")) {

            if (gatePassRequest.getManagerApprovedAt() != null) {
                throw new ConflictException("This gate pass has already been signed by a manager");
            }
            gatePassRequest.setManagerApprovedBy(currentUser);
            gatePassRequest.setManagerApprovedAt(actionTime);
            digitalSignatureService.findCurrentFor(currentUser.getId())
                    .ifPresent(gatePassRequest::setManagerSignature);

            /*
             * managerComments is the manager's field by name and by
             * meaning, so nobody else's note overwrites what a
             * manager wrote. Every approver's note is preserved in
             * the approval trail regardless.
             */
            gatePassRequest.setManagerComments(request == null ? null : request.comments());

        } else {

            if (gatePassRequest.getEmployeeApprovedAt() != null) {
                throw new ConflictException("This gate pass has already been signed by an employee");
            }
            gatePassRequest.setEmployeeApprovedBy(currentUser);
            gatePassRequest.setEmployeeApprovedAt(actionTime);
            digitalSignatureService.findCurrentFor(currentUser.getId())
                    .ifPresent(gatePassRequest::setEmployeeSignature);
        }

        /*
         * APPROVED means all three blocks are filled - nothing less.
         *
         * It used to be set on the FIRST signature, which made a pass
         * with two blanks on it read as approved everywhere it was
         * counted. A pass with a signature still outstanding stays
         * PENDING, which is also what keeps it rejectable.
         */
        boolean fullySigned = isFullySigned(gatePassRequest);

        if (fullySigned) {

            /*
             * The legacy single-approver pair records whoever closed
             * the document, which is what the older reports and the
             * approval trail are built on.
             */
            if (gatePassRequest.getApprovalTime() == null) {
                gatePassRequest.setApprovedBy(currentUser);
                gatePassRequest.setApprovalTime(actionTime);
            }

            gatePassRequest.setStatus(GatePassStatus.APPROVED);
        }

        Approval approval = new Approval();
        approval.setGatePassRequest(gatePassRequest);
        approval.setManager(currentUser);
        approval.setAction(ApprovalAction.APPROVED);
        approval.setComments(request == null ? null : request.comments());
        approval.setActionTime(actionTime);
        approvalRepository.save(approval);

        GatePassRequest saved = gatePassRequestRepository.save(gatePassRequest);

        if (fullySigned) {
            notifyApproval(saved);
        }

        auditLogService.record(
                "GatePassRequest",
                saved.getId().toString(),
                fullySigned ? "APPROVED" : "SIGNED",
                previousStatus,
                saved.getStatus().name(),
                "{\"gatePassNumber\":\"" + saved.getGatePassNumber() + "\"}",
                currentUser
        );
        return toResponse(saved);
    }

    public GatePassResponse reject(UUID requestId, ApprovalActionRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        ensureManagerOrAdmin(currentUser);
        GatePassRequest gatePassRequest = findAccessibleRequest(requestId);
        if (gatePassRequest.getStatus() != GatePassStatus.PENDING) {
            throw new BusinessException("Only pending requests can be rejected");
        }
        if (request.comments() == null || request.comments().isBlank()) {
            throw new BusinessException("Rejection comments are required");
        }

        gatePassRequest.setApprovedBy(currentUser);
        gatePassRequest.setApprovalTime(Instant.now());
        gatePassRequest.setManagerComments(request.comments());
        gatePassRequest.setStatus(GatePassStatus.REJECTED);

        Approval approval = new Approval();
        approval.setGatePassRequest(gatePassRequest);
        approval.setManager(currentUser);
        approval.setAction(ApprovalAction.REJECTED);
        approval.setComments(request.comments());
        approval.setActionTime(Instant.now());
        approvalRepository.save(approval);

        GatePassRequest saved = gatePassRequestRepository.save(gatePassRequest);
        notificationService.notifyUsers(
                List.of(saved.getRequestedBy()),
                saved,
                "Gate pass rejected",
                "Request " + saved.getRequestNumber() + " was rejected. Comments: " + request.comments()
        );
        auditLogService.record(
                "GatePassRequest",
                saved.getId().toString(),
                "REJECTED",
                "PENDING",
                saved.getStatus().name(),
                "{\"comments\":\"" + request.comments().replace("\"", "'") + "\"}",
                currentUser
        );
        return toResponse(saved);
    }

    /**
     * Looks a gate pass up by its human-readable number.
     *
     * The guard scans a QR code or types the number off a printed
     * pass; neither gives them the UUID that GET /{id} needs.
     */
    @Transactional(readOnly = true)
    public GatePassResponse getByGatePassNumber(String gatePassNumber) {
        if (gatePassNumber == null || gatePassNumber.isBlank()) {
            throw new BusinessException("Gate pass number is required");
        }
        GatePassRequest gatePassRequest = gatePassRequestRepository
                .findByGatePassNumberIgnoreCaseAndDeletedFalse(gatePassNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No gate pass found with number " + gatePassNumber));

        /*
         * Re-fetch through findAccessibleRequest so the warehouse and
         * ownership rules are applied in exactly one place rather than
         * being re-implemented (and drifting) here.
         */
        return toResponse(findAccessibleRequest(gatePassRequest.getId()));
    }

    /**
     * Lets the requester withdraw a request that has not been decided
     * on yet. There was previously no way to undo a submission at all.
     */
    public GatePassResponse cancel(UUID requestId, String remarks) {
        User currentUser = currentUserService.requireCurrentUser();
        GatePassRequest gatePassRequest = findAccessibleRequest(requestId);

        boolean isOwner = gatePassRequest.getRequestedBy().getId().equals(currentUser.getId());
        if (!isOwner && !hasRole(currentUser, "ADMIN") && !hasRole(currentUser, "MANAGER")) {
            throw new UnauthorizedOperationException("You can only cancel your own requests");
        }
        if (gatePassRequest.getStatus() != GatePassStatus.PENDING
                && gatePassRequest.getStatus() != GatePassStatus.DRAFT) {
            throw new BusinessException("Only a request that is still pending can be cancelled");
        }

        String previousStatus = gatePassRequest.getStatus().name();
        gatePassRequest.setStatus(GatePassStatus.CANCELLED);
        appendRemark(gatePassRequest, "Cancelled", remarks);

        GatePassRequest saved = gatePassRequestRepository.save(gatePassRequest);
        auditLogService.record(
                "GatePassRequest",
                saved.getId().toString(),
                "CANCELLED",
                previousStatus,
                saved.getStatus().name(),
                null,
                currentUser
        );
        return toResponse(saved);
    }

    private void appendRemark(GatePassRequest gatePassRequest, String label, String remarks) {
        if (remarks == null || remarks.isBlank()) {
            return;
        }
        String existing = gatePassRequest.getRemarks() == null
                ? ""
                : gatePassRequest.getRemarks() + "\n";
        gatePassRequest.setRemarks(existing + label + ": " + remarks.trim());
    }

    /**
     * Tells the approvers a pass has been raised.
     *
     * The pass is already issued and printable at this point, so
     * this is not "please authorise before anything can happen" - it
     * is "this went out, sign it off when you get to it". Managers
     * and admins both get it; the requester does not need telling
     * about their own pass.
     */
    /**
     * Fills the creator's own authorisation block when they are one
     * of the two approvers.
     *
     * Dated at creation because that IS when they authorised it -
     * they raised the document. The status only reaches APPROVED if
     * this leaves nothing outstanding, which happens when a user
     * holds both roles.
     */
    /*
     * The creator signs their own block, and only their own.
     *
     * Three roles sign a gate pass and any of the three can raise
     * one. Whoever raises it has, by raising it, authorised it in
     * their own capacity - so that block is stamped immediately and
     * the other two are left blank for the people who hold them.
     */
    private void applyCreatorAuthorisation(GatePassRequest gatePassRequest, User currentUser) {

        Instant now = Instant.now();

        if (hasRole(currentUser, "EMPLOYEE")) {
            gatePassRequest.setEmployeeApprovedBy(currentUser);
            gatePassRequest.setEmployeeApprovedAt(now);
            digitalSignatureService.findCurrentFor(currentUser.getId())
                    .ifPresent(gatePassRequest::setEmployeeSignature);
        }

        if (hasRole(currentUser, "ADMIN")) {
            gatePassRequest.setAdminApprovedBy(currentUser);
            gatePassRequest.setAdminApprovedAt(now);
            digitalSignatureService.findCurrentFor(currentUser.getId())
                    .ifPresent(gatePassRequest::setAdminSignature);
        }

        if (hasRole(currentUser, "MANAGER")) {
            gatePassRequest.setManagerApprovedBy(currentUser);
            gatePassRequest.setManagerApprovedAt(now);
            digitalSignatureService.findCurrentFor(currentUser.getId())
                    .ifPresent(gatePassRequest::setManagerSignature);
        }

        boolean fullySigned = isFullySigned(gatePassRequest);

        if (fullySigned) {
            gatePassRequest.setApprovedBy(currentUser);
            gatePassRequest.setApprovalTime(now);
            gatePassRequest.setStatus(GatePassStatus.APPROVED);
        }
    }

    /**
     * All three blocks filled. Nothing else counts as approved.
     */
    private boolean isFullySigned(GatePassRequest gatePassRequest) {

        return gatePassRequest.getEmployeeApprovedAt() != null
                && gatePassRequest.getManagerApprovedAt() != null
                && gatePassRequest.getAdminApprovedAt() != null;
    }

    private void notifyPassRaised(GatePassRequest gatePassRequest) {

        UUID warehouseId = gatePassRequest.getWarehouse().getId();

        /*
         * Only the roles that still owe a signature are told. A pass
         * raised by a manager is already manager-signed, so telling
         * every manager it is "awaiting your authorisation" would be
         * false - and would fill their queue with work that is not
         * theirs.
         */
        List<User> recipients = new ArrayList<>();

        if (gatePassRequest.getEmployeeApprovedAt() == null) {
            recipients.addAll(userRepository.findActiveByRoleAndWarehouse("EMPLOYEE", warehouseId));
        }
        if (gatePassRequest.getManagerApprovedAt() == null) {
            recipients.addAll(userRepository.findActiveByRoleAndWarehouse("MANAGER", warehouseId));
        }
        if (gatePassRequest.getAdminApprovedAt() == null) {
            recipients.addAll(userRepository.findActiveByRoleAndWarehouse("ADMIN", warehouseId));
        }

        recipients.removeIf(user -> user.getId().equals(gatePassRequest.getRequestedBy().getId()));

        if (recipients.isEmpty()) {
            return;
        }

        String message = "Gate Pass " + gatePassRequest.getGatePassNumber()
                + " raised by " + gatePassRequest.getRequestedBy().getFullName()
                + " for truck " + gatePassRequest.getVehicle().getRegistrationNumber()
                + ". Awaiting your authorisation.";

        notificationService.notifyUsers(
                recipients, gatePassRequest, "Gate pass awaiting authorisation", message);
    }

    private void notifyApproval(GatePassRequest gatePassRequest) {
        List<User> recipients = new ArrayList<>();
        recipients.add(gatePassRequest.getRequestedBy());
        recipients.addAll(userRepository.findActiveByRoleAndWarehouse("MANAGER", gatePassRequest.getWarehouse().getId()));
        recipients.addAll(userRepository.findActiveByRoleAndWarehouse("ADMIN", gatePassRequest.getWarehouse().getId()));
        String message = "Gate Pass " + gatePassRequest.getGatePassNumber()
                + " approved for truck " + gatePassRequest.getVehicle().getRegistrationNumber()
                + " and vendor " + gatePassRequest.getVendor().getName() + ".";
        notificationService.notifyUsers(recipients, gatePassRequest, "Gate pass approved", message);
    }

    private Warehouse resolveWarehouse(User currentUser, UUID warehouseId) {
        if (warehouseId == null) {
            /*
             * gate_pass_requests.warehouse_id is NOT NULL, so falling
             * back to a user who has no warehouse would fail deep in
             * the persistence layer with an unhelpful message.
             */
            if (currentUser.getWarehouse() == null) {
                throw new BusinessException(
                        "Your account is not assigned to a warehouse, "
                                + "so a warehouse must be supplied");
            }
            return currentUser.getWarehouse();
        }
        /*
         * Scoped to the caller's company, so a warehouse id belonging to
         * another client resolves to nothing rather than to their site.
         */
        Warehouse warehouse = warehouseRepository
                .findByIdAndCompanyIdAndDeletedFalse(warehouseId, currentUser.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        if (!hasRole(currentUser, "ADMIN")) {
            if (currentUser.getWarehouse() == null
                    || !warehouse.getId().equals(currentUser.getWarehouse().getId())) {
                throw new UnauthorizedOperationException("You cannot create requests for another warehouse");
            }
        }
        return warehouse;
    }

    private UUID effectiveWarehouseId(User currentUser, UUID warehouseId) {
        if (hasRole(currentUser, "ADMIN")) {
            return warehouseId;
        }
        UUID ownWarehouseId = currentUser.getWarehouse() == null
                ? null
                : currentUser.getWarehouse().getId();
        if (warehouseId != null && !warehouseId.equals(ownWarehouseId)) {
            throw new UnauthorizedOperationException("You cannot access another warehouse");
        }
        return ownWarehouseId;
    }

    /*
     * Finds the driver inside THIS company, or records a new one for it.
     *
     * The company matters more than it looks. Driver mobile numbers used
     * to be unique across the whole system, so the second client to send
     * the same lorry driver through their gate would have resolved to the
     * first client's driver row - and printed that client's spelling of
     * his name on their pass. Each company now keeps its own record of
     * the same man.
     */
    private Driver resolveDriver(GatePassCreateRequest request, Company company) {
        if (request.driverId() != null) {
            return driverRepository
                    .findByIdAndCompanyIdAndDeletedFalse(request.driverId(), company.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        }
        if (request.driverName() == null || request.driverName().isBlank()
                || request.driverMobile() == null || request.driverMobile().isBlank()) {
            throw new BusinessException("Driver details are required");
        }
        return driverRepository
                .findByMobileNumberAndCompanyIdAndDeletedFalse(
                        request.driverMobile().trim(), company.getId())
                .orElseGet(() -> {
                    Driver driver = new Driver();
                    driver.setCompany(company);
                    driver.setName(request.driverName().trim());
                    driver.setMobileNumber(request.driverMobile().trim());
                    driver.setLicenseNumber(request.driverLicenseNumber());
                    driver.setActive(true);
                    return driverRepository.save(driver);
                });
    }

    private Vehicle resolveVehicle(
            GatePassCreateRequest request,
            Vendor vendor,
            Company company
    ) {
        if (request.vehicleId() != null) {
            return vehicleRepository
                    .findByIdAndCompanyIdAndDeletedFalse(request.vehicleId(), company.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        }
        if (request.truckNumber() == null || request.truckNumber().isBlank()
                || request.vehicleType() == null || request.vehicleType().isBlank()) {
            throw new BusinessException("Vehicle details are required");
        }
        String registrationNumber = request.truckNumber().trim().toUpperCase();
        return vehicleRepository
                .findByRegistrationNumberIgnoreCaseAndCompanyIdAndDeletedFalse(
                        registrationNumber, company.getId())
                .orElseGet(() -> {
                    Vehicle vehicle = new Vehicle();
                    vehicle.setCompany(company);
                    vehicle.setRegistrationNumber(registrationNumber);
                    vehicle.setVehicleType(request.vehicleType().trim());
                    vehicle.setVendor(vendor);
                    vehicle.setActive(true);
                    return vehicleRepository.save(vehicle);
                });
    }

    private void saveItems(
            GatePassRequest gatePassRequest,
            List<GatePassItemRequest> items,
            String packageDescription,
            Integer packageCount
    ) {
        List<GatePassItemRequest> effectiveItems = (items == null || items.isEmpty())
                ? List.of(new GatePassItemRequest(packageDescription, packageCount, "packages"))
                : items;
        int lineNumber = 1;
        for (GatePassItemRequest item : effectiveItems) {
            GatePassItem entity = new GatePassItem();
            entity.setGatePassRequest(gatePassRequest);
            entity.setLineNumber(lineNumber++);
            entity.setItemDescription(item.itemDescription());
            entity.setQuantity(item.quantity());
            entity.setUnitOfMeasure(item.unitOfMeasure());
            gatePassItemRepository.save(entity);
        }
    }

    private GatePassRequest findAccessibleRequest(UUID id) {
        GatePassRequest gatePassRequest = gatePassRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gate pass request not found"));
        User currentUser = currentUserService.requireCurrentUser();

        /*
         * The company fence, before anything else.
         *
         * An admin runs their own company and nothing beyond it, so this
         * check applies to every role without exception - it is the one
         * boundary that is never waived.
         */
        if (!gatePassRequest.getWarehouse().getCompany().getId()
                .equals(currentUser.companyId())) {
            throw new ResourceNotFoundException("Gate pass request not found");
        }

        /*
         * An ADMIN may reach any warehouse of their own company. Anyone
         * else is limited to their own - and a user with no warehouse
         * assigned is limited to nothing, rather than dereferencing null.
         */
        if (!hasRole(currentUser, "ADMIN")) {
            if (currentUser.getWarehouse() == null
                    || !gatePassRequest.getWarehouse().getId().equals(currentUser.getWarehouse().getId())) {
                throw new UnauthorizedOperationException("You cannot access this gate pass");
            }
        }
        /*
         * No ownership check beyond the warehouse.
         *
         * A gate pass carries three signatures - employee, manager and
         * admin - and whoever raised it is only one of the three. So an
         * employee has to be able to open a pass an admin raised, in
         * order to sign it, and the same the other way round. Limiting
         * an employee to their own requests made signing impossible for
         * every pass they did not start.
         *
         * The warehouse check above is the real boundary: people see the
         * passes of the site they work at, and nothing from another one.
         */
        return gatePassRequest;
    }

    private void ensureManagerOrAdmin(User user) {
        if (!hasRole(user, "MANAGER") && !hasRole(user, "ADMIN")) {
            throw new UnauthorizedOperationException("Manager or admin role required");
        }
    }

    private boolean isEmployee(User user) {
        return hasRole(user, "EMPLOYEE");
    }

    private boolean hasRole(User user, String roleCode) {
        return user.getRoles().stream().map(Role::getCode).anyMatch(roleCode::equalsIgnoreCase);
    }

    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "attachment";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /*
     * Blank and absent mean the same thing for these, and storing
     * an empty string would make "not recorded" indistinguishable
     * from "recorded as nothing".
     */
    private String trimToNull(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private GatePassResponse toResponse(GatePassRequest gatePassRequest) {
        List<GatePassItemResponse> items = gatePassItemRepository
                .findAllByGatePassRequestIdAndDeletedFalseOrderByLineNumberAsc(gatePassRequest.getId())
                .stream()
                .map(item -> new GatePassItemResponse(
                        item.getLineNumber(),
                        item.getItemDescription(),
                        item.getQuantity(),
                        item.getUnitOfMeasure()
                ))
                .toList();

        List<ApprovalTrailResponse> approvals = approvalRepository
                .findAllByGatePassRequestIdAndDeletedFalseOrderByActionTimeDesc(gatePassRequest.getId())
                .stream()
                .map(approval -> new ApprovalTrailResponse(
                        approval.getId(),
                        approval.getAction(),
                        approval.getComments(),
                        approval.getManager().getId(),
                        approval.getManager().getFullName(),
                        approval.getActionTime()
                ))
                .toList();

        List<AttachmentResponse> attachments = attachmentRepository
                .findAllByGatePassRequestIdAndDeletedFalseOrderByCreatedAtDesc(gatePassRequest.getId())
                .stream()
                .map(attachment -> new AttachmentResponse(
                        attachment.getId(),
                        attachment.getFileName(),
                        attachment.getContentType(),
                        attachment.getObjectKey(),
                        attachment.getObjectUrl(),
                        attachment.getSizeBytes(),
                        attachment.getCreatedAt()
                ))
                .toList();

        return new GatePassResponse(
                gatePassRequest.getId(),
                gatePassRequest.getRequestNumber(),
                gatePassRequest.getGatePassNumber(),
                gatePassRequest.getStatus(),
                gatePassRequest.getWarehouse().getId(),
                gatePassRequest.getWarehouse().getName(),
                gatePassRequest.getVendor().getId(),
                gatePassRequest.getVendor().getName(),
                gatePassRequest.getDriver().getId(),
                gatePassRequest.getDriver().getName(),
                gatePassRequest.getDriver().getMobileNumber(),
                gatePassRequest.getVehicle().getId(),
                gatePassRequest.getVehicle().getRegistrationNumber(),
                gatePassRequest.getVehicle().getVehicleType(),
                gatePassRequest.getPackageCount(),
                gatePassRequest.getPackageDescription(),
                gatePassRequest.getDestination(),
                gatePassRequest.getDispatchDate(),
                gatePassRequest.getTransportName(),
                gatePassRequest.getInvoiceNumber(),
                gatePassRequest.getInvoiceDate(),
                gatePassRequest.getRemarks(),
                gatePassRequest.getManagerComments(),
                gatePassRequest.getRequestedBy().getId(),
                gatePassRequest.getRequestedBy().getFullName(),
                gatePassRequest.getApprovedBy() != null ? gatePassRequest.getApprovedBy().getId() : null,
                gatePassRequest.getApprovedBy() != null ? gatePassRequest.getApprovedBy().getFullName() : null,
                gatePassRequest.getApprovalTime(),
                gatePassRequest.getEmployeeApprovedBy() != null
                        ? gatePassRequest.getEmployeeApprovedBy().getId() : null,
                gatePassRequest.getEmployeeApprovedBy() != null
                        ? gatePassRequest.getEmployeeApprovedBy().getFullName() : null,
                gatePassRequest.getEmployeeApprovedAt(),
                gatePassRequest.getManagerApprovedBy() != null
                        ? gatePassRequest.getManagerApprovedBy().getId() : null,
                gatePassRequest.getManagerApprovedBy() != null
                        ? gatePassRequest.getManagerApprovedBy().getFullName() : null,
                gatePassRequest.getManagerApprovedAt(),
                gatePassRequest.getAdminApprovedBy() != null
                        ? gatePassRequest.getAdminApprovedBy().getId() : null,
                gatePassRequest.getAdminApprovedBy() != null
                        ? gatePassRequest.getAdminApprovedBy().getFullName() : null,
                gatePassRequest.getAdminApprovedAt(),
                items,
                approvals,
                attachments,
                gatePassRequest.getCreatedAt(),
                gatePassRequest.getUpdatedAt()
        );
    }
}
