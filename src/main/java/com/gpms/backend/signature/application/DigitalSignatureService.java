package com.gpms.backend.signature.application;

import com.gpms.backend.common.exception.BusinessException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.signature.api.dto.DigitalSignatureResponse;
import com.gpms.backend.signature.domain.DigitalSignature;
import com.gpms.backend.signature.infrastructure.DigitalSignatureRepository;
import com.gpms.backend.user.domain.User;
import com.gpms.backend.user.infrastructure.UserRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class DigitalSignatureService {

    /**
     * A signature is a small transparent PNG in practice. The cap is
     * deliberately tight: these are embedded in every PDF the system
     * prints, so a multi-megabyte upload would bloat every document
     * produced from then on.
     */
    private static final long MAX_BYTES = 1024L * 1024L;

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/png", "image/jpeg", "image/jpg");

    private final DigitalSignatureRepository digitalSignatureRepository;
    private final UserRepository userRepository;

    public DigitalSignatureService(
            DigitalSignatureRepository digitalSignatureRepository,
            UserRepository userRepository
    ) {
        this.digitalSignatureRepository = digitalSignatureRepository;
        this.userRepository = userRepository;
    }

    /**
     * The signature currently in force for a person, if they have
     * one.
     *
     * Returns an Optional rather than throwing: an approver without a
     * signature on file must still be able to approve. The pass then
     * prints their name and date over an empty signature line.
     */
    @Transactional(readOnly = true)
    public Optional<DigitalSignature> findCurrentFor(UUID userId) {
        return digitalSignatureRepository.findByUserIdAndActiveTrueAndDeletedFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<DigitalSignatureResponse> listCurrent() {
        return digitalSignatureRepository.findAllByActiveTrueAndDeletedFalse()
                .stream()
                .map(DigitalSignatureService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DigitalSignature requireImage(UUID signatureId) {
        return digitalSignatureRepository.findByIdAndDeletedFalse(signatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Signature not found"));
    }

    /**
     * Stores a NEW version of this person's signature.
     *
     * Available to EVERY role, not just approvers. The requester's
     * signature is printed on the pass at the moment it is raised,
     * so an employee needs one just as much as a manager does.
     *
     * The previous version is deactivated rather than replaced. Every
     * gate pass that was approved with it holds its id and goes on
     * rendering exactly as it was signed - which is the whole reason
     * these rows are immutable.
     */
    public DigitalSignatureResponse upload(UUID userId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Signature image is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("Signature image must be 1 MB or smaller");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase();

        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("Signature must be a PNG or JPEG image");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException("Could not read the uploaded signature");
        }

        digitalSignatureRepository.findByUserIdAndActiveTrueAndDeletedFalse(userId)
                .ifPresent(previous -> {
                    previous.setActive(false);
                    digitalSignatureRepository.save(previous);
                });

        /*
         * Flushed before the insert so the partial unique index on
         * (user_id) WHERE active is not tripped by the old row still
         * being active inside this transaction.
         */
        digitalSignatureRepository.flush();

        DigitalSignature signature = new DigitalSignature();
        signature.setUser(user);
        signature.setFileName(sanitize(file.getOriginalFilename()));
        signature.setContentType(contentType);
        signature.setImageData(bytes);
        signature.setSizeBytes((long) bytes.length);
        signature.setActive(true);

        return toResponse(digitalSignatureRepository.save(signature));
    }

    /**
     * Retires the current signature without putting another in its
     * place. Past approvals keep the version they were signed with.
     */
    public void remove(UUID userId) {
        DigitalSignature current = digitalSignatureRepository
                .findByUserIdAndActiveTrueAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No signature on file"));
        current.setActive(false);
        current.setDeletedAt(Instant.now());
        digitalSignatureRepository.save(current);
    }

    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "signature.png";
        }
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static DigitalSignatureResponse toResponse(DigitalSignature signature) {
        return new DigitalSignatureResponse(
                signature.getId(),
                signature.getUser().getId(),
                signature.getUser().getFullName(),
                signature.getFileName(),
                signature.getContentType(),
                signature.getSizeBytes(),
                signature.getCreatedAt()
        );
    }
}
