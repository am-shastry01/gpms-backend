package com.gpms.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    /** Turn all driver SMS off without changing any other setting. */
    private boolean enabled = true;

    /** "logging" (default) or "twilio". */
    private String provider = "logging";

    /** How long a driver's pass link stays valid. */
    private int linkTtlHours = 72;

    private Twilio twilio = new Twilio();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getLinkTtlHours() {
        return linkTtlHours;
    }

    public void setLinkTtlHours(int linkTtlHours) {
        this.linkTtlHours = linkTtlHours;
    }

    public Twilio getTwilio() {
        return twilio;
    }

    public void setTwilio(Twilio twilio) {
        this.twilio = twilio;
    }

    public static class Twilio {

        private String accountSid;
        private String authToken;
        private String fromNumber;

        public String getAccountSid() {
            return accountSid;
        }

        public void setAccountSid(String accountSid) {
            this.accountSid = accountSid;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getFromNumber() {
            return fromNumber;
        }

        public void setFromNumber(String fromNumber) {
            this.fromNumber = fromNumber;
        }
    }
}
