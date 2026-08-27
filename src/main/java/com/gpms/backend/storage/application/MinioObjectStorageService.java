package com.gpms.backend.storage.application;

import com.gpms.backend.common.exception.BusinessException;
import com.gpms.backend.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.InputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MinioObjectStorageService implements ObjectStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioObjectStorageService(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public StoredObject upload(String objectKey, MultipartFile file) {
        try {
            ensureBucketExists();
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucket())
                                .object(objectKey)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }
            String objectUrl = minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + objectKey;
            return new StoredObject(
                    objectKey,
                    objectUrl,
                    file.getSize(),
                    file.getOriginalFilename(),
                    file.getContentType()
            );
        } catch (Exception exception) {
            throw new BusinessException("Failed to upload file to object storage: " + exception.getMessage());
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build()
        );
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
        }
    }
}
