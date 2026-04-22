package com.expmatik.backend.maintenance;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.notification.NotificationService; // Asumiendo que existe

@Component
public class MaintenanceTask {

    private final MaintenanceRepository maintenanceRepository;
    private final NotificationService notificationService;

    public MaintenanceTask(MaintenanceRepository maintenanceRepository, 
                          NotificationService notificationService) {
        this.maintenanceRepository = maintenanceRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void checkAllMaintenances() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<Maintenance> maintenances = maintenanceRepository.findPendingMaintenancesByMaintenanceDateAfter(MaintenanceStatus.PENDING, yesterday);
        for (Maintenance maintenance : maintenances) {
            maintenance.setStatus(MaintenanceStatus.DELAYED);
            maintenanceRepository.save(maintenance);
            //Crear notificacion de retraso
        }
    }
}
