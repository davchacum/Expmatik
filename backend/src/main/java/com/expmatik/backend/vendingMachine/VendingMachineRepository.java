package com.expmatik.backend.vendingMachine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VendingMachineRepository extends JpaRepository<VendingMachine, UUID> {

    @Query("SELECT v FROM VendingMachine v WHERE v.name = :name AND v.user.id = :userId")
    Optional<VendingMachine> findByNameAndUserId(String name, UUID userId);

    Page<VendingMachine> findAllByUserId(UUID userId, Pageable pageable);

    List<VendingMachine> findAllByUserId(UUID userId);

}
