package com.nimbus.backend.deployment.controller;

import com.nimbus.backend.deployment.service.impl.DeploymentStreamService;
import com.nimbus.backend.deployment.service.KubernetesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.scheduling.annotation.Async;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DeploymentLogStreamController {

    private final KubernetesService kubernetesService;
    private final DeploymentStreamService deploymentStreamService;

    @Async("taskExecutor")
    @MessageMapping("/deployments/{deploymentId}/logs/attach")
    public void attachLiveLogs(@DestinationVariable Long deploymentId) {
        String k8sDeploymentName = "nimbus-" + deploymentId;
        log.info("Client attached to live log stream for deployment: {}", k8sDeploymentName);

        kubernetesService.streamPodLogs(k8sDeploymentName, logLine -> {
            deploymentStreamService.streamProgress(
                    deploymentId,
                    null,
                    null,
                    100,
                    logLine
            );
        });
    }
}