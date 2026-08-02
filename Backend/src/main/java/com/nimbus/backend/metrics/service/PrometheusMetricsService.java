package com.nimbus.backend.metrics.service;

import com.nimbus.backend.metrics.dto.PodMetricsResponse;

public interface PrometheusMetricsService {

    PodMetricsResponse getPodMetrics(Long deploymentId, Long startEpochSec, Long endEpochSec);

}
