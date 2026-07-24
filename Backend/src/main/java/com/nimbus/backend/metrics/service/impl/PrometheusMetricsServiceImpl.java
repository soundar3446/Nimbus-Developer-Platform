package com.nimbus.backend.metrics.service.impl;

import com.nimbus.backend.metrics.dto.PodMetricsResponse;
import com.nimbus.backend.metrics.dto.PodMetricsResponse.MetricDataPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbus.backend.metrics.service.PrometheusMetricsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrometheusMetricsServiceImpl implements PrometheusMetricsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${prometheus.server.url:http://prometheus-k8s.monitoring.svc.cluster.local:9090}")
    private String prometheusUrl;

    public PodMetricsResponse getPodMetrics(String deploymentName, long startEpochSec, long endEpochSec) {
        // PromQL Query 1: CPU usage rate per pod
        String cpuQuery = String.format("sum(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~\"%s-.*\"})", deploymentName);

        // PromQL Query 2: Memory usage per pod in MB
        String memoryQuery = String.format("sum(container_memory_working_set_bytes{pod=~\"%s-.*\"}) / 1024 / 1024", deploymentName);

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
                List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");

                if (!result.isEmpty()) {
                    List<List<Object>> values = (List<List<Object>>) result.get(0).get("values");
                    for (List<Object> val : values) {
                        long timestamp = ((Number) val.get(0)).longValue();
                        double value = Double.parseDouble(val.get(1).toString());
                        points.add(new MetricDataPoint(timestamp, value));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to query Prometheus metrics for query: {}", query, e);
        }
        return points;
    }

}
