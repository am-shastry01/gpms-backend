package com.gpms.backend.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gpms.backend.config.JwtProperties;
import com.gpms.backend.user.domain.Role;
import com.gpms.backend.user.domain.User;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void shouldGenerateAndValidateToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setAccessTokenMinutes(15);
        JwtService jwtService = new JwtService(properties);

        Role role = new Role();
        role.setCode("EMPLOYEE");

        User user = new User();
        user.setUsername("employee");
        user.setPasswordHash("encoded");
        user.setRoles(Set.of(role));

        String token = jwtService.generateToken(user);

        assertEquals("employee", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, "employee"));
    }
}
