package com.gpms.backend.superadmin.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * The browser half of the system.
 *
 * A filter chain of its own, ordered ahead of the API's, matching only
 * /superadmin/**. Separate on purpose:
 *
 *   - It is session-based with a form login, because a person is
 *     sitting in front of it. The API stays stateless and bearer-token.
 *   - CSRF protection is ON here. The API disables it because it has no
 *     cookies to ride on; this chain has a session cookie and real
 *     forms, so it needs the token.
 *   - Its credentials live in the environment, not in the users table,
 *     so no amount of tenant data can mint an account that opens it.
 *
 * The API's JWT filter is registered on the API chain only, so a bearer
 * token means nothing here and a session means nothing there.
 */
@Configuration
@EnableConfigurationProperties(SuperAdminProperties.class)
public class SuperAdminSecurityConfig {

    private static final Logger LOG =
            LoggerFactory.getLogger(SuperAdminSecurityConfig.class);

    public static final String ROLE = "SUPERADMIN";

    private final SuperAdminProperties properties;
    private final LoginAttemptGuard loginAttemptGuard;

    public SuperAdminSecurityConfig(
            SuperAdminProperties properties,
            LoginAttemptGuard loginAttemptGuard
    ) {
        this.properties = properties;
        this.loginAttemptGuard = loginAttemptGuard;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain superAdminFilterChain(HttpSecurity http) throws Exception {

        http
                .securityMatcher("/superadmin/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/superadmin/login", "/superadmin/css/**")
                        .permitAll()
                        .anyRequest().hasRole(ROLE)
                )
                .formLogin(form -> form
                        .loginPage("/superadmin/login")
                        .loginProcessingUrl("/superadmin/login")
                        .defaultSuccessUrl("/superadmin", true)
                        /*
                         * Handlers rather than plain URLs, so the attempt
                         * guard sees every answer: a failure counts, a
                         * success wipes the count.
                         */
                        .successHandler(this::onSuccess)
                        .failureHandler(this::onFailure)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/superadmin/logout")
                        .logoutSuccessUrl("/superadmin/login?signedout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                /*
                 * Scoped to this chain's own in-memory credential, so the
                 * API's DaoAuthenticationProvider - which reads the users
                 * table - is never consulted for a super-admin login, and
                 * this one is never consulted for an app login.
                 */
                .authenticationManager(superAdminAuthenticationManager())
                /*
                 * Refuses a blocked address before the password is even
                 * looked at, so a locked-out caller costs nothing to
                 * turn away.
                 */
                .addFilterBefore(
                        new BlockedAddressFilter(loginAttemptGuard),
                        UsernamePasswordAuthenticationFilter.class)
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    private AuthenticationManager superAdminAuthenticationManager() {

        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        UserDetailsService users = new InMemoryUserDetailsManager(
                User.withUsername(properties.getUsername())
                        .password(encoder.encode(resolvePassword()))
                        .roles(ROLE)
                        .build());

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);

        return new ProviderManager(provider);
    }

    /*
     * A configured password, or a fresh random one announced in the log.
     *
     * No published fallback: a default password in a repository is a
     * credential, not a default. The generated one changes on every
     * restart, which is exactly the nudge needed to go and set
     * APP_SUPERADMIN_PASSWORD properly.
     */
    private String resolvePassword() {

        String configured = properties.getPassword();

        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        String generated = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        LOG.warn("""

                ================================================================
                 SUPER ADMIN PASSWORD (generated - no APP_SUPERADMIN_PASSWORD set)

                   url      : /superadmin
                   username : {}
                   password : {}

                 This changes every time the server restarts. Set
                 APP_SUPERADMIN_PASSWORD to keep one.
                ================================================================
                """, properties.getUsername(), generated);

        return generated;
    }

    private void onSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws java.io.IOException {
        loginAttemptGuard.recordSuccess(request);
        response.sendRedirect(request.getContextPath() + "/superadmin");
    }

    private void onFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws java.io.IOException {
        loginAttemptGuard.recordFailure(request);
        response.sendRedirect(request.getContextPath() + "/superadmin/login?error");
    }

    /**
     * Turns away an address that has run out of attempts.
     *
     * Only the login POST is gated. A blocked caller can still see the
     * page - telling them to come back later is the point.
     */
    private static class BlockedAddressFilter
            extends org.springframework.web.filter.OncePerRequestFilter {

        private final LoginAttemptGuard guard;

        BlockedAddressFilter(LoginAttemptGuard guard) {
            this.guard = guard;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                jakarta.servlet.FilterChain chain
        ) throws jakarta.servlet.ServletException, java.io.IOException {

            boolean loginPost = "POST".equalsIgnoreCase(request.getMethod())
                    && request.getRequestURI().endsWith("/superadmin/login");

            if (loginPost && guard.isBlocked(request)) {
                response.sendRedirect(request.getContextPath()
                        + "/superadmin/login?locked=" + guard.minutesRemaining(request));
                return;
            }

            chain.doFilter(request, response);
        }
    }
}
