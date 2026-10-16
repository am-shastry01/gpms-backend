package com.gpms.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.minio")
public class MinioProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;

    /*
     * S3 region. Cloudflare R2 wants the literal "auto"; a real AWS
     * bucket wants its own region. Left blank the client works it out,
     * which is right for a self-hosted MinIO.
     */
    private String region;

    /** How long a generated photo link stays valid. */
    private int linkExpiryMinutes = 60;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getLinkExpiryMinutes() {
        return linkExpiryMinutes;
    }

    public void setLinkExpiryMinutes(int linkExpiryMinutes) {
        this.linkExpiryMinutes = linkExpiryMinutes;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
}
