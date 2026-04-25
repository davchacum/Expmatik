package com.expmatik.backend.maintenanceDetail;

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
import com.expmatik.backend.vendingSlot.SlotLabelFormatter;
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
        validateSlotStock(maintenance.getMaintenanceDetails(), maintenanceDetailCreate, vendingSlot, user);
        newMaintenanceDetail.setProduct(vendingSlot.getProduct());
        reserveProductStock(newMaintenanceDetail.getProduct(), newMaintenanceDetail.getQuantityToRestock(), user);
        return newMaintenanceDetail;
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

    private void validateSlotStock(List<MaintenanceDetail> currentMaintenanceDetails,MaintenanceDetailCreate newDetail,VendingSlot vendingSlot, User user) {
        List<MaintenanceDetail> sameSlot = currentMaintenanceDetails.stream()
            .filter(detail -> detail.getRowNumber().equals(newDetail.rowNumber()) && detail.getColumnNumber().equals(newDetail.columnNumber()))
            .toList();
        Integer totalQuantity = sameSlot.stream().map(MaintenanceDetail::getQuantityToRestock).reduce(0, Integer::sum);
        if(totalQuantity + newDetail.quantityToRestock() > vendingSlot.getMaxCapacity()){
            throw new BadRequestException("The total quantity to restock for this slot: " + SlotLabelFormatter.toFrontendLabel(vendingSlot.getRowNumber(), vendingSlot.getColumnNumber()) + " exceeds its maximum capacity.");
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
    public void performSlotsMaintenance(Maintenance maintenance,User user) {
        List<VendingSlot> affectedSlots = maintenanceDetailRepository.findDistinctVendingSlotsByMaintenance(maintenance.getId());

        for (VendingSlot slot : affectedSlots) {
            List<MaintenanceDetail> detailsForSlot = maintenanceDetailRepository.findMaintenanceDetailsByMaintenanceIdAndSlotCoordinates(maintenance.getId(), slot.getVendingMachine().getName(), slot.getRowNumber(), slot.getColumnNumber());
            for (MaintenanceDetail detail : detailsForSlot) {
                vendingSlotService.addStockToVendingSlot(slot.getId(), detail.getQuantityToRestock(), detail.getExpirationDate(), user);
            }
        }
    }

    @Transactional(readOnly = true)
    public MaintenanceDetail findById(UUID id) {
        return maintenanceDetailRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Maintenance detail not found with id: " + id));
    }

}
