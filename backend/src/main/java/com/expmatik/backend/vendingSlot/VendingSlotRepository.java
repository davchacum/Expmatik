package com.expmatik.backend.vendingSlot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.expmatik.backend.maintenance.MaintenanceStatus;

public interface VendingSlotRepository extends JpaRepository<VendingSlot, UUID> {

    Optional<VendingSlot> findById(UUID id);

    @Query("SELECT v FROM VendingSlot v WHERE v.vendingMachine.id = :machineId")
    List<VendingSlot> findAllByVendingMachineId(UUID machineId);

    @Query("SELECT v FROM VendingSlot v WHERE v.vendingMachine.name = :machineName AND v.rowNumber = :row AND v.columnNumber = :column")
    Optional<VendingSlot> findByVendingMachineNameAndRowAndColumn(String machineName, Integer row, Integer column);

    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM Maintenance m
        JOIN m.maintenanceDetails md
        WHERE (m.status = :pendingStatus OR m.status = :delayedStatus OR m.status = :draftStatus)
        AND EXISTS (SELECT 1 FROM VendingSlot vs WHERE vs.id = :vendingSlotId AND vs.vendingMachine.id = m.vendingMachine.id AND vs.rowNumber = md.rowNumber AND vs.columnNumber = md.columnNumber)
    """)
    boolean existsPendingOrDelayedBySlot(
        @Param("pendingStatus") MaintenanceStatus pendingStatus,
        @Param("delayedStatus") MaintenanceStatus delayedStatus,
        @Param("draftStatus") MaintenanceStatus draftStatus,
        @Param("vendingSlotId") UUID vendingSlotId
    );

}
