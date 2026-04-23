package com.expmatik.backend.maintenance;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.notification.NotificationService;
import com.expmatik.backend.notification.NotificationType;

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
            createDelayNotificationMaintainer(maintenance);
            createDelayNotificationAdministrator(maintenance);
        }
    }

    private void createDelayNotificationMaintainer(Maintenance maintenance) {
        String message = "Tienes una tarea de mantenimiento asignada para el día de ayer y todavía no la has completado. Por favor, revisa el mantenimiento y realízalo lo antes posible.";
        String link = "Unknown";
        notificationService.createNotification(NotificationType.DELAYED_MAINTENANCE, message, link, maintenance.getMaintainer());
    }

    private void createDelayNotificationAdministrator(Maintenance maintenance) {
        String message = "La tarea de mantenimiento asignada a " + maintenance.getMaintainer().getEmail() + " para el día de ayer todavía no ha sido completada. Por favor, revisa el mantenimiento y contacta con el mantenedor para que lo realice lo antes posible.";
        String link = "Unknown";
        notificationService.createNotification(NotificationType.DELAYED_MAINTENANCE, message, link, maintenance.getAdministrator());
    }
}
