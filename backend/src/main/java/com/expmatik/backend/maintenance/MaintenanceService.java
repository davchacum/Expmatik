package com.expmatik.backend.maintenance;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.maintenance.DTOs.MaintenanceCreate;
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
        
        return maintenance;
    }

    @Transactional
    public Maintenance updateStatus(UUID id, MaintenanceStatus newStatus, User user) {
        Maintenance maintenance = findById(id, user);
        maintenance.setStatus(newStatus);
        return save(maintenance);
    }

    @Transactional
    public Maintenance createMaintenance(MaintenanceCreate maintenanceCreate,User administrator) {
        Maintenance maintenance = new Maintenance();
        maintenance.setAdministrator(administrator);
        maintenance.setDescription(maintenanceCreate.description());
        maintenance.setMaintenanceDate(maintenanceCreate.maintenanceDate());
        User maintainer = userService.findByEmail(maintenanceCreate.maintainerEmail()).orElseThrow(() -> new ResourceNotFoundException("Maintainer not found with email: " + maintenanceCreate.maintainerEmail()));
        if (maintainer.getRole() != Role.MAINTAINER) {
            throw new BadRequestException("The specified user is not a maintainer.");
        }
        maintenance.setMaintainer(maintainer);
        
        maintenance.setStatus(MaintenanceStatus.PENDING);
        List<MaintenanceDetail> details = new ArrayList<>();
        for (MaintenanceDetailCreate detail : maintenanceCreate.maintenanceDetails()) {
            details.add(maintenanceDetailService.createMaintenanceDetail(detail, administrator));
        }
        return save(maintenance);
    }

    @Transactional(readOnly = true)
    public Page<Maintenance> searchMaintenances(User user, MaintenanceStatus status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        if (user.getRole() == Role.ADMINISTRATOR) {
            return maintenanceRepository.searchMaintenances(user.getId(), null, status, startDate, endDate, pageable);
        } else {
            return maintenanceRepository.searchMaintenances(null, user.getId(), status, startDate, endDate, pageable);
        }
    }

}
