package com.expmatik.backend.maintenance;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.maintenance.DTOs.MaintenanceCreate;
import com.expmatik.backend.maintenance.DTOs.MaintenanceUpdate;
import com.expmatik.backend.maintenanceDetail.MaintenanceDetail;
import com.expmatik.backend.maintenanceDetail.MaintenanceDetailService;
import com.expmatik.backend.maintenanceDetail.DTOs.MaintenanceDetailCreate;
import com.expmatik.backend.notification.NotificationService;
import com.expmatik.backend.notification.NotificationType;
import com.expmatik.backend.user.Role;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;
import com.expmatik.backend.vendingMachine.VendingMachine;
import com.expmatik.backend.vendingMachine.VendingMachineService;

@Service
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final UserService userService;
    private final VendingMachineService vendingMachineService;
    private final MaintenanceDetailService maintenanceDetailService;
    private final NotificationService notificationService;

    public MaintenanceService(MaintenanceRepository maintenanceRepository, UserService userService, VendingMachineService vendingMachineService, MaintenanceDetailService maintenanceDetailService, NotificationService notificationService) {
        this.maintenanceRepository = maintenanceRepository;
        this.userService = userService;
        this.vendingMachineService = vendingMachineService;
        this.maintenanceDetailService = maintenanceDetailService;
        this.notificationService = notificationService;

    }

    @Transactional
    public Maintenance save(Maintenance maintenance) {
        return maintenanceRepository.save(maintenance);
    }

    @Transactional(readOnly = true)
    public Maintenance findById(UUID id,User user) {
        Maintenance maintenance = maintenanceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Maintenance not found with id: " + id));
        
        if (!(maintenance.getAdministrator().getId().equals(user.getId()) || maintenance.getMaintainer().getId().equals(user.getId()))) {
            throw new AccessDeniedException("You are not the administrator or maintainer of this maintenance record.");
        }
        if (maintenance.getStatus() == MaintenanceStatus.DRAFT && user.getRole() != Role.ADMINISTRATOR) {
            throw new AccessDeniedException("Only administrators can access DRAFT maintenance records.");
        }
        
        return maintenance;
    }

    @Transactional
    public Maintenance pendingMaintenance(UUID id,User user){
        Maintenance maintenance = findById(id, user);
        if(maintenance.getStatus() != MaintenanceStatus.DRAFT){
            throw new BadRequestException("Maintenance status must be DRAFT to be set as PENDING.");
        }
        if (maintenance.getMaintenanceDetails().isEmpty()) {
                throw new BadRequestException("Cannot change status to PENDING without any maintenance details. Please add at least one maintenance detail before changing the status to PENDING.");
        }
        createPendingNotification(maintenance);
        maintenance.setStatus(MaintenanceStatus.PENDING);
        return save(maintenance);

    }

    @Transactional
    public Maintenance completedMaintenance(UUID id,User user){

        Maintenance maintenance = findById(id, user);
        if(!(maintenance.getStatus() == MaintenanceStatus.PENDING || maintenance.getStatus() == MaintenanceStatus.DELAYED)){
            throw new BadRequestException("Maintenance status must be PENDING or DELAYED to be set as COMPLETED.");
        }
        maintenanceDetailService.performSlotsMaintenance(maintenance, user);
        createCompletedNotification(maintenance);
        maintenance.setStatus(MaintenanceStatus.COMPLETED);
        return save(maintenance);

    }

    private void createPendingNotification(Maintenance maintenance) {
        String message = "Tienes una tarea de mantenimiento asignada para el día " + maintenance.getMaintenanceDate() + ". Por favor, revisa el mantenimiento y realízalo lo antes posible.";
        String link = "Unknown";
        notificationService.createNotification(NotificationType.ASSIGNED_RESTOCKING, message, link, maintenance.getMaintainer());
    }

    private void createCompletedNotification(Maintenance maintenance) {
        String message = "La tarea de mantenimiento asignada a " + maintenance.getMaintainer().getEmail() + " para el día " + maintenance.getMaintenanceDate() + " ha sido completada.";
        String link = "Unknown";
        notificationService.createNotification(NotificationType.COMPLETED_RESTOCKING, message, link, maintenance.getAdministrator());
    }

    @Transactional
    public Maintenance createMaintenance(MaintenanceCreate maintenanceCreate,User administrator) {
        Maintenance maintenance = new Maintenance();
        maintenance.setAdministrator(administrator);
        maintenance.setDescription(maintenanceCreate.description());
        maintenance.setMaintenanceDate(maintenanceCreate.maintenanceDate());
        User maintainer = userService.findByEmail(maintenanceCreate.maintainerEmail()).orElseThrow(() -> new ResourceNotFoundException("Maintainer not found with email: " + maintenanceCreate.maintainerEmail()));
        if (!maintainer.getRole().equals(Role.MAINTAINER) ) {
            throw new BadRequestException("Only users with the role of MAINTAINER can be assigned to maintenance tasks.");
        }
        VendingMachine vendingMachine = vendingMachineService.findVendingMachineByNameAndUserId(maintenanceCreate.vendingMachineName(), administrator);
        maintenance.setMaintainer(maintainer);
        maintenance.setVendingMachine(vendingMachine);
        maintenance.setMaintenanceDetails(new ArrayList<>());
        
        maintenance.setStatus(MaintenanceStatus.DRAFT);
        return save(maintenance);
    }

    @Transactional(readOnly = true)
    public Page<Maintenance> searchMaintenances(User user, MaintenanceStatus status,String machineName, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        String machineNameParam = (machineName != null && !machineName.isBlank()) ? machineName : null;
        if (user.getRole() == Role.ADMINISTRATOR) {
            return maintenanceRepository.searchMaintenances(user.getId(), null, false, null, status, machineNameParam, startDate, endDate, pageable);
        } else {
            return maintenanceRepository.searchMaintenances(null, user.getId(), true, MaintenanceStatus.DRAFT, status, machineNameParam, startDate, endDate, pageable);
        }
    }

    @Transactional
    public void deleteMaintenance(UUID id, User user) {
        Maintenance maintenance = findById(id, user);
        if (maintenance.getStatus() != MaintenanceStatus.DRAFT) {
            throw new BadRequestException("Only maintenance records in DRAFT status can be deleted.");
        }

        for (MaintenanceDetail detail : maintenance.getMaintenanceDetails()) {
            maintenanceDetailService.releaseReservedStock(detail, user);
        }

        maintenanceRepository.delete(maintenance);
    }

    @Transactional
    public Maintenance updateMaintenance(UUID id, MaintenanceUpdate maintenanceUpdate, User user) {
        Maintenance maintenance = findById(id, user);
        if (maintenance.getStatus() != MaintenanceStatus.DRAFT) {
            throw new BadRequestException("Only maintenance records in DRAFT status can be updated.");
        }
        maintenance.setDescription(maintenanceUpdate.description());
        User maintainer = userService.findByEmail(maintenanceUpdate.maintainerEmail()).orElseThrow(() -> new ResourceNotFoundException("Maintainer not found with email: " + maintenanceUpdate.maintainerEmail()));
        if (!maintainer.getRole().equals(Role.MAINTAINER) ) {
            throw new BadRequestException("Only users with the role of MAINTAINER can be assigned to maintenance tasks.");
        }
        maintenance.setMaintainer(maintainer);
        return save(maintenance);
    }

    @Transactional
    public Maintenance addMaintenanceDetail(UUID maintenanceId, MaintenanceDetailCreate maintenanceDetailCreate, User user) {
        Maintenance maintenance = findById(maintenanceId, user);
        if (maintenance.getStatus() != MaintenanceStatus.DRAFT) {
            throw new BadRequestException("Can only add maintenance details to DRAFT maintenance records.");
        }
        MaintenanceDetail newDetail = maintenanceDetailService.createMaintenanceDetail(maintenance, maintenanceDetailCreate, user);
        maintenance.getMaintenanceDetails().add(newDetail);
        return save(maintenance);
    }

    @Transactional
    public Maintenance deleteMaintenanceDetail(UUID maintenanceId, UUID detailId, User user) {
        Maintenance maintenance = findById(maintenanceId, user);
        if (maintenance.getStatus() != MaintenanceStatus.DRAFT) {
            throw new BadRequestException("Can only delete maintenance details from DRAFT maintenance records.");
        }
        MaintenanceDetail detailToDelete = maintenanceDetailService.findById(detailId);
        if (!maintenance.getMaintenanceDetails().contains(detailToDelete)) {
            throw new BadRequestException("The specified maintenance detail does not belong to this maintenance record.");
        }

        maintenanceDetailService.releaseReservedStock(detailToDelete, user);
        maintenance.getMaintenanceDetails().remove(detailToDelete);
        return save(maintenance);
    }

}
