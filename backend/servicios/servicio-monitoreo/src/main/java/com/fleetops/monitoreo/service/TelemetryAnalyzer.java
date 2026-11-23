package com.fleetops.monitoreo.service;

import com.fleetops.common.events.AlertMessage;
import com.fleetops.common.events.TelemetryEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Reglas puras de detección (sin Kafka ni BD): fácil de probar de forma unitaria. */
@Component
public class TelemetryAnalyzer {

    private final MonitoringRulesProperties rules;

    public TelemetryAnalyzer(MonitoringRulesProperties rules) {
        this.rules = rules;
    }

    public List<AlertMessage> analyze(TelemetryEvent e) {
        List<AlertMessage> alerts = new ArrayList<>();
        if (e.speedKmh() > rules.speedLimitKmh()) {
            alerts.add(alert(e, "SPEEDING", e.speedKmh() > rules.speedLimitKmh() * 1.2 ? "HIGH" : "MEDIUM",
                    "Exceso de velocidad: %.0f km/h (límite %.0f)".formatted(e.speedKmh(), rules.speedLimitKmh())));
        }
        if (e.fuelLevel() != null && e.fuelLevel() < rules.lowFuelPercent()) {
            alerts.add(alert(e, "LOW_FUEL", "MEDIUM", "Combustible bajo: %.0f%%".formatted(e.fuelLevel())));
        }
        return alerts;
    }

    private static AlertMessage alert(TelemetryEvent e, String type, String severity, String description) {
        return new AlertMessage(UUID.randomUUID(), e.tripId(), e.vehicleId(), type, severity, description, Instant.now());
    }
}
