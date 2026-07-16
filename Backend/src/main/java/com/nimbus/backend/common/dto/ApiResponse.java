package com.nimbus.backend.common.dto;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {
    public ApiResponse(boolean success, String message, T data) {
        this(success, message, data, Instant.now());
    }
}