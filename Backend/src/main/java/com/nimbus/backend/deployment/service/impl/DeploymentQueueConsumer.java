package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.deployment.dto.DeploymentTaskEvent;
import com.nimbus.backend.deployment.enums.DeploymentStatus;
import com.nimbus.backend.deployment.repository.DeploymentRepository;
import com.nimbus.backend.deployment.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeploymentQueueConsumer {

    private final DeploymentService deploymentService;
    private final DeploymentRepository deploymentRepository;

    @KafkaListener(topics = "nimbus-deployment-builds", groupId = "nimbus-deployment-group")
    public void consumeDeploymentTask(DeploymentTaskEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("➔ [KAFKA CONSUMER] Dequeued task tracking ID [{}] for compilation workflow.", event.getDeploymentId());

        // Update operational status tracking from QUEUED to RUNNING in database
        updateStatus(event.getDeploymentId(), DeploymentStatus.RUNNING, null);

        try {
            //  Heavy operations execution: Git Clone -> Docker compilation -> Custom OkHttp Patch Transaction
            deploymentService.executeClusterWorkload(event);

            long duration = System.currentTimeMillis() - startTime;

            // Mark deployment as completely healthy and log telemetry metrics (BIGINT ms support!)
            updateStatus(event.getDeploymentId(), DeploymentStatus.SUCCESSFUL, duration);
            log.info(" [KAFKA CONSUMER] Workload successfully pushed out for deployment ID: {}", event.getDeploymentId());

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error(" [KAFKA CONSUMER] Fatal processing crash encountered inside background build pipeline", e);

            // Fault isolation: update database status to FAILED so dashboard updates gracefully
            updateStatus(event.getDeploymentId(), DeploymentStatus.FAILED, duration);
        }
    }

    private void updateStatus(Long id, DeploymentStatus status, Long durationMs) {
        deploymentRepository.findById(id).ifPresent(deployment -> {
            deployment.setStatus(status);
            if (durationMs != null) {
                deployment.setDurationMs(durationMs);
            }
            deploymentRepository.save(deployment);
        });
    }
}