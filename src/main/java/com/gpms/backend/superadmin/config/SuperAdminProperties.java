package com.gpms.backend.superadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The super-admin site's own credentials.
 *
 * Deliberately NOT a row in the users table. This login is not a client
 * account and must not be reachable from client data: nothing a company
 * admin can do - create a user, grant a role, edit a profile - can ever
 * produce an account that opens /superadmin. It comes from the
 * environment and nowhere else.
 *
 * With no password configured, one is generated at startup and written
 * to the log. That is better than a published default, which is a
 * credential as soon as the repository is readable.
 */
@ConfigurationProperties(prefix = "app.superadmin")
public class SuperAdminProperties {

    private String username = "superadmin";

    private String password = "";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
