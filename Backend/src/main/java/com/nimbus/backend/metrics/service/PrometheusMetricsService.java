package com.nimbus.backend.metrics.service;

import com.nimbus.backend.metrics.dto.PodMetricsResponse;

public interface PrometheusMetricsService {

    PodMetricsResponse getPodMetrics(String deploymentName, long startEpochSec, long endEpochSec);

}
