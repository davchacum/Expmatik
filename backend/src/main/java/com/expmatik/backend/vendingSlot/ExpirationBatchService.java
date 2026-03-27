package com.expmatik.backend.vendingSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.UnauthorizedActionException;
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
        if(expirationBatches.get(0).getVendingSlot().getVendingMachine().getUser().getId() != user.getId()) {
            throw new UnauthorizedActionException("You are not authorized to view the expiration batches of this vending slot.");
        }
        return expirationBatches;
    }   

    public void pushExpirationBatch(VendingSlot vendingSlot, LocalDate expirationDate, Integer quantity, User user) {
    if (vendingSlot.getCurrentStock() + quantity > vendingSlot.getMaxCapacity()) {
                throw new ConflictException("Cannot add stock to the vending slot because it exceeds the maximum capacity.");
            }
        List<ExpirationBatch> expirationBatches = getExpirationBatchesByVendingSlotId(vendingSlot.getId(), user);
        Optional<ExpirationBatch> existingBatch = expirationBatches.stream()
                .filter(batch -> batch.getExpirationDate().equals(expirationDate))
                .findFirst();
        if (existingBatch.isPresent()) {
            existingBatch.get().setQuantity(existingBatch.get().getQuantity() + quantity);
            vendingSlot.setCurrentStock(vendingSlot.getCurrentStock() + quantity);
            return;
        }
        ExpirationBatch batch = new ExpirationBatch();
        batch.setExpirationDate(expirationDate);
        batch.setQuantity(quantity);
        batch.setVendingSlot(vendingSlot);
        expirationBatchRepository.save(batch);
        vendingSlot.setCurrentStock(vendingSlot.getCurrentStock() + quantity);
    }

    public void popUnitExpirationBatch(VendingSlot vendingSlot, User user) {
        if(vendingSlot.getCurrentStock() <= 0) {
            throw new ConflictException("Cannot remove stock from the vending slot because it is empty.");
        }
        List<ExpirationBatch> expirationBatches = getExpirationBatchesByVendingSlotId(vendingSlot.getId(), user);
        ExpirationBatch existingBatch = expirationBatches.get(0);
        if (existingBatch.getQuantity() <= 0) {
            throw new ConflictException("Cannot remove stock from the vending slot because there is no stock with the specified expiration date.");
        }
        existingBatch.setQuantity(existingBatch.getQuantity() - 1);
        if(existingBatch.getQuantity() <= 0) {
            expirationBatchRepository.delete(existingBatch);
        }
        vendingSlot.setCurrentStock(vendingSlot.getCurrentStock() - 1);
    }

    //Aqui añadiré la futura función de limpiar el stock caducado

}
