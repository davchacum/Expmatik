package com.expmatik.backend.maintenanceDetail;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.expmatik.backend.vendingSlot.VendingSlot;

public interface MaintenanceDetailRepository extends JpaRepository<MaintenanceDetail, UUID> {

    @Query("""
        SELECT DISTINCT vs
        FROM VendingSlot vs
        WHERE EXISTS (
            SELECT 1
            FROM Maintenance m
            JOIN m.maintenanceDetails md
            WHERE m.id = :maintenanceId
              AND md.rowNumber = vs.rowNumber
              AND md.columnNumber = vs.columnNumber
              AND m.vendingMachine.name = vs.vendingMachine.name
        )
    """)
    List<VendingSlot> findDistinctVendingSlotsByMaintenance(@Param("maintenanceId") UUID maintenanceId);

    @Query("""
        SELECT md
        FROM MaintenanceDetail md
        WHERE md.rowNumber = :row
          AND md.columnNumber = :column
          AND EXISTS (
              SELECT 1
              FROM Maintenance m
              JOIN m.maintenanceDetails md2
              WHERE m.id = :maintenanceId
                AND md2.id = md.id
                AND m.vendingMachine.name = :machineName
          )
    """)
    List<MaintenanceDetail> findMaintenanceDetailsByMaintenanceIdAndSlotCoordinates(@Param("maintenanceId") UUID maintenanceId, @Param("machineName") String machineName, @Param("row") Integer row, @Param("column") Integer column);

}
