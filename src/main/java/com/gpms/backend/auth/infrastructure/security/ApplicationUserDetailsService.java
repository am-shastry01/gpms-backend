package com.gpms.backend.auth.infrastructure.security;

import com.gpms.backend.user.infrastructure.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ApplicationUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public ApplicationUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCaseAndDeletedFalse(username)
                .map(SecurityUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
    }
}
