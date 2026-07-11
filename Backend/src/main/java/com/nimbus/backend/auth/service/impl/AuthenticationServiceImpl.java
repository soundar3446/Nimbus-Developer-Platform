package com.nimbus.backend.auth.service.impl;

import com.nimbus.backend.auth.dto.AuthenticationResponse;
import com.nimbus.backend.auth.dto.LoginRequest;
import com.nimbus.backend.auth.dto.RegisterRequest;
import com.nimbus.backend.auth.jwt.JwtService;
import com.nimbus.backend.auth.service.AuthenticationService;
import com.nimbus.backend.common.exception.AlreadyExistsException;
import com.nimbus.backend.user.entity.User;
import com.nimbus.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService javaJwtService;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager; // Handles credential checks

    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        // 1. Enforce unique emails
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyExistsException("Email address is already registered: " + request.getEmail());
        }

        // 2. Build the User entity and securely hash the raw password
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // 🔥 Securely Hashed
                .build();

        userRepository.save(user);

        // 3. Automatically sign them in by generating a token upon registration completion
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String jwtToken = javaJwtService.generateToken(userDetails);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResponse login(LoginRequest request) {
        // 1. Delegate credential verification to Spring Security's AuthenticationManager
        // This automatically checks the password against the database via CustomUserDetailsService
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. If authentication succeeds, load the user details and mint a fresh JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String jwtToken = javaJwtService.generateToken(userDetails);

        // 3. Fetch the full user entity profile details for the response metadata
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

}
