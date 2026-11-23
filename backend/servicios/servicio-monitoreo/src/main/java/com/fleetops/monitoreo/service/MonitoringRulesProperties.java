package com.fleetops.monitoreo.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Umbrales de negocio para la deteccion de alertas sobre la telemetria (mismos defaults que servicio-flota). */
@ConfigurationProperties(prefix = "fleetops.monitoring")
public record MonitoringRulesProperties(double speedLimitKmh, double lowFuelPercent) {
    public MonitoringRulesProperties {
        speedLimitKmh = speedLimitKmh <= 0 ? 90 : speedLimitKmh;
        lowFuelPercent = lowFuelPercent <= 0 ? 15 : lowFuelPercent;
    }
}
