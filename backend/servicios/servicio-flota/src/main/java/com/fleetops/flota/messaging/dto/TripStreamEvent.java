package com.fleetops.flota.messaging.dto;

import com.fleetops.flota.domain.Trip;
import java.time.Instant;
import java.util.UUID;

/** Evento de ciclo de vida de un viaje publicado en Kafka (topico fleetops.viajes.eventos, key = tripId). */
public record TripStreamEvent(
    String eventId,
    String eventType,
    String tripId,
    String vehicleId,
    String vehiclePlate,
    String driverName,
    String origin,
    String destination,
    String status,
    Instant occurredAt
) {
    public static TripStreamEvent of(String eventType, Trip trip) {
        return new TripStreamEvent(UUID.randomUUID().toString(), eventType, trip.getId(), trip.getVehicleId(),
            trip.getVehiclePlate(), trip.getDriverName(), trip.getOriginName(), trip.getDestinationName(),
            trip.getStatus().name(), Instant.now());
    }
}
