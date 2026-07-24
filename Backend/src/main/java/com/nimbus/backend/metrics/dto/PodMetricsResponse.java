package com.nimbus.backend.metrics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodMetricsResponse {

    private String deploymentName;
    private List<MetricDataPoint> cpuUsageHistory;    // CPU usage in cores/millicores over time
    private List<MetricDataPoint> memoryUsageHistory; // Memory usage in Megabytes over time
    private List<MetricDataPoint> networkIHistory;     // Network In/Out bandwidth

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetricDataPoint {
        private long timestamp;
        private double value;
    }
}