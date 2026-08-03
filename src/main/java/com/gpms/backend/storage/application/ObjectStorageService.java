package com.gpms.backend.storage.application;

import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageService {

    StoredObject upload(String objectKey, MultipartFile file);

    record StoredObject(
            String objectKey,
            String objectUrl,
            long sizeBytes,
            String fileName,
            String contentType
    ) {
    }
}
