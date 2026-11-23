package com.fleetops.flota.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Lectura GPS publicada en Kafka (topico fleetops.telemetria.gps, key = vehicleId). */
public record TelemetryStreamEvent(
    String eventId,
    String vehicleId,
    String tripId,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal speedKmh,
    BigDecimal fuelLevel,
    BigDecimal odometerKm,
    Instant recordedAt,
    String source
) {
    public static TelemetryStreamEvent of(TelemetryEventPayload p) {
        return new TelemetryStreamEvent(p.eventId(), p.vehicleId(), p.tripId(), p.latitude(), p.longitude(),
            p.speedKph(), p.fuelPercent(), p.odometerKm(), p.recordedAt(), p.source());
    }
}
