package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.deployment.dto.DeploymentTaskEvent;
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

    @KafkaListener(topics = com.nimbus.backend.common.config.KafkaTopicConfig.DEPLOYMENT_BUILDS_TOPIC, groupId = "nimbus-deployment-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeDeploymentTask(DeploymentTaskEvent event) {
        log.info("➔ [KAFKA CONSUMER] Dequeued task tracking ID [{}] for compilation workflow.", event.getDeploymentId());

        try {
            //  Heavy operations execution: Git Clone -> Docker compilation -> Custom OkHttp Patch Transaction
            deploymentService.executeClusterWorkload(event);

            log.info(" [KAFKA CONSUMER] Workload successfully pushed out for deployment ID: {}", event.getDeploymentId());

        } catch (Exception e) {
            log.error(" [KAFKA CONSUMER] Fatal processing crash encountered inside background build pipeline", e);
        }
    }
}