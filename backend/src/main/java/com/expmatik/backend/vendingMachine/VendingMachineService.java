package com.expmatik.backend.vendingMachine;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.user.Role;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingMachine.DTOs.VendingMachineCreate;
import com.expmatik.backend.vendingMachine.DTOs.VendingMachineUpdate;
import com.expmatik.backend.vendingSlot.VendingSlotService;

@Service
public class VendingMachineService {

    private final VendingMachineRepository vendingMachineRepository;
    public final VendingSlotService vendingSlotService;

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
        if (user.getRole() == Role.MAINTAINER) return;
        if (!vendingMachine.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("The user is not the owner of the vending machine.");
        }
    }

    void validateVendingMachineNameUniqueness(String name, User user) {
        if (vendingMachineRepository.findByNameAndUserId(name, user.getId()).isPresent()) {
            throw new ConflictException("A vending machine with the same name already exists.");
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
        vendingMachine.setLocation(vendingMachineUpdate.location());
        return vendingMachineRepository.save(vendingMachine);
    }

    @Transactional(readOnly = true)
    public Page<VendingMachine> listVendingMachines(User user, Pageable pageable) {
        return vendingMachineRepository.findAllByUserId(user.getId(), pageable);
    }

    @Transactional
    public VendingMachine findVendingMachineByNameAndUserId(String name, User user) {
        return vendingMachineRepository.findByNameAndUserId(name, user.getId()).orElseThrow(() -> new ResourceNotFoundException("The vending machine does not exist."));
    }

    @Transactional
    public VendingMachine createVendingMachineForSeeder(String name, String location, Integer rowCount, Integer columnCount, Integer maxCapacityPerSlot, User user) {
        return vendingMachineRepository.findByNameAndUserId(name, user.getId())
                .orElseGet(() -> {
                    VendingMachine vm = new VendingMachine();
                    vm.setName(name);
                    vm.setLocation(location);
                    vm.setRowCount(rowCount);
                    vm.setColumnCount(columnCount);
                    vm.setUser(user);
                    VendingMachine saved = vendingMachineRepository.save(vm);
                    vendingSlotService.createVendingSlotsForMachine(saved, rowCount, columnCount, maxCapacityPerSlot);
                    return saved;
                });
    }

}
