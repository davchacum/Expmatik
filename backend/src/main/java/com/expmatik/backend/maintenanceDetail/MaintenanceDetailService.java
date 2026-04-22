package com.expmatik.backend.maintenanceDetail;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.maintenance.Maintenance;
import com.expmatik.backend.maintenanceDetail.DTOs.MaintenanceDetailCreate;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingSlot.VendingSlot;
import com.expmatik.backend.vendingSlot.VendingSlotService;


@Service
public class MaintenanceDetailService {

    private final MaintenanceDetailRepository maintenanceDetailRepository;
    private final VendingSlotService vendingSlotService;

    public MaintenanceDetailService(MaintenanceDetailRepository maintenanceDetailRepository, VendingSlotService vendingSlotService) {
        this.maintenanceDetailRepository = maintenanceDetailRepository;
        this.vendingSlotService = vendingSlotService;

    }

    @Transactional
    public MaintenanceDetail save(MaintenanceDetail maintenanceDetail) {
        return maintenanceDetailRepository.save(maintenanceDetail);
    }

    @Transactional
    public MaintenanceDetail createMaintenanceDetail(Maintenance maintenance, MaintenanceDetailCreate maintenanceDetailCreate, User user) {
        MaintenanceDetail newMaintenanceDetail = new MaintenanceDetail();
        newMaintenanceDetail.setExpirationDate(maintenanceDetailCreate.expirationDate());
        newMaintenanceDetail.setQuantityToRestock(maintenanceDetailCreate.quantityToRestock());
        VendingSlot vendingSlot = 
            vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(
                    maintenanceDetailCreate.vendingMachineName(),
                    maintenanceDetailCreate.rowNumber(),
                    maintenanceDetailCreate.columnNumber(),
                    user);
        newMaintenanceDetail.setRowNumber(maintenanceDetailCreate.rowNumber());
        newMaintenanceDetail.setColumnNumber(maintenanceDetailCreate.columnNumber());
        newMaintenanceDetail.setVendingMachine(vendingSlot.getVendingMachine());
        validateProduct(vendingSlot, maintenanceDetailCreate);
        validateExpirationDate(maintenance, maintenanceDetailCreate);
        validateSlotStock(maintenance.getMaintenanceDetails(), maintenanceDetailCreate, vendingSlot, user);
        newMaintenanceDetail.setProduct(vendingSlot.getProduct());
        return save(newMaintenanceDetail);
    }

    private void validateProduct(VendingSlot vendingSlot, MaintenanceDetailCreate newDetail) {
        if (!newDetail.barcode().equals(vendingSlot.getProduct().getBarcode())) {
            throw new BadRequestException("The barcode does not match the product in the vending slot.");
        }
    }

    private void validateSameMachine(List<MaintenanceDetail> currentMaintenanceDetails,MaintenanceDetailCreate newDetail) {
        if(!currentMaintenanceDetails.stream().allMatch(detail -> detail.getVendingMachine().getName().equals(newDetail.vendingMachineName()))){
            throw new BadRequestException("All maintenance details must be for the same vending machine.");
        }
    }

    private void validateSlotStock(List<MaintenanceDetail> currentMaintenanceDetails,MaintenanceDetailCreate newDetail,VendingSlot vendingSlot, User user) {
        validateSameMachine(currentMaintenanceDetails, newDetail);
        List<MaintenanceDetail> sameSlot = currentMaintenanceDetails.stream()
            .filter(detail -> detail.getRowNumber().equals(newDetail.rowNumber()) && detail.getColumnNumber().equals(newDetail.columnNumber()))
            .toList();
        Integer totalQuantity = sameSlot.stream().map(MaintenanceDetail::getQuantityToRestock).reduce(0, Integer::sum);
        if(totalQuantity + newDetail.quantityToRestock() > vendingSlot.getMaxCapacity()){
            throw new BadRequestException("The total quantity to restock for the same vending slot exceeds its maximum capacity.");
        }
    }

    private void validateExpirationDate(Maintenance maintenance, MaintenanceDetailCreate newDetail) {
        if (newDetail.expirationDate() != null && newDetail.expirationDate().isBefore(maintenance.getMaintenanceDate())) {
            throw new BadRequestException("The expiration date cannot be before the maintenance date.");
        }
    }

    @Transactional
    public void deleteMaintenanceDetail(MaintenanceDetail maintenanceDetail) {
        maintenanceDetailRepository.delete(maintenanceDetail);
    }

}
