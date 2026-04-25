package com.expmatik.backend.maintenance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceRepository extends JpaRepository<Maintenance, UUID> {

    @Query("""
        SELECT m FROM Maintenance m
        WHERE (:administratorId IS NULL OR m.administrator.id = :administratorId)
        AND (:maintainerId IS NULL OR m.maintainer.id = :maintainerId)
        AND (:machineName IS NULL OR LOWER(m.vendingMachine.name) LIKE LOWER(CONCAT('%', CAST(:machineName AS string), '%')))
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
        @Param("machineName") String machineName,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("""
        SELECT m FROM Maintenance m
        WHERE m.status = :pendingStatus
        AND m.maintenanceDate > :currentDate
    """)
    List<Maintenance> findPendingMaintenancesByMaintenanceDateAfter(
        @Param("pendingStatus") MaintenanceStatus pendingStatus,
        @Param("currentDate") LocalDate currentDate
    );

    @Query("""
        SELECT DISTINCT m FROM Maintenance m
        JOIN m.maintenanceDetails md
        WHERE m.status = :delayedStatus
        AND md.expirationDate = :expirationDate
    """)
    List<Maintenance> findDelayedMaintenanceByDetailsExpirationDate(
        @Param("delayedStatus") MaintenanceStatus delayedStatus,
        @Param("expirationDate") LocalDate expirationDate
    );

}
