package com.fleetops.monitoreo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Agregado en caliente por vehículo, alimentado por el tópico de telemetría. */
@Entity
@Table(name = "vehicle_stats")
public class VehicleStats {

    @Id
    @Column(length = 80)
    private String vehicleId;

    private long readings;
    private double speedSum;
    private double maxSpeedKmh;
    private Double lastFuelLevel;
    private Instant lastReadingAt;
    private long alertsRaised;

    protected VehicleStats() {
    }

    public VehicleStats(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public void register(double speed, Double fuel, Instant at) {
        readings++;
        speedSum += speed;
        maxSpeedKmh = Math.max(maxSpeedKmh, speed);
        lastFuelLevel = fuel;
        lastReadingAt = at;
    }

    public void addAlerts(int n) {
        alertsRaised += n;
    }

    public double getAvgSpeedKmh() {
        return readings == 0 ? 0 : Math.round(speedSum / readings * 10) / 10.0;
    }

    public String getVehicleId() { return vehicleId; }
    public long getReadings() { return readings; }
    public double getMaxSpeedKmh() { return maxSpeedKmh; }
    public Double getLastFuelLevel() { return lastFuelLevel; }
    public Instant getLastReadingAt() { return lastReadingAt; }
    public long getAlertsRaised() { return alertsRaised; }
}
