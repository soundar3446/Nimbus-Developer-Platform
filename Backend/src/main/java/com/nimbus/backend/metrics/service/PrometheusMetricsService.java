package com.nimbus.backend.metrics.service;

import com.nimbus.backend.metrics.dto.PodMetricsResponse;
import reactor.core.publisher.Mono;

public interface PrometheusMetricsService {

    Mono<PodMetricsResponse> getPodMetrics(Long deploymentId, Long startEpochSec, Long endEpochSec);

}
