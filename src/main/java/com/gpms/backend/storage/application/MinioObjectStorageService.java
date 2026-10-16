package com.gpms.backend.storage.application;

import com.gpms.backend.common.exception.BusinessException;
import com.gpms.backend.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MinioObjectStorageService implements ObjectStorageService {

    private static final Logger log =
            LoggerFactory.getLogger(MinioObjectStorageService.class);

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
            throw new BusinessException("Could not store the uploaded file");
        }
    }

    @Override
    public String presignedUrl(String objectKey, String fallbackUrl) {

        if (objectKey == null || objectKey.isBlank()) {
            return fallbackUrl;
        }

        try {

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .expiry(
                                    minioProperties.getLinkExpiryMinutes(),
                                    TimeUnit.MINUTES)
                            .build()
            );

        } catch (Exception failure) {
            /*
             * A pass that cannot produce a photo link is still a pass.
             * The stored url goes back instead - it will not open on a
             * private bucket, but a broken picture is a far better
             * outcome than a gate pass that refuses to load at the
             * gate because its attachment could not be signed.
             */
            log.warn("Could not sign a link for {}: {}",
                    objectKey, failure.getMessage());

            return fallbackUrl;
        }
    }

    /*
     * Creates the bucket only if it is genuinely missing AND we are
     * allowed to create it.
     *
     * A scoped R2 or S3 token usually cannot create buckets, and on
     * some providers it cannot even ask whether one exists. Neither is
     * a reason to fail an upload into a bucket that was created by
     * hand, so a refusal here is logged and the upload goes ahead - if
     * the bucket really is missing, the putObject that follows says so
     * properly.
     */
    private void ensureBucketExists() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build()
            );
            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
            }
        } catch (Exception failure) {
            log.debug("Bucket check skipped for {}: {}",
                    minioProperties.getBucket(), failure.getMessage());
        }
    }
}
