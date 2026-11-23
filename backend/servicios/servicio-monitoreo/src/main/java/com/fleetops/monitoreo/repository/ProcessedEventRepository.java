package com.fleetops.monitoreo.repository;

import com.fleetops.monitoreo.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
