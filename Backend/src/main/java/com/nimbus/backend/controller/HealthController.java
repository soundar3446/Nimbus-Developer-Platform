package com.nimbus.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import com.nimbus.backend.common.dto.ApiResponse;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkHealth() {
        Map<String, String> data = new HashMap<>();
        data.put("status", "UP");
        data.put("message", "Nimbus Backend is running");

        ApiResponse<Map<String, String>> response = new ApiResponse<>(true, "Service is healthy", data);
        return ResponseEntity.ok(response);
    }
}