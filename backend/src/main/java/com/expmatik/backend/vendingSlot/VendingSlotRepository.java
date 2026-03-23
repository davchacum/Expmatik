package com.expmatik.backend.vendingSlot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VendingSlotRepository extends JpaRepository<VendingSlot, UUID> {

    Optional<VendingSlot> findById(UUID id);

    @Query("SELECT v FROM VendingSlot v WHERE v.vendingMachine.id = :machineId")
    List<VendingSlot> findAllByUserIdAndMachineId(UUID machineId);

}
