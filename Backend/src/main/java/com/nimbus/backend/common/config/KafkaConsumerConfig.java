package com.nimbus.backend.common.config;

import com.nimbus.backend.deployment.dto.DeploymentTaskEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, DeploymentTaskEvent> consumerFactory() {

        Map<String, Object> props =
                new HashMap<>(kafkaProperties.buildConsumerProperties());

        // Default bootstrap server if not configured
        props.putIfAbsent(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "kafka:29092"
        );

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "nimbus-deployment-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Error handling deserializers
        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);

        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);

        props.put(
                ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS,
                StringDeserializer.class);

        props.put(
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JacksonJsonDeserializer.class);

        // JSON configuration
        props.put(
                JacksonJsonDeserializer.VALUE_DEFAULT_TYPE,
                DeploymentTaskEvent.class.getName());

        props.put(
                JacksonJsonDeserializer.TRUSTED_PACKAGES,
                "*");

        props.put(
                JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS,
                false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeploymentTaskEvent> kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, DeploymentTaskEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        return factory;
    }
}