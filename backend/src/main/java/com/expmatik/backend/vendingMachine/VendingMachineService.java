package com.expmatik.backend.vendingMachine;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingMachine.DTOs.VendingMachineCreate;
import com.expmatik.backend.vendingMachine.DTOs.VendingMachineUpdate;
import com.expmatik.backend.vendingSlot.VendingSlotService;

@Service
public class VendingMachineService {

    private final VendingMachineRepository vendingMachineRepository;
    public final VendingSlotService vendingSlotService;

    @Autowired
    public VendingMachineService(VendingMachineRepository vendingMachineRepository, VendingSlotService vendingSlotService) {
        this.vendingMachineRepository = vendingMachineRepository;
        this.vendingSlotService = vendingSlotService;
    }

    @Transactional
    public VendingMachine createVendingMachine(VendingMachineCreate vendingMachineCreate, User user) {
        validateVendingMachineNameUniqueness(vendingMachineCreate.name(), user);
        VendingMachine vendingMachine = VendingMachineCreate.toEntity(vendingMachineCreate);
        vendingMachine.setUser(user);
        vendingMachine = vendingMachineRepository.save(vendingMachine);
        vendingSlotService.createVendingSlotsForMachine(vendingMachine,vendingMachineCreate.rowCount(), vendingMachineCreate.columnCount(),vendingMachineCreate.maxCapacityPerSlot());
        return vendingMachine;
    }

    void validateVendingMachineOwnership(VendingMachine vendingMachine, User user) {
        if (!vendingMachine.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("The user is not the owner of the vending machine.");
        }
    }

    void validateVendingMachineNameUniqueness(String name, User user) {
        if (vendingMachineRepository.findByNameAndUserId(name, user.getId()).isPresent()) {
            throw new IllegalArgumentException("A vending machine with the same name already exists.");
        }
    }

    @Transactional(readOnly = true)
    public VendingMachine findVendingMachineById(UUID vendingMachineId) {
        return vendingMachineRepository.findById(vendingMachineId).orElseThrow(() -> new ResourceNotFoundException("The vending machine does not exist."));
    }

    @Transactional(readOnly = true)
    public VendingMachine getVendingMachineById(UUID vendingMachineId, User user) {
        VendingMachine vendingMachine = findVendingMachineById(vendingMachineId);
        validateVendingMachineOwnership(vendingMachine, user);
        return vendingMachine;

    }

    @Transactional
    public VendingMachine updateVendingMachine(UUID vendingMachineId, VendingMachineUpdate vendingMachineUpdate, User user) {
        VendingMachine vendingMachine = findVendingMachineById(vendingMachineId);
        validateVendingMachineOwnership(vendingMachine, user);
        validateVendingMachineNameUniqueness(vendingMachineUpdate.name(), user);
        vendingMachine.setLocation(vendingMachineUpdate.location());
        vendingMachine.setName(vendingMachineUpdate.name());
        return vendingMachineRepository.save(vendingMachine);
    }

    @Transactional(readOnly = true)
    public Page<VendingMachine> listVendingMachines(User user, Pageable pageable) {
        return vendingMachineRepository.findAllByUserId(user.getId(), pageable);
    }

}
