package com.gpms.backend.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties minioProperties) {

        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(
                        minioProperties.getAccessKey(),
                        minioProperties.getSecretKey());

        /*
         * Only set when configured. Cloudflare R2 requires the literal
         * "auto" and refuses a signature built for any other region;
         * a self-hosted MinIO has no region at all and the client
         * works it out. Setting a wrong one fails with a signature
         * mismatch that says nothing about regions.
         */
        if (minioProperties.getRegion() != null
                && !minioProperties.getRegion().isBlank()) {
            builder = builder.region(minioProperties.getRegion());
        }

        return builder.build();
    }
}
