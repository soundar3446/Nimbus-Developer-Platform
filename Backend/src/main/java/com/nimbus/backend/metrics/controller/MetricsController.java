package com.nimbus.backend.metrics.controller;

import com.nimbus.backend.metrics.dto.PodMetricsResponse;
import com.nimbus.backend.metrics.service.PrometheusMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import com.nimbus.backend.common.dto.ApiResponse;
import com.nimbus.backend.common.exception.ResourceNotFoundException;
import com.nimbus.backend.deployment.entity.Deployment;
import com.nimbus.backend.deployment.repository.DeploymentRepository;
import com.nimbus.backend.auth.service.CurrentUserService;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final PrometheusMetricsService prometheusMetricsService;

    @GetMapping("/deployments/{deploymentId}")
    public ResponseEntity<ApiResponse<PodMetricsResponse>> getDeploymentMetrics(
            @PathVariable Long deploymentId,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end) {

        PodMetricsResponse metrics = prometheusMetricsService.getPodMetrics(deploymentId, start, end);

        ApiResponse<PodMetricsResponse> apiResponse = new ApiResponse<>(
                true,
                "Metrics retrieved successfully",
                metrics
        );

        return ResponseEntity.ok(apiResponse);
    }
}