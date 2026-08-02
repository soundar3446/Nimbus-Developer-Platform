package com.nimbus.backend.metrics.service.impl;

import com.nimbus.backend.auth.service.CurrentUserService;
import com.nimbus.backend.common.exception.ResourceNotFoundException;
import com.nimbus.backend.deployment.entity.Deployment;
import com.nimbus.backend.deployment.repository.DeploymentRepository;
import com.nimbus.backend.metrics.dto.PodMetricsResponse;
import com.nimbus.backend.metrics.dto.PodMetricsResponse.MetricDataPoint;
import com.nimbus.backend.metrics.service.PrometheusMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import jakarta.annotation.PostConstruct;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrometheusMetricsServiceImpl implements PrometheusMetricsService {

    private final DeploymentRepository deploymentRepository;
    private final CurrentUserService currentUserService;

   private RestClient restClient;

    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder().baseUrl(prometheusUrl).build();
    }

    @Value("${prometheus.url:http://host.docker.internal:9090}")
    private String prometheusUrl;

    @Override
    public PodMetricsResponse getPodMetrics(Long deploymentId, Long startEpochSec, Long endEpochSec) {

        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment footprint target not found"));
        
        if (!deployment.getProject().getOwner().getEmail().equals(currentUserService.getCurrentUserEmail())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to access this deployment's metrics.");
        }

        long now = Instant.now().getEpochSecond();
        long endEpoch = (endEpochSec != null) ? endEpochSec : now;
        long startEpoch = (startEpochSec != null) ? startEpochSec : now - 3600; // Default to last 1 hour

        String deploymentName = "nimbus-" + deploymentId;
       String formattedPrefix = deploymentName;

        // CPU usage rate per pod (Cores) using rate with 2m window
        String cpuQuery = String.format(
            "sum(rate(container_cpu_usage_seconds_total{pod=~\"%s-.*\",container!=\"\",container!=\"POD\"}[2m]))",
            formattedPrefix);

        // Memory usage per pod in MB
        String memoryQuery = String.format(
            "sum(container_memory_working_set_bytes{pod=~\"%s-.*\",container!=\"\",container!=\"POD\"}) / 1024 / 1024",
            formattedPrefix);

        // Network I/O (bytes/sec)
        String networkQuery = String.format(
            "sum(rate(container_network_receive_bytes_total{pod=~\"%s-.*\",container!=\"\",container!=\"POD\"}[2m])) + sum(rate(container_network_transmit_bytes_total{pod=~\"%s-.*\",container!=\"\",container!=\"POD\"}[2m]))",
            formattedPrefix, formattedPrefix);

        List<MetricDataPoint> cpuData = queryPrometheusRange(cpuQuery, startEpoch, endEpoch, "15s");
        List<MetricDataPoint> memoryData = queryPrometheusRange(memoryQuery, startEpoch, endEpoch, "15s");
        List<MetricDataPoint> networkIoData = queryPrometheusRange(networkQuery, startEpoch, endEpoch, "15s");

        return PodMetricsResponse.builder()
                .deploymentName(deploymentName)
                .cpuUsageHistory(cpuData)
                .memoryUsageHistory(memoryData)
                .networkIoHistory(networkIoData)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<MetricDataPoint> queryPrometheusRange(String query, long start, long end, String step) {
        List<MetricDataPoint> points = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/query_range")
                            .queryParam("query", query)
                            .queryParam("start", start)
                            .queryParam("end", end)
                            .queryParam("step", step)
                            .build())
                    .retrieve()
                    .body(Map.class);
            if (response != null && "success".equals(response.get("status"))) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");

                    if (result != null && !result.isEmpty()) {
                        List<List<Object>> values = (List<List<Object>>) result.get(0).get("values");
                        if (values != null) {
                            for (List<Object> val : values) {
                                long timestamp = ((Number) val.get(0)).longValue();
                                String rawValue = val.get(1).toString();
                                
                                double value = 0.0;
                                try {
                                    value = Double.parseDouble(rawValue);
                                    if (Double.isNaN(value) || Double.isInfinite(value)) {
                                        value = 0.0;
                                    }
                                } catch (NumberFormatException e) {
                                    log.warn("Failed to parse metric value '{}', defaulting to 0.0", rawValue);
                                }
                                
                                points.add(new MetricDataPoint(timestamp, value));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to query Prometheus metrics for query: {}", query, e);
        }
        return points;
    }
}