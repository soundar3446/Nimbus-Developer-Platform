package com.nimbus.backend.auth.controller;

import com.nimbus.backend.auth.dto.AuthenticationResponse;
import com.nimbus.backend.auth.dto.LoginRequest;
import com.nimbus.backend.auth.dto.RegisterRequest;
import com.nimbus.backend.auth.service.AuthenticationService;
import com.nimbus.backend.common.dto.ApiResponse;
import com.nimbus.backend.user.dto.UserResponse;
import com.nimbus.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    /**
     * POST /api/auth/register
     * Registers a new user and returns an access token.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthenticationResponse response = authenticationService.register(request);
        ApiResponse<AuthenticationResponse> apiResponse = new ApiResponse<>(
                true,
                "User registered successfully",
                response
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    /**
     * POST /api/auth/login
     * Authenticates credentials and returns a valid access token.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationResponse response = authenticationService.login(request);
        ApiResponse<AuthenticationResponse> apiResponse = new ApiResponse<>(
                true,
                "Authentication successful",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * GET /api/auth/me
     * Fetches the current authenticated user's profile info using the active token session.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        // userDetails.getUsername() retrieves the email injected into the SecurityContext by the JWT Filter
        UserResponse response = userService.getUserByEmail(userDetails.getUsername());

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>(
                true,
                "Current user profile retrieved successfully",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }
}
