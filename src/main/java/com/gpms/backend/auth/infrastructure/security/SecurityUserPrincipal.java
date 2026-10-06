package com.gpms.backend.auth.infrastructure.security;

import com.gpms.backend.user.domain.Role;
import com.gpms.backend.user.domain.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUserPrincipal implements UserDetails {

    private final User user;

    public SecurityUserPrincipal(User user) {
        this.user = user;
    }

    /** The row behind this principal, for checks the filter has to make. */
    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles()
                .stream()
                .map(Role::getCode)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /*
     * Three ways to be switched off, and the company is one of them.
     *
     * Suspending a client has to stop every one of their people signing
     * in - otherwise "suspended" would mean nothing until each account
     * was disabled by hand.
     */
    @Override
    public boolean isEnabled() {
        return user.isActive()
                && !user.isDeleted()
                && user.getCompany() != null
                && user.getCompany().isActive()
                && !user.getCompany().isDeleted();
    }
}
