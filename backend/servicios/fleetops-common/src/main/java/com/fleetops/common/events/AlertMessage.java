package com.fleetops.common.events;

import java.time.Instant;
import java.util.UUID;

/** Alerta analitica detectada por servicio-monitoreo a partir del streaming de Kafka. */
public record AlertMessage(
        UUID alertId,
        String tripId,
        String vehicleId,
        String type,
        String severity,
        String description,
        Instant createdAt
) {
}
