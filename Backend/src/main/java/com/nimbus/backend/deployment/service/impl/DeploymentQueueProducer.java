package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.common.config.KafkaTopicConfig;
import com.nimbus.backend.deployment.dto.DeploymentTaskEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentQueueProducer {

    private final KafkaTemplate<String, DeploymentTaskEvent> kafkaTemplate;

    public void sendToBuildQueue(DeploymentTaskEvent event) {
        log.info("Publishing deployment task event to Kafka topic [ {} ] for Project UUID: {}",
                KafkaTopicConfig.DEPLOYMENT_BUILDS_TOPIC, event.getProjectUuid());

        // Push out the payload asynchronously using the Project UUID as the routing partition key
        kafkaTemplate.send(KafkaTopicConfig.DEPLOYMENT_BUILDS_TOPIC, event.getProjectUuid(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully appended task to partition [{}] at offset [{}] for deployment ID: {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                event.getDeploymentId());
                    } else {
                        log.error("Critical: Failed to append deployment task to Kafka broker queue cluster", ex);
                    }
                });
    }
}