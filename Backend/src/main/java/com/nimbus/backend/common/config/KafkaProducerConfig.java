package com.nimbus.backend.common.config;

import com.nimbus.backend.deployment.dto.DeploymentTaskEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    public ProducerFactory<String, DeploymentTaskEvent> producerFactory() {

        Map<String, Object> configProps =
                new HashMap<>(kafkaProperties.buildProducerProperties());

        configProps.putIfAbsent(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "kafka:29092");

        configProps.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);

        configProps.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JacksonJsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, DeploymentTaskEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}