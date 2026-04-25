package com.expmatik.backend.vendingSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ExpiredProductException;
import com.expmatik.backend.exceptions.OutOfStockException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.maintenance.MaintenanceStatus;
import com.expmatik.backend.notification.NotificationService;
import com.expmatik.backend.notification.NotificationType;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingMachine.VendingMachine;

@Service
public class VendingSlotService {

    private final VendingSlotRepository vendingSlotRepository;

    private final ProductService productService;

    private final ExpirationBatchService expirationBatchService;

    private final NotificationService notificationService;
    

    public VendingSlotService(VendingSlotRepository vendingSlotRepository, ProductService productService, ExpirationBatchService expirationBatchService, NotificationService notificationService) {
        this.vendingSlotRepository = vendingSlotRepository;
        this.productService = productService;
        this.expirationBatchService = expirationBatchService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public VendingSlot getVendingSlotById(UUID id,User user) {
            VendingSlot vendingSlot = getVendingSlotById(id);
            checkUserAuthorization(vendingSlot, user);
            return vendingSlot; 
    }

    @Transactional(readOnly = true)
    public VendingSlot getVendingSlotById(UUID id) {
            VendingSlot vendingSlot = vendingSlotRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("The vending slot does not exist."));
            return vendingSlot; 
    }

    @Transactional(readOnly = true)
    public List<VendingSlot> getVendingSlotsByUserIdAndMachineId(UUID machineId,User user) {
        List<VendingSlot> vendingSlots = vendingSlotRepository.findAllByVendingMachineId(machineId);
        if (vendingSlots.isEmpty()) {
            throw new ResourceNotFoundException("The vending machine does not exist.");
        }
        checkUserAuthorization(vendingSlots.get(0), user);
        return vendingSlots;
    }

    @Transactional
    public VendingSlot saveVendingSlot(VendingSlot vendingSlot) {
        return vendingSlotRepository.save(vendingSlot);
    }

