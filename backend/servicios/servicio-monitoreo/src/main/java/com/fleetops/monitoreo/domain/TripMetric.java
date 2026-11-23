package com.fleetops.monitoreo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

/** Métricas por viaje construidas a partir del tópico fleetops.viajes.eventos. */
@Entity
@Table(name = "trip_metrics")
public class TripMetric {

    @Id
    @Column(length = 80)
    private String tripId;

    @Column(length = 80)
    private String vehicleId;

    @Column(length = 20)
    private String vehiclePlate;

    @Column(length = 20)
    private String status;

    private int events;
    private int delays;
    private int incidents;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationMinutes;
    private Instant lastEventAt;

    protected TripMetric() {
    }

    public TripMetric(String tripId, String vehicleId, String vehiclePlate) {
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.vehiclePlate = vehiclePlate;
    }

    public void apply(String type, String newStatus, Instant at) {
        events++;
        status = newStatus;
        lastEventAt = at;
        switch (type) {
            case "STARTED" -> startedAt = at;
            case "DELAYED" -> delays++;
            case "INCIDENT" -> incidents++;
            case "COMPLETED", "CANCELLED" -> {
                completedAt = at;
                if (startedAt != null) {
                    durationMinutes = Duration.between(startedAt, at).toMinutes();
                }
            }
            default -> { }
        }
    }

    public String getTripId() { return tripId; }
    public String getVehicleId() { return vehicleId; }
    public String getVehiclePlate() { return vehiclePlate; }
    public String getStatus() { return status; }
    public int getEvents() { return events; }
    public int getDelays() { return delays; }
    public int getIncidents() { return incidents; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getDurationMinutes() { return durationMinutes; }
    public Instant getLastEventAt() { return lastEventAt; }
}
