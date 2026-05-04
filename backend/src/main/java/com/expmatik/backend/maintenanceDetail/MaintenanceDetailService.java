package com.expmatik.backend.maintenanceDetail;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.maintenance.Maintenance;
import com.expmatik.backend.maintenanceDetail.DTOs.MaintenanceDetailCreate;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.productInfo.ProductInfo;
import com.expmatik.backend.productInfo.ProductInfoService;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingSlot.VendingSlot;
import com.expmatik.backend.vendingSlot.VendingSlotService;


@Service
public class MaintenanceDetailService {

    private final MaintenanceDetailRepository maintenanceDetailRepository;
    private final VendingSlotService vendingSlotService;
    private final ProductInfoService productInfoService;

    public MaintenanceDetailService(MaintenanceDetailRepository maintenanceDetailRepository, VendingSlotService vendingSlotService, ProductInfoService productInfoService) {
        this.maintenanceDetailRepository = maintenanceDetailRepository;
        this.vendingSlotService = vendingSlotService;
        this.productInfoService = productInfoService;

    }

    @Transactional
    public MaintenanceDetail createMaintenanceDetail(Maintenance maintenance, MaintenanceDetailCreate maintenanceDetailCreate, User user) {
        MaintenanceDetail newMaintenanceDetail = new MaintenanceDetail();
        newMaintenanceDetail.setExpirationDate(maintenanceDetailCreate.expirationDate());
        newMaintenanceDetail.setQuantityToRestock(maintenanceDetailCreate.quantityToRestock());
        VendingSlot vendingSlot =
            vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(
                    maintenance.getVendingMachine().getName(),
                    maintenanceDetailCreate.rowNumber(),
                    maintenanceDetailCreate.columnNumber(),
                    user);
        newMaintenanceDetail.setRowNumber(maintenanceDetailCreate.rowNumber());
        newMaintenanceDetail.setColumnNumber(maintenanceDetailCreate.columnNumber());
        validateProduct(vendingSlot, maintenanceDetailCreate);
        validateExpirationDate(maintenance, maintenanceDetailCreate, vendingSlot.getProduct());
        validateSlotCapacity(maintenance, newMaintenanceDetail, vendingSlot);
        newMaintenanceDetail.setProduct(vendingSlot.getProduct());
        reserveProductStock(newMaintenanceDetail.getProduct(), newMaintenanceDetail.getQuantityToRestock(), user);
        return newMaintenanceDetail;
    }

    private void validateSlotCapacity(Maintenance maintenance, MaintenanceDetail newDetail, VendingSlot vendingSlot) {
        Integer alreadyRequestedForSlot = maintenanceDetailRepository.sumQuantityToRestockByMaintenanceIdAndSlotCoordinates(
            maintenance.getId(),
            maintenance.getVendingMachine().getName(),
            newDetail.getRowNumber(),
            newDetail.getColumnNumber()
        );

        int availableSpace = vendingSlot.getMaxCapacity();
        int totalRequestedForSlot = alreadyRequestedForSlot + newDetail.getQuantityToRestock();

        if (totalRequestedForSlot > availableSpace) {
            throw new ConflictException(
                "The total quantity requested for this vending slot exceeds the available space. Available space: "
                + availableSpace + ", requested: " + totalRequestedForSlot + "."
            );
        }
    }

    private void reserveProductStock(Product product, Integer quantityToReserve, User user) {
        ProductInfo productInfo = productInfoService.getOrCreateProductInfo(product.getId(), user, null);
        if (productInfo.getStockQuantity() < quantityToReserve) {
            throw new ConflictException("Insufficient stock in inventory to create this maintenance detail.");
        }
        productInfoService.editStockQuantity(productInfo.getId(), user, -quantityToReserve, null);
    }

    @Transactional
    public void releaseReservedStock(MaintenanceDetail maintenanceDetail, User user) {
        ProductInfo productInfo = productInfoService.getOrCreateProductInfo(maintenanceDetail.getProduct().getId(), user, null);
        productInfoService.editStockQuantity(productInfo.getId(), user, maintenanceDetail.getQuantityToRestock(), null);
    }

    private void validateProduct(VendingSlot vendingSlot, MaintenanceDetailCreate newDetail) {
        if( vendingSlot.getProduct() == null) {
            throw new BadRequestException("The vending slot does not have a product assigned.");
        }

        if (!newDetail.barcode().equals(vendingSlot.getProduct().getBarcode())) {
            throw new BadRequestException("The barcode does not match the product in the vending slot.");
        }
    }

    private void validateExpirationDate(Maintenance maintenance, MaintenanceDetailCreate newDetail, Product product) {
        if(product.getIsPerishable()){
            if (newDetail.expirationDate() == null) {
                throw new BadRequestException("Expiration date is required for perishable products.");
            }
            if (newDetail.expirationDate().isBefore(maintenance.getMaintenanceDate())) {
                throw new BadRequestException("The expiration date cannot be before the maintenance date.");
            }
        }else{
            if (newDetail.expirationDate() != null) {
                throw new BadRequestException("Expiration date should not be provided for non-perishable products.");
            }
        }
    }

    @Transactional
    public void performSlotsMaintenance(Maintenance maintenance, User user) {
        List<VendingSlot> affectedSlots = maintenanceDetailRepository.findDistinctVendingSlotsByMaintenance(maintenance.getId());
        User administrator = maintenance.getAdministrator();

        for (VendingSlot slot : affectedSlots) {
            List<MaintenanceDetail> detailsForSlot = maintenanceDetailRepository.findMaintenanceDetailsByMaintenanceIdAndSlotCoordinates(
                maintenance.getId(), slot.getVendingMachine().getName(), slot.getRowNumber(), slot.getColumnNumber());

            int remainingSpace = slot.getMaxCapacity() - slot.getCurrentStock();

            for (MaintenanceDetail detail : detailsForSlot) {
                int quantityToAdd = Math.min(detail.getQuantityToRestock(), remainingSpace);
                int quantityToReturn = detail.getQuantityToRestock() - quantityToAdd;

                boolean isExpired = detail.getProduct().getIsPerishable()
                        && detail.getExpirationDate() != null
                        && detail.getExpirationDate().isBefore(LocalDate.now());
                if (isExpired) {
                    quantityToReturn += quantityToAdd;
                    quantityToAdd = 0;
                }

                if (quantityToAdd > 0) {
                    vendingSlotService.addStockToVendingSlot(slot.getId(), quantityToAdd, detail.getExpirationDate(), user);
                }
                if (quantityToReturn > 0) {
                    releasePartialStock(detail.getProduct(), quantityToReturn, administrator);
                }

                detail.setQuantityRestocked(quantityToAdd);
                detail.setQuantityReturned(quantityToReturn);
                maintenanceDetailRepository.save(detail);

                remainingSpace -= quantityToAdd;
            }
        }
    }

    private void releasePartialStock(Product product, Integer quantity, User user) {
        ProductInfo productInfo = productInfoService.getOrCreateProductInfo(product.getId(), user, null);
        productInfoService.editStockQuantity(productInfo.getId(), user, quantity, null);
    }

    @Transactional(readOnly = true)
    public MaintenanceDetail findById(UUID id) {
        return maintenanceDetailRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Maintenance detail not found with id: " + id));
    }

}
