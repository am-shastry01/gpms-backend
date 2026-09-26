package com.gpms.backend.signature.domain;

import com.gpms.backend.common.domain.BaseEntity;
import com.gpms.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One version of one person's signature image.
 *
 * Rows are immutable. Uploading a replacement deactivates this one
 * and inserts a new row; nothing is overwritten, and nothing is
 * deleted.
 *
 * That is what makes a signed gate pass stable. Each approval stores
 * the id of the exact row applied at the time, so replacing a
 * signature today cannot retroactively change the appearance of a
 * document approved last month.
 */
@Entity
@Table(name = "digital_signatures")
public class DigitalSignature extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    /* bytea, not @Lob - see the note on CompanyProfile.logoData. */
    @Column(name = "image_data", nullable = false)
    private byte[] imageData;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /** False once a newer upload has superseded this version. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
