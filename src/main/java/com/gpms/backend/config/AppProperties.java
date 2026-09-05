package com.gpms.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * The address a driver's phone can reach this backend on.
     *
     * "localhost" is useless here: the link is opened on the driver's
     * own handset, so it has to be the machine's LAN address (phone on
     * the same WiFi) or a public tunnel such as ngrok.
     */
    private String publicBaseUrl = "http://localhost:8081";

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
