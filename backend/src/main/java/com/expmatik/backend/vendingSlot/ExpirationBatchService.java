package com.expmatik.backend.vendingSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ExpiredProductException;
import com.expmatik.backend.exceptions.OutOfStockException;
import com.expmatik.backend.exceptions.SlotBlockedException;
import com.expmatik.backend.user.User;


@Service
public class ExpirationBatchService {

    private final ExpirationBatchRepository expirationBatchRepository;

    public ExpirationBatchService(ExpirationBatchRepository expirationBatchRepository) {
        this.expirationBatchRepository = expirationBatchRepository;
    }

    @Transactional(readOnly = true)
    public List<ExpirationBatch> getExpirationBatchesByVendingSlotId(UUID vendingSlotId,User user) {
        List<ExpirationBatch> expirationBatches = expirationBatchRepository.findAllByVendingSlotIdOrderByExpirationDateAsc(vendingSlotId);
        if(!expirationBatches.isEmpty() && expirationBatches.get(0).getVendingSlot().getVendingMachine().getUser().getId() != user.getId()) {
            throw new AccessDeniedException("You are not authorized to view the expiration batches of this vending slot.");
        }
        return expirationBatches;
    }   

    @Transactional
    public void pushExpirationBatch(VendingSlot vendingSlot, LocalDate expirationDate, Integer quantity, User user) {
        Optional<ExpirationBatch> existingBatch = expirationBatchRepository.findFirstByVendingSlotIdAndExpirationDate(vendingSlot.getId(), expirationDate);
        if (existingBatch.isPresent()) {
            existingBatch.get().setQuantity(existingBatch.get().getQuantity() + quantity);
            vendingSlot.setCurrentStock(vendingSlot.getCurrentStock() + quantity);
            expirationBatchRepository.save(existingBatch.get());
            return;
        }else{
            ExpirationBatch batch = new ExpirationBatch();
            batch.setExpirationDate(expirationDate);
            batch.setQuantity(quantity);
            batch.setVendingSlot(vendingSlot);
            expirationBatchRepository.save(batch);
        }
        vendingSlot.setCurrentStock(vendingSlot.getCurrentStock() + quantity);
    }

    @Transactional
    public void popUnitExpirationBatch(VendingSlot vendingSlot, User user) {
        popUnitExpirationBatchInternal(vendingSlot, user);
    }

    public void popUnitExpirationBatchForSale(VendingSlot vendingSlot, User user) {
        popUnitExpirationBatchInternal(vendingSlot, user);
    }

    private void popUnitExpirationBatchInternal(VendingSlot vendingSlot, User user) {
        
        List<ExpirationBatch> expirationBatches = getExpirationBatchesByVendingSlotId(vendingSlot.getId(), user);
        ExpirationBatch existingBatch = expirationBatches.get(0);

        if (existingBatch.getExpirationDate().isBefore(LocalDate.now())) {
            throw new ExpiredProductException("Cannot register sale because the product is expired.");
        }

        if (existingBatch.getQuantity() <= 0) {
            throw new OutOfStockException("Cannot remove stock from the vending slot because there is no stock with the specified expiration date.");
        }

        if (vendingSlot.getIsBlocked()) {
            throw new SlotBlockedException("The vending slot is blocked for maintenance.");
        }

        existingBatch.setQuantity(existingBatch.getQuantity() - 1);
        if(existingBatch.getQuantity() <= 0) {
            expirationBatchRepository.delete(existingBatch);
        }else {
            expirationBatchRepository.save(existingBatch);
        }
        vendingSlot.setCurrentStock(vendingSlot.getCurrentStock() - 1);
    }

    //Aqui añadiré la futura función de limpiar el stock caducado

}
