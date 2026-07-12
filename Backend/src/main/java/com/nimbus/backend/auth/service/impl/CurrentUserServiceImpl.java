package com.nimbus.backend.auth.service.impl;

import com.nimbus.backend.auth.service.CurrentUserService;
import com.nimbus.backend.common.exception.ResourceNotFoundException;
import com.nimbus.backend.user.entity.User;
import com.nimbus.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user profile no longer exists."));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getCurrentUserOptional() {
        try {
            return Optional.of(getCurrentUser());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("No active security context session discovered.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof String principalString) {
            return principalString; // Fallback handles simple string principal variants
        }

        throw new IllegalStateException("Unexpected security context principal structural type.");
    }
}