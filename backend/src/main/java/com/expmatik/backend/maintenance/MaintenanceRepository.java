package com.expmatik.backend.maintenance;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRepository extends JpaRepository<Maintenance, UUID> {

}
