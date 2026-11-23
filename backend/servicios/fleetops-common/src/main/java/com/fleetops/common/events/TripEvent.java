package com.fleetops.common.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de ciclo de vida de un viaje publicado por servicio-flota en fleetops.viajes.eventos
 * (key Kafka = tripId: todos los eventos de un viaje caen en la misma particion y llegan en orden).
 */
public record TripEvent(
        @NotBlank String eventId,
        @NotNull TripEventType eventType,
        @NotBlank String tripId,
        String vehicleId,
        String vehiclePlate,
        String driverName,
        String origin,
        String destination,
        @NotBlank String status,
        @NotNull Instant occurredAt
) {
    public static TripEvent of(TripEventType type, String tripId, String vehicleId, String plate, String driver,
                               String origin, String destination, String status) {
        return new TripEvent(UUID.randomUUID().toString(), type, tripId, vehicleId, plate, driver, origin,
                destination, status, Instant.now());
    }
}
