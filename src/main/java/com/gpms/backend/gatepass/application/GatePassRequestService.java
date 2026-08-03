package com.gpms.backend.gatepass.application;

import com.gpms.backend.audit.application.AuditLogService;
import com.gpms.backend.common.exception.BusinessException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.common.exception.UnauthorizedOperationException;
import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.driver.domain.Driver;
import com.gpms.backend.driver.infrastructure.DriverRepository;
import com.gpms.backend.gatepass.api.dto.ApprovalActionRequest;
import com.gpms.backend.gatepass.api.dto.ApprovalTrailResponse;
import com.gpms.backend.gatepass.api.dto.AttachmentResponse;
import com.gpms.backend.gatepass.api.dto.ExitConfirmationRequest;
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
    private final QrCodeService qrCodeService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ObjectStorageService objectStorageService;

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
            QrCodeService qrCodeService,
            NotificationService notificationService,
            AuditLogService auditLogService,
            ObjectStorageService objectStorageService
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
        this.qrCodeService = qrCodeService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.objectStorageService = objectStorageService;
    }

    public GatePassResponse create(GatePassCreateRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        Warehouse warehouse = resolveWarehouse(currentUser, request.warehouseId());
        Vendor vendor = vendorRepository.findByIdAndDeletedFalse(request.vendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        Driver driver = resolveDriver(request);
        Vehicle vehicle = resolveVehicle(request, vendor);

        GatePassRequest gatePassRequest = new GatePassRequest();
        gatePassRequest.setRequestNumber(gatePassNumberGenerator.nextRequestNumber());
        gatePassRequest.setWarehouse(warehouse);
        gatePassRequest.setRequestedBy(currentUser);
        gatePassRequest.setVendor(vendor);
        gatePassRequest.setDriver(driver);
        gatePassRequest.setVehicle(vehicle);
        gatePassRequest.setStatus(GatePassStatus.PENDING);
        gatePassRequest.setDispatchDate(request.dispatchDate());
        gatePassRequest.setPackageCount(request.packageCount());
        gatePassRequest.setPackageDescription(request.packageDescription().trim());
        gatePassRequest.setDestination(request.destination().trim());
        gatePassRequest.setRemarks(request.remarks());
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
                .and(GatePassSpecifications.hasStatus(status))
                .and(GatePassSpecifications.warehouseEquals(effectiveWarehouseId))
                .and(GatePassSpecifications.dispatchDateBetween(fromDate, toDate))
                .and(GatePassSpecifications.textSearch(query));
        if (isEmployee(currentUser)) {
            specification = specification.and(GatePassSpecifications.requestedByEquals(currentUser.getId()));
        }
        return gatePassRequestRepository.findAll(specification, pageable).map(this::toResponse);
    }

    public GatePassResponse getById(UUID id) {
        GatePassRequest gatePassRequest = findAccessibleRequest(id);
        return toResponse(gatePassRequest);
    }

    public AttachmentResponse addAttachment(UUID requestId, MultipartFile file) {
        GatePassRequest gatePassRequest = findAccessibleRequest(requestId);
        User currentUser = currentUserService.requireCurrentUser();
        String objectKey = "warehouses/" + gatePassRequest.getWarehouse().getCode()
                + "/gate-passes/" + gatePassRequest.getId()
                + "/" + Instant.now().toEpochMilli() + "-" + sanitize(file.getOriginalFilename());
        ObjectStorageService.StoredObject storedObject = objectStorageService.upload(objectKey, file);

        Attachment attachment = new Attachment();
        attachment.setGatePassRequest(gatePassRequest);
        attachment.setUploadedBy(currentUser);
        attachment.setFileName(storedObject.fileName());
        attachment.setContentType(storedObject.contentType());
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

    public byte[] getQrPng(UUID requestId) {
        GatePassRequest gatePassRequest = findAccessibleRequest(requestId);
        if (gatePassRequest.getQrContent() == null || gatePassRequest.getQrContent().isBlank()) {
            throw new BusinessException("QR code is not available for this gate pass");
        }
        return qrCodeService.generatePng(gatePassRequest.getQrContent());
    }

    public GatePassResponse approve(UUID requestId, ApprovalActionRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        ensureManagerOrAdmin(currentUser);
        GatePassRequest gatePassRequest = findAccessibleRequest(requestId);
        if (gatePassRequest.getStatus() != GatePassStatus.PENDING) {
            throw new BusinessException("Only pending requests can be approved");
        }

        gatePassRequest.setApprovedBy(currentUser);
        gatePassRequest.setApprovalTime(Instant.now());
        gatePassRequest.setManagerComments(request.comments());
        gatePassRequest.setGatePassNumber(gatePassNumberGenerator.nextGatePassNumber());
        gatePassRequest.setStatus(GatePassStatus.GATE_GENERATED);
        gatePassRequest.setQrContent(qrCodeService.buildQrContent(gatePassRequest));

        Approval approval = new Approval();
        approval.setGatePassRequest(gatePassRequest);
        approval.setManager(currentUser);
        approval.setAction(ApprovalAction.APPROVED);
        approval.setComments(request.comments());
        approval.setActionTime(Instant.now());
        approvalRepository.save(approval);

        GatePassRequest saved = gatePassRequestRepository.save(gatePassRequest);
        notifyApproval(saved);
        auditLogService.record(
                "GatePassRequest",
                saved.getId().toString(),
                "APPROVED",
                "PENDING",
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

    public GatePassResponse markExit(UUID requestId, ExitConfirmationRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        ensureSecurityOrAdmin(currentUser);
        GatePassRequest gatePassRequest = findAccessibleRequest(requestId);
        if (gatePassRequest.getStatus() != GatePassStatus.GATE_GENERATED
                && gatePassRequest.getStatus() != GatePassStatus.APPROVED) {
            throw new BusinessException("Only approved gate passes can be marked as exited");
        }
        gatePassRequest.setExitedBy(currentUser);
        gatePassRequest.setExitTime(Instant.now());
        gatePassRequest.setStatus(GatePassStatus.EXITED);
        if (request.remarks() != null && !request.remarks().isBlank()) {
            String existingRemarks = gatePassRequest.getRemarks() == null ? "" : gatePassRequest.getRemarks() + "\n";
            gatePassRequest.setRemarks(existingRemarks + "Exit: " + request.remarks().trim());
        }
        GatePassRequest saved = gatePassRequestRepository.save(gatePassRequest);

        List<User> recipients = new ArrayList<>();
        recipients.add(saved.getRequestedBy());
        recipients.addAll(userRepository.findActiveByRoleAndWarehouse("MANAGER", saved.getWarehouse().getId()));
        recipients.addAll(userRepository.findActiveByRoleAndWarehouse("ADMIN", saved.getWarehouse().getId()));
        notificationService.notifyUsers(
                recipients,
                saved,
                "Truck exited",
                "Gate pass " + saved.getGatePassNumber() + " exited at " + saved.getExitTime()
        );
        auditLogService.record(
                "GatePassRequest",
                saved.getId().toString(),
                "EXITED",
                "GATE_GENERATED",
                saved.getStatus().name(),
                null,
                currentUser
        );
        return toResponse(saved);
    }

    private void notifyApproval(GatePassRequest gatePassRequest) {
        List<User> recipients = new ArrayList<>();
        recipients.add(gatePassRequest.getRequestedBy());
        recipients.addAll(userRepository.findActiveByRoleAndWarehouse("SECURITY", gatePassRequest.getWarehouse().getId()));
        recipients.addAll(userRepository.findActiveByRoleAndWarehouse("ADMIN", gatePassRequest.getWarehouse().getId()));
        String message = "Gate Pass " + gatePassRequest.getGatePassNumber()
                + " approved for truck " + gatePassRequest.getVehicle().getRegistrationNumber()
                + " and vendor " + gatePassRequest.getVendor().getName() + ".";
        notificationService.notifyUsers(recipients, gatePassRequest, "Gate pass approved", message);
    }

    private Warehouse resolveWarehouse(User currentUser, UUID warehouseId) {
        if (warehouseId == null) {
            return currentUser.getWarehouse();
        }
        Warehouse warehouse = warehouseRepository.findByIdAndDeletedFalse(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        if (!hasRole(currentUser, "ADMIN") && !warehouse.getId().equals(currentUser.getWarehouse().getId())) {
            throw new UnauthorizedOperationException("You cannot create requests for another warehouse");
        }
        return warehouse;
    }

    private UUID effectiveWarehouseId(User currentUser, UUID warehouseId) {
        if (hasRole(currentUser, "ADMIN")) {
            return warehouseId;
        }
        if (warehouseId != null && !warehouseId.equals(currentUser.getWarehouse().getId())) {
            throw new UnauthorizedOperationException("You cannot access another warehouse");
        }
        return currentUser.getWarehouse().getId();
    }

    private Driver resolveDriver(GatePassCreateRequest request) {
        if (request.driverId() != null) {
            return driverRepository.findByIdAndDeletedFalse(request.driverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        }
        if (request.driverName() == null || request.driverName().isBlank()
                || request.driverMobile() == null || request.driverMobile().isBlank()) {
            throw new BusinessException("Driver details are required");
        }
        return driverRepository.findByMobileNumberAndDeletedFalse(request.driverMobile().trim())
                .orElseGet(() -> {
                    Driver driver = new Driver();
                    driver.setName(request.driverName().trim());
                    driver.setMobileNumber(request.driverMobile().trim());
                    driver.setLicenseNumber(request.driverLicenseNumber());
                    driver.setActive(true);
                    return driverRepository.save(driver);
                });
    }

    private Vehicle resolveVehicle(GatePassCreateRequest request, Vendor vendor) {
        if (request.vehicleId() != null) {
            return vehicleRepository.findByIdAndDeletedFalse(request.vehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        }
        if (request.truckNumber() == null || request.truckNumber().isBlank()
                || request.vehicleType() == null || request.vehicleType().isBlank()) {
            throw new BusinessException("Vehicle details are required");
        }
        String registrationNumber = request.truckNumber().trim().toUpperCase();
        return vehicleRepository.findByRegistrationNumberIgnoreCaseAndDeletedFalse(registrationNumber)
                .orElseGet(() -> {
                    Vehicle vehicle = new Vehicle();
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
        if (!hasRole(currentUser, "ADMIN") && !gatePassRequest.getWarehouse().getId().equals(currentUser.getWarehouse().getId())) {
            throw new UnauthorizedOperationException("You cannot access this gate pass");
        }
        if (isEmployee(currentUser) && !gatePassRequest.getRequestedBy().getId().equals(currentUser.getId())) {
            throw new UnauthorizedOperationException("You can only access your own requests");
        }
        return gatePassRequest;
    }

    private void ensureManagerOrAdmin(User user) {
        if (!hasRole(user, "MANAGER") && !hasRole(user, "ADMIN")) {
            throw new UnauthorizedOperationException("Manager or admin role required");
        }
    }

    private void ensureSecurityOrAdmin(User user) {
        if (!hasRole(user, "SECURITY") && !hasRole(user, "ADMIN")) {
            throw new UnauthorizedOperationException("Security or admin role required");
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
                gatePassRequest.getRemarks(),
                gatePassRequest.getManagerComments(),
                gatePassRequest.getQrContent(),
                gatePassRequest.getRequestedBy().getId(),
                gatePassRequest.getRequestedBy().getFullName(),
                gatePassRequest.getApprovedBy() != null ? gatePassRequest.getApprovedBy().getId() : null,
                gatePassRequest.getApprovedBy() != null ? gatePassRequest.getApprovedBy().getFullName() : null,
                gatePassRequest.getApprovalTime(),
                gatePassRequest.getExitedBy() != null ? gatePassRequest.getExitedBy().getId() : null,
                gatePassRequest.getExitedBy() != null ? gatePassRequest.getExitedBy().getFullName() : null,
                gatePassRequest.getExitTime(),
                items,
                approvals,
                attachments,
                gatePassRequest.getCreatedAt(),
                gatePassRequest.getUpdatedAt()
        );
    }
}