    @Transactional(readOnly = true)
    public VendingSlot getVendingSlotByMachineNameAndRowAndColumn(String machineName, Integer row, Integer column, User user) {
        VendingSlot vendingSlot = vendingSlotRepository.findByVendingMachineNameAndRowAndColumn(machineName, row, column)
                .orElseThrow(() -> new ResourceNotFoundException("The vending slot does not exist."));
        checkUserAuthorization(vendingSlot, user);
        return vendingSlot;
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
                saveVendingSlot(vendingSlot);
            }
        }
    }

    @Transactional
    public VendingSlot assignProductToVendingSlot(UUID vendingSlotId, String barcode, User user) {
        VendingSlot vendingSlot = getVendingSlotById(vendingSlotId,user);
        checkUserAuthorization(vendingSlot, user);
        checkVendingSlotIsEmpty(vendingSlot);
        checkVendingHasPendingMaintenance(vendingSlot);
        if(vendingSlot.getIsBlocked()) {
            throw new ConflictException("Cannot assign or unassign a product to a vending slot that is blocked for maintenance.");
        }
        if(barcode==null){
            vendingSlot.setProduct(null);
        }else{
            Product product = productService.findInternalProductByBarcode(barcode, user.getId());
            vendingSlot.setProduct(product);
        }
        return saveVendingSlot(vendingSlot);
    }

    private void checkVendingHasPendingMaintenance(VendingSlot vendingSlot) {
        Boolean hasPendingMaintenance = vendingSlotRepository.existsPendingOrDelayedBySlot(
            MaintenanceStatus.PENDING,
            MaintenanceStatus.DELAYED,
            MaintenanceStatus.DRAFT,
            vendingSlot.getId()
        );
        if (hasPendingMaintenance) {
            throw new ConflictException("Cannot assign or unassign a product to a vending slot that has pending maintenance.");
        }
    }

    @Transactional
    public VendingSlot updateBlockStatus(UUID vendingSlotId, Boolean isBlocked, User user) {
        VendingSlot vendingSlot = getVendingSlotById(vendingSlotId,user);
        checkUserAuthorization(vendingSlot, user);
        if(vendingSlot.getIsBlocked().equals(Boolean.TRUE) && isBlocked.equals(Boolean.FALSE)) {
            List<ExpirationBatch> expirationBatches = expirationBatchService.getExpirationBatchesByVendingSlotId(vendingSlot.getId(), user);
            if(!expirationBatches.isEmpty()) {
                Boolean hasNonExpiredBatch = expirationBatches.stream().anyMatch(batch -> batch.getExpirationDate() != null && batch.getExpirationDate().isAfter(LocalDate.now()));
                if(!hasNonExpiredBatch) {
                    throw new ConflictException("Cannot unblock a vending slot with expired products.");
                }

            }
        }
        checkVendingHasPendingMaintenance(vendingSlot);
        vendingSlot.setIsBlocked(isBlocked);
        return saveVendingSlot(vendingSlot);
    }

    @Transactional
    public VendingSlot addStockToVendingSlot(UUID vendingSlotId, Integer quantity, LocalDate expirationDate, User user) {
        VendingSlot vendingSlot = getVendingSlotById(vendingSlotId);
        if(vendingSlot.getProduct() == null) {
            throw new ConflictException("Cannot add stock to a vending slot that does not have an assigned product.");
        }
        if(vendingSlot.getCurrentStock() + quantity > vendingSlot.getMaxCapacity()) {
            throw new ConflictException("Cannot add stock to a vending slot that exceeds its maximum capacity.");
        }
        if(vendingSlot.getProduct().getIsPerishable()){
            if (expirationDate == null) {
                throw new ConflictException("Expiration date is required for perishable products.");
            }
            if( expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
                throw new ExpiredProductException("Cannot add stock with an expiration date in the past.");
            }
        }else{
            if (expirationDate != null) {
                throw new ConflictException("Expiration date should not be provided for non-perishable products.");
            }
        }
        expirationBatchService.pushExpirationBatch(vendingSlot, expirationDate, quantity, user);
        
        return saveVendingSlot(vendingSlot);
    }

    @Transactional
    public VendingSlot popStockFromVendingSlot(UUID vendingSlotId, User user) {
        return popStockFromVendingSlotInternal(vendingSlotId, user, false);
    }

    public VendingSlot popStockFromVendingSlotForSale(UUID vendingSlotId, User user) {
        return popStockFromVendingSlotInternal(vendingSlotId, user, true);
    }

    private VendingSlot popStockFromVendingSlotInternal(UUID vendingSlotId, User user, boolean saleFlow) {
        VendingSlot vendingSlot = getVendingSlotById(vendingSlotId,user);
        checkUserAuthorization(vendingSlot, user);
        if (vendingSlot.getProduct() == null) {
            throw new ConflictException("Cannot register sale because the vending slot does not have a product assigned.");
        }

        if(vendingSlot.getCurrentStock() <= 0) {
            throw new OutOfStockException("Cannot remove stock from the vending slot because it is empty.");
        }

        if (saleFlow) {
            expirationBatchService.popUnitExpirationBatchForSale(vendingSlot, user);
        } else {
            expirationBatchService.popUnitExpirationBatch(vendingSlot, user);
        }
        if(vendingSlot.getCurrentStock() == 3) {
            createProductLowStockNotification(vendingSlot, user);
        }else if(vendingSlot.getCurrentStock() == 0) {
            createProductOutOfStockNotification(vendingSlot, user);
        }
        return saveVendingSlot(vendingSlot);
    }

    private void createProductLowStockNotification(VendingSlot vendingSlot, User user) {
            String slotLabel = SlotLabelFormatter.toFrontendLabel(vendingSlot.getRowNumber(), vendingSlot.getColumnNumber());
            String message = "El stock del producto " + vendingSlot.getProduct().getName() + " en la ranura " + slotLabel + " de la máquina expendedora " + vendingSlot.getVendingMachine().getName() + " le quedan 3 unidades. Por favor, recargue el producto lo antes posible para evitar quedarse sin stock.";
            String link ="/vending-machines/" + vendingSlot.getVendingMachine().getId() + "/details";
            notificationService.createNotification(NotificationType.PRODUCT_LOW_STOCK,message, link, user);
    }

    private void createProductOutOfStockNotification(VendingSlot vendingSlot, User user) {
            String slotLabel = SlotLabelFormatter.toFrontendLabel(vendingSlot.getRowNumber(), vendingSlot.getColumnNumber());
            String message = "El stock del producto " + vendingSlot.getProduct().getName() + " en la ranura " + slotLabel + " de la máquina expendedora " + vendingSlot.getVendingMachine().getName() + " se ha agotado. Por favor, recargue el producto lo antes posible para evitar perder ventas.";
            String link ="/vending-machines/" + vendingSlot.getVendingMachine().getId() + "/details";
            notificationService.createNotification(NotificationType.PRODUCT_OUT_OF_STOCK,message, link, user);
    }
 
    public void checkUserAuthorization(VendingSlot vendingSlot, User user) {
        if (!vendingSlot.getVendingMachine().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("The user is not the owner of the vending machine.");
        }
    }

    public void checkVendingSlotIsEmpty(VendingSlot vendingSlot) {
        if (vendingSlot.getCurrentStock() > 0) {
            throw new ConflictException("The vending slot is not empty.");
        }
    }

}
