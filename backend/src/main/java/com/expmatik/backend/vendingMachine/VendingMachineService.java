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

@Service
public class VendingMachineService {

    private final VendingMachineRepository vendingMachineRepository;

    @Autowired
    public VendingMachineService(VendingMachineRepository vendingMachineRepository) {
        this.vendingMachineRepository = vendingMachineRepository;
    }

    @Transactional
    public VendingMachine createVendingMachine(VendingMachineCreate vendingMachineCreate, User user) {
        validateVendingMachineNameUniqueness(vendingMachineCreate.name(), user);
        VendingMachine vendingMachine = VendingMachineCreate.toEntity(vendingMachineCreate);
        vendingMachine.setUser(user);
        return vendingMachineRepository.save(vendingMachine);
    }

    void validateVendingMachineOwnership(VendingMachine vendingMachine, User user) {
        if (!vendingMachine.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("El usuario no es el propietario de la máquina expendedora.");
        }
    }

    void validateVendingMachineNameUniqueness(String name, User user) {
        if (vendingMachineRepository.findByNameAndUserId(name, user.getId()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una máquina expendedora con el mismo nombre.");
        }
    }

    @Transactional(readOnly = true)
    public VendingMachine findVendingMachineById(UUID vendingMachineId) {
        return vendingMachineRepository.findById(vendingMachineId).orElseThrow(() -> new ResourceNotFoundException("La máquina expendedora no existe."));
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
