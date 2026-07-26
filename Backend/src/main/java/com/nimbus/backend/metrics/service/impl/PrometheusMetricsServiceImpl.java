package com.nimbus.backend.metrics.service.impl;

import com.nimbus.backend.metrics.dto.PodMetricsResponse;
import com.nimbus.backend.metrics.dto.PodMetricsResponse.MetricDataPoint;
import com.nimbus.backend.metrics.service.PrometheusMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrometheusMetricsServiceImpl implements PrometheusMetricsService {

   private final RestTemplate restTemplate = new RestTemplate();

    @Value("${prometheus.url:http://host.docker.internal:9090}")
    private String prometheusUrl;

    @Override
    public PodMetricsResponse getPodMetrics(String deploymentName, long startEpochSec, long endEpochSec) {

       String formattedPrefix = deploymentName.startsWith("nimbus-") ? deploymentName : "nimbus-" + deploymentName;

        // CPU usage rate per pod (Cores) using irate with 2m window
        String cpuQuery = String.format(
            "sum(irate(container_cpu_usage_seconds_total{pod=~\"%s-.*\",container!=\"\",container!=\"POD\"}[2m]))",
            formattedPrefix);

        // Memory usage per pod in MB
        String memoryQuery = String.format(
            "sum(container_memory_working_set_bytes{pod=~\"%s-.*\",container!=\"\",container!=\"POD\"}) / 1024 / 1024",
            formattedPrefix);

        List<MetricDataPoint> cpuData = queryPrometheusRange(cpuQuery, startEpochSec, endEpochSec, "15s");
        List<MetricDataPoint> memoryData = queryPrometheusRange(memoryQuery, startEpochSec, endEpochSec, "15s");

        return PodMetricsResponse.builder()
                .deploymentName(deploymentName)
                .cpuUsageHistory(cpuData)
                .memoryUsageHistory(memoryData)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<MetricDataPoint> queryPrometheusRange(String query, long start, long end, String step) {
        List<MetricDataPoint> points = new ArrayList<>();
        try {
            URI targetUri = UriComponentsBuilder.fromUriString(prometheusUrl)
                    .path("/api/v1/query_range")
                    .queryParam("query", query)
                    .queryParam("start", start)
                    .queryParam("end", end)
                    .queryParam("step", step)
                    .build()
                    .encode()
                    .toUri();

            Map<String, Object> response = restTemplate.getForObject(targetUri, Map.class);
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