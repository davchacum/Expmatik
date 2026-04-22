package com.expmatik.backend.maintenance;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceRepository extends JpaRepository<Maintenance, UUID> {

    @Query("""
        SELECT m FROM Maintenance m
        WHERE (m.administrator is NULL OR m.administrator.id = :administratorId)
        AND (m.maintainer is NULL OR m.maintainer.id = :maintainerId)
        AND (:excludeDraft = false OR m.status <> :draftStatus)
        AND (:status IS NULL OR m.status = :status)
        AND (m.maintenanceDate >= COALESCE(:startDate, m.maintenanceDate))
        AND (m.maintenanceDate <= COALESCE(:endDate, m.maintenanceDate))
        ORDER BY m.maintenanceDate DESC
    """)
    Page<Maintenance> searchMaintenances(
        @Param("administratorId") UUID administratorId,
        @Param("maintainerId") UUID maintainerId,
        @Param("excludeDraft") boolean excludeDraft,
        @Param("draftStatus") MaintenanceStatus draftStatus,
        @Param("status") MaintenanceStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

}
