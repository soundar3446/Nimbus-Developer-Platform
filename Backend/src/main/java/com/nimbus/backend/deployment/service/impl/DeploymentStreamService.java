package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.deployment.dto.DeploymentProgressEvent;
import com.nimbus.backend.deployment.enums.DeploymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentStreamService {

    private final SimpMessagingTemplate messagingTemplate;

    public void streamProgress(Long deploymentId, String projectUuid, DeploymentStatus status, int progressPercentage, String logLine) {
        String destination = "/topic/deployments/" + deploymentId;

        DeploymentProgressEvent event = DeploymentProgressEvent.builder()
                .deploymentId(deploymentId)
                .projectUuid(projectUuid)
                .status(status)
                .progressPercentage(progressPercentage)
                .logLine(logLine)
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend(destination, event);
    }
}