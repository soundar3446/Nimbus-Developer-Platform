package com.nimbus.backend.auth.service;

import com.nimbus.backend.auth.dto.AuthenticationResponse;
import com.nimbus.backend.auth.dto.LoginRequest;
import com.nimbus.backend.auth.dto.RegisterRequest;

public interface AuthenticationService {
    AuthenticationResponse register(RegisterRequest request);
    AuthenticationResponse login(LoginRequest request);
}