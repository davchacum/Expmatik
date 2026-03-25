package com.expmatik.backend.vendingSlot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.exceptions.UnauthorizedActionException;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingMachine.VendingMachine;

@Service
public class VendingSlotService {

    private final VendingSlotRepository vendingSlotRepository;

    private final ProductService productService;

    public VendingSlotService(VendingSlotRepository vendingSlotRepository, ProductService productService) {
        this.vendingSlotRepository = vendingSlotRepository;
        this.productService = productService;
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
        List<VendingSlot> vendingSlots = vendingSlotRepository.findAllByVendingMachineId(machineId);
        if(!vendingSlots.isEmpty() && !vendingSlots.get(0).getVendingMachine().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("The user is not the owner of the vending machine.");
        }
        return vendingSlots;
    }

    @Transactional
    public VendingSlot saveVendingSlot(VendingSlot vendingSlot) {
        return vendingSlotRepository.save(vendingSlot);
    }

    @Transactional
    public void createVendingSlotsForMachine(VendingMachine machine, Integer rowCount, Integer columnCount, Integer maxCapacityPerSlot) {
        for (int row = 1; row <= rowCount; row++) {
            for (int column = 1; column <= columnCount; column++) {
                VendingSlot vendingSlot = new VendingSlot();
                vendingSlot.setRowNumber(row);
                vendingSlot.setColumnNumber(column);
                vendingSlot.setMaxCapacity(maxCapacityPerSlot);
                vendingSlot.setCurrentStock(0);
                vendingSlot.setIsBlocked(true);
                vendingSlot.setVendingMachine(machine);
                vendingSlot.setExpirationBatch(new ArrayList<>());
                vendingSlotRepository.save(vendingSlot);
            }
        }
    }

    @Transactional
    public VendingSlot assignProductToVendingSlot(UUID vendingSlotId, String barcode, User user) {
        VendingSlot vendingSlot = getVendingSlotById(vendingSlotId,user);
        if(!vendingSlot.getVendingMachine().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("The user is not the owner of the vending machine.");
        }
        if(vendingSlot.getCurrentStock() > 0) {
            throw new ConflictException("Cannot assign a product to a vending slot that is not empty.");
        }
        //Revisar que no tenga tarea de mantenimiento pendiente actualmente no implementado
        if(vendingSlot.getIsBlocked()) {
            throw new ConflictException("Cannot assign a product to a vending slot that is blocked for maintenance.");
        }
        if(barcode.equals(null)){
            vendingSlot.setProduct(null);
        }else{
            Product product = productService.findInternalProductByBarcode(barcode, user.getId());
            vendingSlot.setProduct(product);
        }
        return vendingSlotRepository.save(vendingSlot);
    }

    @Transactional
    public VendingSlot updateBlockStatus(UUID vendingSlotId, Boolean isBlocked, User user) {
        VendingSlot vendingSlot = getVendingSlotById(vendingSlotId,user);
        if(!vendingSlot.getVendingMachine().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("The user is not the owner of the vending machine.");
        }
        if(vendingSlot.getIsBlocked().equals(Boolean.TRUE) && isBlocked.equals(Boolean.FALSE)) {
            if(!vendingSlot.getExpirationBatch().isEmpty()) {
                Boolean hasNonExpiredBatch = vendingSlot.getExpirationBatch().stream().anyMatch(batch -> batch.getExpirationDate().isAfter(LocalDate.now()));
                if(!hasNonExpiredBatch) {
                    throw new ConflictException("Cannot unblock a vending slot with expired products.");
                }

            }
        }
        //Revisar que no tenga tarea de mantenimiento pendiente actualmente no implementado
        vendingSlot.setIsBlocked(isBlocked);
        return vendingSlotRepository.save(vendingSlot);

    }





}
