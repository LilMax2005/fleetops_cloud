package com.fleetops.common.events;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/**
 * Lectura GPS publicada por servicio-flota en fleetops.telemetria.gps (key Kafka = vehicleId:
 * orden por vehiculo y balanceo entre las 6 particiones). Mismo esquema que TelemetryStreamEvent.
 */
public record TelemetryEvent(
        @NotBlank String eventId,
        @NotBlank String vehicleId,
        String tripId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull @PositiveOrZero @DecimalMax("300.0") Double speedKmh,
        @DecimalMin("0.0") @DecimalMax("100.0") Double fuelLevel,
        @PositiveOrZero Double odometerKm,
        @NotNull Instant recordedAt,
        String source
) {
}
