package com.gpms.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * The address this backend is reachable on.
     *
     * Used when a generated document or link has to name the server
     * rather than assume it. No longer has anything to do with driver
     * SMS links, which are gone along with the guard flow.
     */
    private String publicBaseUrl = "http://localhost:8081";

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
