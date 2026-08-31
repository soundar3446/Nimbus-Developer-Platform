package com.nimbus.backend.deployment.controller;

import com.nimbus.backend.deployment.service.KubernetesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@Slf4j
public class DeploymentLogStreamController {

    private final KubernetesService kubernetesService;

    @GetMapping(value = "/api/v1/deployments/{deploymentId}/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> attachLiveLogs(@PathVariable Long deploymentId) {
        String k8sDeploymentName = "nimbus-" + deploymentId;
        log.info("Client attached to live SSE log stream for deployment: {}", k8sDeploymentName);

        // Native reactive SSE stream pulling logs directly from Kubernetes
        return kubernetesService.streamPodLogs(k8sDeploymentName)
                .doOnCancel(() -> log.info("Client disconnected from SSE log stream for: {}", k8sDeploymentName))
                .doOnError(e -> log.error("SSE Log Stream failed for {}: {}", k8sDeploymentName, e.getMessage()));
    }
}