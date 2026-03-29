package com.expmatik.backend.vendingSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ExpiredProductException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.exceptions.SlotBlockedException;
import com.expmatik.backend.exceptions.UnauthorizedActionException;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingMachine.VendingMachine;

@Service
public class VendingSlotService {

    private final VendingSlotRepository vendingSlotRepository;

    private final ProductService productService;

    private final ExpirationBatchService expirationBatchService;

    public VendingSlotService(VendingSlotRepository vendingSlotRepository, ProductService productService, ExpirationBatchService expirationBatchService) {
        this.vendingSlotRepository = vendingSlotRepository;
        this.productService = productService;
        this.expirationBatchService = expirationBatchService;
    }

    @Transactional(readOnly = true)
    public VendingSlot getVendingSlotById(UUID id,User user) {
            VendingSlot vendingSlot = vendingSlotRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("The vending slot does not exist."));
            checkUserAuthorization(vendingSlot, user);
            return vendingSlot; 
    }

    @Transactional(readOnly = true)
    public List<VendingSlot> getVendingSlotsByUserIdAndMachineId(UUID machineId,User user) {
        List<VendingSlot> vendingSlots = vendingSlotRepository.findAllByVendingMachineId(machineId);
        checkUserAuthorization(vendingSlots.get(0), user);
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
                vendingSlotRepository.save(vendingSlot);
            }
        }
    }

    @Transactional
    public VendingSlot assignProductToVendingSlot(UUID vendingSlotId, String barcode, User user) {
        VendingSlot vendingSlot = getVendingSlotById(vendingSlotId,user);
        checkUserAuthorization(vendingSlot, user);
        checkVendingSlotNotEmpty(vendingSlot);
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
        checkUserAuthorization(vendingSlot, user);
        if(vendingSlot.getIsBlocked().equals(Boolean.TRUE) && isBlocked.equals(Boolean.FALSE)) {
            List<ExpirationBatch> expirationBatches = expirationBatchService.getExpirationBatchesByVendingSlotId(vendingSlot.getId(), user);
            if(!expirationBatches.isEmpty()) {
                Boolean hasNonExpiredBatch = expirationBatches.stream().anyMatch(batch -> batch.getExpirationDate().isAfter(LocalDate.now()));
                if(!hasNonExpiredBatch) {
                    throw new ConflictException("Cannot unblock a vending slot with expired products.");
                }

            }
        }
        //Revisar que no tenga tarea de mantenimiento pendiente actualmente no implementado
        vendingSlot.setIsBlocked(isBlocked);
        return vendingSlotRepository.save(vendingSlot);
    }

    @Transactional
    public VendingSlot addStockToVendingSlot(UUID vendingSlotId, Integer quantity, LocalDate expirationDate, User user) {
        VendingSlot vendingSlot = getVendingSlotById(vendingSlotId,user);
        checkUserAuthorization(vendingSlot, user);
        if(vendingSlot.getProduct() == null) {
            throw new ConflictException("Cannot add stock to a vending slot that does not have an assigned product.");
        }
        checkVendingSlotNotBlocked(vendingSlot);
        if(quantity <= 0) {
            throw new ConflictException("Quantity must be greater than zero.");
        }
        if(vendingSlot.getCurrentStock() + quantity > vendingSlot.getMaxCapacity()) {
            throw new ConflictException("Cannot add stock to a vending slot that exceeds its maximum capacity.");
        }
        if(expirationDate.isBefore(LocalDate.now())) {
            throw new ExpiredProductException("Cannot add stock with an expiration date in the past.");
        }
        expirationBatchService.pushExpirationBatch(vendingSlot, expirationDate, quantity, user);
        
        return vendingSlotRepository.save(vendingSlot);
    }

    //Funcion simple para probar el funcionamiento de la pila

    @Transactional
    public VendingSlot popStockFromVendingSlot(UUID vendingSlotId, User user) {
        VendingSlot vendingSlot = getVendingSlotById(vendingSlotId,user);
        checkUserAuthorization(vendingSlot, user);
        expirationBatchService.popUnitExpirationBatch(vendingSlot, user);
        
        return vendingSlotRepository.save(vendingSlot);
    }
 
    public void checkUserAuthorization(VendingSlot vendingSlot, User user) {
        if (!vendingSlot.getVendingMachine().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("The user is not the owner of the vending machine.");
        }
    }

    public void checkVendingSlotNotEmpty(VendingSlot vendingSlot) {
        if (vendingSlot.getCurrentStock() > 0) {
            throw new ConflictException("The vending slot is not empty.");
        }
    }

    public void checkVendingSlotNotBlocked(VendingSlot vendingSlot) {
        if (vendingSlot.getIsBlocked()) {
            throw new SlotBlockedException("The vending slot is blocked for maintenance.");
        }
    }

}
