package com.fleetops.flota.messaging.kafka;

import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.messaging.dto.TelemetryEventPayload;
import com.fleetops.flota.messaging.dto.TelemetryStreamEvent;
import com.fleetops.flota.messaging.dto.TripStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Productor Kafka de servicio-flota. Publica despues del commit para no emitir eventos de cambios
 * que se revirtieron; un fallo de Kafka se registra y no rompe el flujo transaccional (RabbitMQ y
 * PostgreSQL siguen siendo la fuente de verdad del dominio).
 */
@Component
public class KafkaStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaStreamPublisher.class);

    private final KafkaTemplate<String, Object> kafka;
    private final String tripTopic;
    private final String telemetryTopic;
    private final boolean enabled;

    public KafkaStreamPublisher(KafkaTemplate<String, Object> kafka,
                                @Value("${fleetops.kafka.topics.trip-events.name}") String tripTopic,
                                @Value("${fleetops.kafka.topics.telemetry.name}") String telemetryTopic,
                                @Value("${fleetops.kafka.enabled:true}") boolean enabled) {
        this.kafka = kafka;
        this.tripTopic = tripTopic;
        this.telemetryTopic = telemetryTopic;
        this.enabled = enabled;
    }

    public void tripEvent(String eventType, Trip trip) {
        TripStreamEvent event = TripStreamEvent.of(eventType, trip);
        afterCommit(() -> send(tripTopic, event.tripId(), event, event.eventId()));
    }

    public void telemetry(TelemetryEventPayload payload) {
        TelemetryStreamEvent event = TelemetryStreamEvent.of(payload);
        afterCommit(() -> send(telemetryTopic, event.vehicleId(), event, event.eventId()));
    }

    private void send(String topic, String key, Object event, String eventId) {
        if (!enabled) {
            return;
        }
        try {
            kafka.send(topic, key, event).whenComplete((result, error) -> {
                if (error != null) {
                    log.error("kafka_publish_failed topic={} key={} eventId={}", topic, key, eventId, error);
                } else {
                    log.info("kafka_published topic={} partition={} offset={} key={} eventId={}", topic,
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset(), key, eventId);
                }
            });
        } catch (RuntimeException exception) {
            log.error("kafka_publish_failed topic={} key={} eventId={}", topic, key, eventId, exception);
        }
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
