package com.fleetops.flota.messaging.kafka;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracion de los topicos Kafka que crea servicio-flota (particiones, replicas y configs). */
@ConfigurationProperties(prefix = "fleetops.kafka.topics")
public record KafkaTopicsProperties(TopicSpec tripEvents, TopicSpec telemetry) {

    public record TopicSpec(String name, int partitions, short replicas, Map<String, String> configs) {
        public TopicSpec {
            configs = configs == null ? Map.of() : Map.copyOf(configs);
        }

        public String dlt() {
            return name + ".DLT";
        }
    }
}
