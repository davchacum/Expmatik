package com.expmatik.backend.maintenance;

import java.time.LocalDateTime;
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
import com.expmatik.backend.user.Role;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;

@Service
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final UserService userService;
    private final MaintenanceDetailService maintenanceDetailService;

    public MaintenanceService(MaintenanceRepository maintenanceRepository, UserService userService, MaintenanceDetailService maintenanceDetailService) {
        this.maintenanceRepository = maintenanceRepository;
        this.userService = userService;
        this.maintenanceDetailService = maintenanceDetailService;

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
    public Maintenance updateStatus(UUID id, MaintenanceStatus newStatus, User user) {
        Maintenance maintenance = findById(id, user);
        if (newStatus == MaintenanceStatus.DRAFT) {
            throw new BadRequestException("Cannot change status back to DRAFT.");
        } else if (newStatus == MaintenanceStatus.PENDING) {
            if(maintenance.getStatus() != MaintenanceStatus.DRAFT) {
                throw new BadRequestException("Can only change status to PENDING from DRAFT.");
            }
            validateAdministrator(maintenance, user);
            //Notificacion
        } else if (newStatus == MaintenanceStatus.DELAYED) {
            throw new BadRequestException("Cannot change status to DELAYED manually. The system will automatically change the status to DELAYED if the maintenance is not completed within 24 hours of the scheduled maintenance date.");
            //Lo ejecuta el sistema
            //Notificacion
        } else if (newStatus == MaintenanceStatus.COMPLETED) {
            if(maintenance.getStatus() != MaintenanceStatus.PENDING && maintenance.getStatus() != MaintenanceStatus.DELAYED) {
                throw new BadRequestException("Can only change status to COMPLETED from PENDING or DELAYED.");
            }
            validateMaintainer(maintenance, user);
            //Notificacion
            maintenanceDetailService.performSlotsMaintenance(maintenance, user);
        }
        maintenance.setStatus(newStatus);

        return save(maintenance);
    }

    private void validateAdministrator(Maintenance maintenance, User user) {
        if (!maintenance.getAdministrator().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not the administrator of this maintenance record.");
        }
    }

    private void validateMaintainer(Maintenance maintenance, User user) {
        if (!maintenance.getMaintainer().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not the maintainer of this maintenance record.");
        }
    }


    @Transactional
    public Maintenance createMaintenance(MaintenanceCreate maintenanceCreate,User administrator) {
        Maintenance maintenance = new Maintenance();
        maintenance.setAdministrator(administrator);
        maintenance.setDescription(maintenanceCreate.description());
        maintenance.setMaintenanceDate(maintenanceCreate.maintenanceDate());
        User maintainer = userService.findByEmail(maintenanceCreate.maintainerEmail()).orElseThrow(() -> new ResourceNotFoundException("Maintainer not found with email: " + maintenanceCreate.maintainerEmail()));
        validateAdministrator(maintenance, administrator);
        maintenance.setMaintainer(maintainer);
        
        maintenance.setStatus(MaintenanceStatus.PENDING);
        return save(maintenance);
    }

    @Transactional(readOnly = true)
    public Page<Maintenance> searchMaintenances(User user, MaintenanceStatus status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        if (user.getRole() == Role.ADMINISTRATOR) {
            return maintenanceRepository.searchMaintenances(user.getId(), null, false, null, status, startDate, endDate, pageable);
        } else {
            return maintenanceRepository.searchMaintenances(null, user.getId(), true, MaintenanceStatus.DRAFT, status, startDate, endDate, pageable);
        }
    }

    @Transactional
    public void deleteMaintenance(UUID id, User user) {
        Maintenance maintenance = findById(id, user);
        validateAdministrator(maintenance, user);
        maintenanceRepository.delete(maintenance);
    }

    @Transactional
    public Maintenance updateMaintenance(UUID id, MaintenanceUpdate maintenanceUpdate, User user) {
        Maintenance maintenance = findById(id, user);
        validateAdministrator(maintenance, user);
        maintenance.setDescription(maintenanceUpdate.description());
        maintenance.setMaintainer(userService.findByEmail(maintenanceUpdate.maintainerEmail()).orElseThrow(() -> new ResourceNotFoundException("Maintainer not found with email: " + maintenanceUpdate.maintainerEmail())));
        return save(maintenance);
    }

    @Transactional
    public Maintenance addMaintenanceDetail(UUID maintenanceId, MaintenanceDetailCreate maintenanceDetailCreate, User user) {
        Maintenance maintenance = findById(maintenanceId, user);
        validateAdministrator(maintenance, user);
        MaintenanceDetail newDetail = maintenanceDetailService.createMaintenanceDetail(maintenance, maintenanceDetailCreate, user);
        maintenance.getMaintenanceDetails().add(newDetail);
        return save(maintenance);
    }

    @Transactional
    public Maintenance deleteMaintenanceDetail(UUID maintenanceId, UUID detailId, User user) {
        Maintenance maintenance = findById(maintenanceId, user);
        validateAdministrator(maintenance, user);
        MaintenanceDetail detailToDelete = maintenance.getMaintenanceDetails().stream()
            .filter(detail -> detail.getId().equals(detailId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Maintenance detail not found with id: " + detailId));

        maintenance.getMaintenanceDetails().remove(detailToDelete);
        maintenanceDetailService.deleteMaintenanceDetail(detailToDelete);
        return save(maintenance);
    }

}
