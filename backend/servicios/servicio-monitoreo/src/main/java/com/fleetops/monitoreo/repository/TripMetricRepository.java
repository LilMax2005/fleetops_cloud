package com.fleetops.monitoreo.repository;

import com.fleetops.monitoreo.domain.TripMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TripMetricRepository extends JpaRepository<TripMetric, String> {
    long countByStatus(String status);

    @Query("select coalesce(avg(t.durationMinutes), 0) from TripMetric t where t.durationMinutes is not null")
    double averageDurationMinutes();

    @Query("select coalesce(sum(t.incidents), 0) from TripMetric t")
    long totalIncidents();

    @Query("select coalesce(sum(t.delays), 0) from TripMetric t")
    long totalDelays();
}
