package com.fleetops.monitoreo.service;

import com.fleetops.common.events.AlertMessage;
import com.fleetops.common.events.TelemetryEvent;
import com.fleetops.common.events.TripEvent;
import com.fleetops.monitoreo.domain.ProcessedEvent;
import com.fleetops.monitoreo.domain.TripMetric;
import com.fleetops.monitoreo.domain.VehicleStats;
import com.fleetops.monitoreo.repository.ProcessedEventRepository;
import com.fleetops.monitoreo.repository.TripMetricRepository;
import com.fleetops.monitoreo.repository.VehicleStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Lógica de analítica: independiente de Kafka (los consumidores solo traducen y delegan). */
@Service
public class AnalyticsService {

    private final VehicleStatsRepository vehicleStats;
    private final TripMetricRepository tripMetrics;
    private final ProcessedEventRepository processed;
    private final TelemetryAnalyzer analyzer;

    public AnalyticsService(VehicleStatsRepository vehicleStats, TripMetricRepository tripMetrics,
                            ProcessedEventRepository processed, TelemetryAnalyzer analyzer) {
        this.vehicleStats = vehicleStats;
        this.tripMetrics = tripMetrics;
        this.processed = processed;
        this.analyzer = analyzer;
    }

    /** @return alertas detectadas (vacía si el evento ya había sido procesado). */
    @Transactional
    public List<AlertMessage> processTelemetry(TelemetryEvent e) {
        if (alreadyProcessed(e.eventId())) {
            return List.of();
        }
        VehicleStats stats = vehicleStats.findById(e.vehicleId()).orElseGet(() -> new VehicleStats(e.vehicleId()));
        stats.register(e.speedKmh(), e.fuelLevel(), e.recordedAt());
        List<AlertMessage> alerts = analyzer.analyze(e);
        stats.addAlerts(alerts.size());
        vehicleStats.save(stats);
        return alerts;
    }

    /** @return alerta operacional si el viaje entra en retraso o incidente. */
    @Transactional
    public List<AlertMessage> processTripEvent(TripEvent e) {
        if (alreadyProcessed(e.eventId())) {
            return List.of();
        }
        TripMetric metric = tripMetrics.findById(e.tripId())
                .orElseGet(() -> new TripMetric(e.tripId(), e.vehicleId(), e.vehiclePlate()));
        metric.apply(e.eventType().name(), e.status(), e.occurredAt());
        tripMetrics.save(metric);
        return switch (e.eventType()) {
            case DELAYED -> List.of(tripAlert(e, "TRIP_DELAYED", "MEDIUM", "Viaje " + e.tripId() + " reporta retraso"));
            case INCIDENT -> List.of(tripAlert(e, "TRIP_INCIDENT", "HIGH", "Incidente en viaje " + e.tripId()
                    + " (" + e.origin() + " → " + e.destination() + ")"));
            default -> List.of();
        };
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        List<VehicleStats> all = vehicleStats.findAll();
        return Map.of(
                "tripsTracked", tripMetrics.count(),
                "tripsEnRoute", tripMetrics.countByStatus("IN_TRANSIT"),
                "tripsCompleted", tripMetrics.countByStatus("COMPLETED"),
                "totalDelays", tripMetrics.totalDelays(),
                "totalIncidents", tripMetrics.totalIncidents(),
                "avgTripDurationMinutes", Math.round(tripMetrics.averageDurationMinutes()),
                "vehiclesReporting", all.size(),
                "telemetryReadings", all.stream().mapToLong(VehicleStats::getReadings).sum(),
                "alertsRaised", all.stream().mapToLong(VehicleStats::getAlertsRaised).sum());
    }

    @Transactional(readOnly = true)
    public List<VehicleStats> vehicles() {
        return vehicleStats.findAll();
    }

    @Transactional(readOnly = true)
    public List<TripMetric> trips() {
        return tripMetrics.findAll();
    }

    private boolean alreadyProcessed(String eventId) {
        if (processed.existsById(eventId)) {
            return true;
        }
        processed.save(new ProcessedEvent(eventId));
        return false;
    }

    private static AlertMessage tripAlert(TripEvent e, String type, String severity, String description) {
        return new AlertMessage(UUID.randomUUID(), e.tripId(), e.vehicleId(), type, severity, description, Instant.now());
    }
}
