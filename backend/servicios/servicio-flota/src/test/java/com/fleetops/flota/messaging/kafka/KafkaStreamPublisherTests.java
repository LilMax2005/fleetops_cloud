package com.fleetops.flota.messaging.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.domain.TripStatus;
import com.fleetops.flota.messaging.dto.TelemetryEventPayload;
import com.fleetops.flota.messaging.dto.TelemetryStreamEvent;
import com.fleetops.flota.messaging.dto.TripStreamEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaStreamPublisherTests {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final KafkaStreamPublisher publisher =
        new KafkaStreamPublisher(kafka, "fleetops.viajes.eventos", "fleetops.telemetria.gps", true);

    @Test
    void publishesTripEventKeyedByTripId() {
        when(kafka.send(any(), any(), any())).thenReturn(new CompletableFuture<>());
        Trip trip = new Trip("trip-1", "Santiago", BigDecimal.ONE, BigDecimal.ONE, "Valparaiso", BigDecimal.TEN,
            BigDecimal.TEN, Instant.now(), Instant.now(), TripStatus.INCIDENT, 40, BigDecimal.TEN, BigDecimal.ONE,
            "veh-1", "KTRZ21", "drv-1", "Ana", Instant.now(), Instant.now());

        publisher.tripEvent("INCIDENT", trip);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq("fleetops.viajes.eventos"), eq("trip-1"), event.capture());
        TripStreamEvent sent = (TripStreamEvent) event.getValue();
        assertEquals("INCIDENT", sent.eventType());
        assertEquals("KTRZ21", sent.vehiclePlate());
        assertEquals("INCIDENT", sent.status());
    }

    @Test
    void publishesTelemetryKeyedByVehicleAndSurvivesBrokerErrors() {
        when(kafka.send(any(), any(), any())).thenThrow(new IllegalStateException("broker caido"));
        var payload = new TelemetryEventPayload("evt-1", "veh-1", "trip-1", BigDecimal.ONE, BigDecimal.ONE,
            new BigDecimal("95"), new BigDecimal("40"), BigDecimal.TEN, Instant.now(), "test");

        publisher.telemetry(payload);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq("fleetops.telemetria.gps"), eq("veh-1"), event.capture());
        assertEquals(new BigDecimal("95"), ((TelemetryStreamEvent) event.getValue()).speedKmh());
    }

    @Test
    void disabledPublisherDoesNotTouchKafka() {
        var disabled = new KafkaStreamPublisher(kafka, "a", "b", false);
        disabled.telemetry(new TelemetryEventPayload("evt-2", "veh-1", null, BigDecimal.ONE, BigDecimal.ONE,
            BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, Instant.now(), "test"));
        org.mockito.Mockito.verifyNoInteractions(kafka);
    }
}
