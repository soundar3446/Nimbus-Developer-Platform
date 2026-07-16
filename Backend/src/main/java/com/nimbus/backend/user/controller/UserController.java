package com.nimbus.backend.user.controller;

import com.nimbus.backend.common.dto.ApiResponse;
import com.nimbus.backend.user.dto.UserRequest;
import com.nimbus.backend.user.dto.UserResponse;
import com.nimbus.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>(
                true,
                "User created successfully",
                response
        );

        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        UserResponse response = userService.getUser(id);

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>(
                true,
                "User retrieved successfully",
                response
        );

        return ResponseEntity.ok(apiResponse);
    }
}