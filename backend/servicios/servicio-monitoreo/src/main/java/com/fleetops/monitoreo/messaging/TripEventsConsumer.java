package com.fleetops.monitoreo.messaging;

import com.fleetops.common.events.KafkaTopics;
import com.fleetops.common.events.TripEvent;
import com.fleetops.common.kafka.EventValidator;
import com.fleetops.monitoreo.service.AnalyticsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumer group "monitoreo-viajes": construye métricas por viaje y alerta retrasos/incidentes. */
@Component
public class TripEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(TripEventsConsumer.class);
    private final EventValidator validator;
    private final AnalyticsService analytics;

    public TripEventsConsumer(EventValidator validator, AnalyticsService analytics) {
        this.validator = validator;
        this.analytics = analytics;
    }

    @KafkaListener(id = "monitoreo-viajes-listener", topics = KafkaTopics.TRIP_EVENTS,
            groupId = "${fleetops.kafka.groups.trips:monitoreo-viajes}", concurrency = "3",
            properties = "spring.json.value.default.type=com.fleetops.common.events.TripEvent")
    public void onTripEvent(ConsumerRecord<String, TripEvent> record) {
        TripEvent event = validator.validate(record.value());
        log.info("Viaje {} -> {} partition={} offset={}", event.tripId(), event.eventType(),
                record.partition(), record.offset());
        analytics.processTripEvent(event).forEach(alert -> log.warn("alerta_analitica tipo={} severidad={} vehiculo={} viaje={} detalle={}",
                alert.type(), alert.severity(), alert.vehicleId(), alert.tripId(), alert.description()));
    }
}
