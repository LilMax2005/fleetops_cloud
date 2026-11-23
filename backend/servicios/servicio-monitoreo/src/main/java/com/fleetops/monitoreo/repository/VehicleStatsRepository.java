package com.fleetops.monitoreo.repository;

import com.fleetops.monitoreo.domain.VehicleStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleStatsRepository extends JpaRepository<VehicleStats, String> {
}
