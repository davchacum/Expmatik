package com.expmatik.backend.maintenanceDetail;

import org.springframework.stereotype.Service;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.maintenanceDetail.DTOs.MaintenanceDetailCreate;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingSlot.VendingSlot;
import com.expmatik.backend.vendingSlot.VendingSlotService;

import jakarta.transaction.Transactional;

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
    public MaintenanceDetail createMaintenanceDetail(MaintenanceDetailCreate maintenanceDetailCreate, User user) {
        MaintenanceDetail newMaintenanceDetail = new MaintenanceDetail();
        newMaintenanceDetail.setExpirationDate(maintenanceDetailCreate.expirationDate());
        newMaintenanceDetail.setQuantityToRestock(maintenanceDetailCreate.quantityToRestock());
        VendingSlot vendingSlot = vendingSlotService.getVendingSlotById(maintenanceDetailCreate.vendingSlotId(), user);
        newMaintenanceDetail.setVendingSlot(vendingSlot);

        if (maintenanceDetailCreate.barcode().equals(vendingSlot.getProduct().getBarcode())) {
            newMaintenanceDetail.setProduct(vendingSlot.getProduct());
        } else {
            throw new BadRequestException("The barcode does not match the product in the vending slot.");
        }
        return save(newMaintenanceDetail);
    }

}
