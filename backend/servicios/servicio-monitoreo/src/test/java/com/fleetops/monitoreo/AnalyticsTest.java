package com.fleetops.monitoreo;

import com.fleetops.common.events.AlertMessage;
import com.fleetops.common.events.TelemetryEvent;
import com.fleetops.common.events.TripEvent;
import com.fleetops.common.events.TripEventType;
import com.fleetops.monitoreo.repository.ProcessedEventRepository;
import com.fleetops.monitoreo.repository.TripMetricRepository;
import com.fleetops.monitoreo.repository.VehicleStatsRepository;
import com.fleetops.monitoreo.service.AnalyticsService;
import com.fleetops.monitoreo.service.MonitoringRulesProperties;
import com.fleetops.monitoreo.service.TelemetryAnalyzer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsTest {

    private final TelemetryAnalyzer analyzer = new TelemetryAnalyzer(new MonitoringRulesProperties(90, 15));

    private static TelemetryEvent reading(double speed, Double fuel) {
        return new TelemetryEvent(UUID.randomUUID().toString(), "veh-1", "trip-7", -33.45, -70.66, speed, fuel, 1200.0,
                Instant.now(), "test");
    }

    @Test
    void lecturaNormalNoGeneraAlertas() {
        assertThat(analyzer.analyze(reading(80, 60.0))).isEmpty();
    }

    @Test
    void detectaExcesoVelocidadYCombustibleBajo() {
        List<AlertMessage> alerts = analyzer.analyze(reading(130, 5.0));
        assertThat(alerts).extracting(AlertMessage::type)
                .containsExactlyInAnyOrder("SPEEDING", "LOW_FUEL");
        assertThat(alerts).filteredOn(a -> a.type().equals("SPEEDING")).first()
                .extracting(AlertMessage::severity).isEqualTo("HIGH");
    }

    @Test
    void eventoDuplicadoNoSeProcesaDosVeces() {
        VehicleStatsRepository stats = mock(VehicleStatsRepository.class);
        TripMetricRepository trips = mock(TripMetricRepository.class);
        ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
        AnalyticsService service = new AnalyticsService(stats, trips, processed, analyzer);

        TripEvent incident = TripEvent.of(TripEventType.INCIDENT, "trip-7", "veh-1", "KTRZ21", "Ana", "Stgo", "Valpo", "INCIDENT");
        when(trips.findById("trip-7")).thenReturn(Optional.empty());
        when(processed.existsById(incident.eventId())).thenReturn(false, true);
        when(trips.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.processTripEvent(incident)).extracting(AlertMessage::type).containsExactly("TRIP_INCIDENT");
        assertThat(service.processTripEvent(incident)).isEmpty();
    }
}
