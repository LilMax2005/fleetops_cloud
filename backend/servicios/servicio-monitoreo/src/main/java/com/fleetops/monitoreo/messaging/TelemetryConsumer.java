package com.fleetops.monitoreo.messaging;

import com.fleetops.common.events.KafkaTopics;
import com.fleetops.common.events.TelemetryEvent;
import com.fleetops.common.kafka.EventValidator;
import com.fleetops.monitoreo.service.AnalyticsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer group "monitoreo-telemetria" con 3 hilos: las 6 particiones del tópico se reparten
 * 2 por hilo. Si se escala servicio-monitoreo a más réplicas en Container Apps, Kafka rebalancea
 * las particiones entre todas las instancias del mismo grupo.
 */
@Component
public class TelemetryConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryConsumer.class);
    private final EventValidator validator;
    private final AnalyticsService analytics;

    public TelemetryConsumer(EventValidator validator, AnalyticsService analytics) {
        this.validator = validator;
        this.analytics = analytics;
    }

    @KafkaListener(id = "monitoreo-telemetria-listener", topics = KafkaTopics.TELEMETRY,
            groupId = "${fleetops.kafka.groups.telemetry:monitoreo-telemetria}", concurrency = "3",
            properties = "spring.json.value.default.type=com.fleetops.common.events.TelemetryEvent")
    public void onTelemetry(ConsumerRecord<String, TelemetryEvent> record) {
        TelemetryEvent event = validator.validate(record.value());
        log.debug("Telemetría {} {} km/h partition={} offset={}", event.vehicleId(), event.speedKmh(),
                record.partition(), record.offset());
        analytics.processTelemetry(event).forEach(alert -> log.warn("alerta_analitica tipo={} severidad={} vehiculo={} viaje={} detalle={}",
                alert.type(), alert.severity(), alert.vehicleId(), alert.tripId(), alert.description()));
    }
}
