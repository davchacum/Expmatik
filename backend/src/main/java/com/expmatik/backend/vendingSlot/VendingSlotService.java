package com.expmatik.backend.vendingSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.exceptions.UnauthorizedActionException;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingMachine.VendingMachine;

@Service
public class VendingSlotService {

    private final VendingSlotRepository vendingSlotRepository;

    public VendingSlotService(VendingSlotRepository vendingSlotRepository) {
        this.vendingSlotRepository = vendingSlotRepository;
    }

    @Transactional(readOnly = true)
    public VendingSlot getVendingSlotById(UUID id,User user) {
            VendingSlot vendingSlot = vendingSlotRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("The vending slot does not exist."));
            if(!vendingSlot.getVendingMachine().getUser().getId().equals(user.getId())) {
                throw new UnauthorizedActionException("The user is not the owner of the vending machine.");
            }
            return vendingSlot; 
    }

    @Transactional(readOnly = true)
    public List<VendingSlot> getVendingSlotsByUserIdAndMachineId(UUID machineId,User user) {
        List<VendingSlot> vendingSlots = vendingSlotRepository.findAllByUserIdAndMachineId(machineId);
        if(!vendingSlots.isEmpty() && !vendingSlots.get(0).getVendingMachine().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("The user is not the owner of the vending machine.");
        }
        return vendingSlotRepository.findAllByUserIdAndMachineId(machineId);
    }

    @Transactional
    public VendingSlot saveVendingSlot(VendingSlot vendingSlot) {
        return vendingSlotRepository.save(vendingSlot);
    }

    @Transactional
    public List<VendingSlot> createVendingSlotsForMachine(Integer rowCount, Integer columnCount, UUID machineId) {
        List<VendingSlot> createdSlots = new ArrayList<>();
        for (int row = 1; row <= rowCount; row++) {
            for (int column = 1; column <= columnCount; column++) {
                VendingSlot vendingSlot = new VendingSlot();
                vendingSlot.setRowNumber(row);
                vendingSlot.setColumnNumber(column);
                vendingSlot.setMaxCapacity(0);
                vendingSlot.setCurrentStock(0);
                vendingSlot.setIsBlocked(false);
                vendingSlot.setVendingMachine(new VendingMachine());
                vendingSlot.getVendingMachine().setId(machineId);
                vendingSlotRepository.save(vendingSlot);
                createdSlots.add(vendingSlot);
            }
        }
        return createdSlots;
    }





}
