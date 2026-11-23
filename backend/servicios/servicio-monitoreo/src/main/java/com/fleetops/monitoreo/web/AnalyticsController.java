package com.fleetops.monitoreo.web;

import com.fleetops.monitoreo.domain.TripMetric;
import com.fleetops.monitoreo.domain.VehicleStats;
import com.fleetops.monitoreo.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    public record VehicleStatsResponse(String vehicleId, long readings, double avgSpeedKmh, double maxSpeedKmh,
                                       Double lastFuelLevel, Instant lastReadingAt,
                                       long alertsRaised) {
        static VehicleStatsResponse from(VehicleStats v) {
            return new VehicleStatsResponse(v.getVehicleId(), v.getReadings(), v.getAvgSpeedKmh(), v.getMaxSpeedKmh(),
                    v.getLastFuelLevel(), v.getLastReadingAt(), v.getAlertsRaised());
        }
    }

    public record TripMetricResponse(String tripId, String vehicleId, String vehiclePlate, String status, int events,
                                     int delays, int incidents, Long durationMinutes, Instant lastEventAt) {
        static TripMetricResponse from(TripMetric t) {
            return new TripMetricResponse(t.getTripId(), t.getVehicleId(), t.getVehiclePlate(), t.getStatus(),
                    t.getEvents(), t.getDelays(), t.getIncidents(), t.getDurationMinutes(), t.getLastEventAt());
        }
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return service.summary();
    }

    @GetMapping("/vehicles")
    public List<VehicleStatsResponse> vehicles() {
        return service.vehicles().stream().map(VehicleStatsResponse::from).toList();
    }

    @GetMapping("/trips")
    public List<TripMetricResponse> trips() {
        return service.trips().stream().map(TripMetricResponse::from).toList();
    }
}
