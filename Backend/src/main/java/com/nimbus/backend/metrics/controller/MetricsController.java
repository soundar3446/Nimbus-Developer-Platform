package com.nimbus.backend.metrics.controller;

import com.nimbus.backend.metrics.dto.PodMetricsResponse;
import com.nimbus.backend.metrics.service.PrometheusMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final PrometheusMetricsService prometheusMetricsService;

    @GetMapping("/deployments/{deploymentId}")
    public ResponseEntity<PodMetricsResponse> getDeploymentMetrics(
            @PathVariable Long deploymentId,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end) {

        long now = Instant.now().getEpochSecond();
        long endEpoch = (end != null) ? end : now;
        long startEpoch = (start != null) ? start : now - 3600; // Default to last 1 hour

        String k8sDeploymentName = "nimbus-" + deploymentId;
        PodMetricsResponse metrics = prometheusMetricsService.getPodMetrics(k8sDeploymentName, startEpoch, endEpoch);

        return ResponseEntity.ok(metrics);
    }
}