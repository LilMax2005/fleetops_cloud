package com.fleetops.flota.messaging.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Declara los topicos de negocio y sus Dead Letter Topics. KafkaAdmin los crea al arrancar si no
 * existen (idempotente); en Azure se puede desactivar con FLEETOPS_KAFKA_CREATE_TOPICS=false.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
@ConditionalOnProperty(name = "fleetops.kafka.create-topics", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    @Bean
    KafkaAdmin.NewTopics fleetOpsTopics(KafkaTopicsProperties topics) {
        return new KafkaAdmin.NewTopics(
            topic(topics.tripEvents()), dlt(topics.tripEvents()),
            topic(topics.telemetry()), dlt(topics.telemetry()));
    }

    private static NewTopic topic(KafkaTopicsProperties.TopicSpec spec) {
        return TopicBuilder.name(spec.name()).partitions(spec.partitions()).replicas(spec.replicas())
            .configs(spec.configs()).build();
    }

    private static NewTopic dlt(KafkaTopicsProperties.TopicSpec spec) {
        return TopicBuilder.name(spec.dlt()).partitions(1).replicas(spec.replicas())
            .config("retention.ms", String.valueOf(14L * 24 * 60 * 60 * 1000))
            .config("min.insync.replicas", spec.replicas() > 1 ? "2" : "1")
            .build();
    }
}
