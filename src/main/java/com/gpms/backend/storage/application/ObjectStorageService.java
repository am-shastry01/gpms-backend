package com.gpms.backend.storage.application;

import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageService {

    StoredObject upload(String objectKey, MultipartFile file);

    /**
     * A link that lets the holder fetch one object, for a while.
     *
     * Generated when a pass is READ, not when the photo is uploaded:
     * a link with an expiry cannot be stored in a column and still be
     * valid a week later. The phone opens it in whatever image viewer
     * the person has, and that viewer sends no Authorization header -
     * which is exactly why the link has to carry its own permission.
     *
     * Returns the stored fallback when no storage is configured, so a
     * gate pass still loads on a deployment with no photo bucket.
     */
    String presignedUrl(String objectKey, String fallbackUrl);

    record StoredObject(
            String objectKey,
            String objectUrl,
            long sizeBytes,
            String fileName,
            String contentType
    ) {
    }
}
