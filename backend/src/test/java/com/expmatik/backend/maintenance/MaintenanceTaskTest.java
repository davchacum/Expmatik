package com.expmatik.backend.maintenance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.expmatik.backend.notification.NotificationService;
import com.expmatik.backend.notification.NotificationType;
import com.expmatik.backend.user.User;

@ExtendWith(MockitoExtension.class)
public class MaintenanceTaskTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private NotificationService notificationService;

    @Spy
    @InjectMocks
    private MaintenanceTask maintenanceTask;

    private Maintenance createMaintenance(MaintenanceStatus status, LocalDate maintenanceDate) {

        User maintainer = new User();
        maintainer.setEmail("maintainer@example.com");

        User administrator = new User();
        administrator.setEmail("administrator@example.com");

        Maintenance maintenance = new Maintenance();
        maintenance.setStatus(status);
        maintenance.setMaintenanceDate(maintenanceDate);
        maintenance.setMaintainer(maintainer);
        maintenance.setAdministrator(administrator);
        return maintenance;
    }

    @Nested
    @DisplayName("checkAllMaintenances")
    class CheckAllMaintenances {

        @Nested
        @DisplayName("Generate Notification Cases")
        class GenerateNotificationCases {

            @Test
            @DisplayName("checkAllMaintenances should create notifications for delayed maintenances")
            void testCheckAllMaintenances_ShouldCreateNotificationsForDelayedMaintenances() {

                LocalDate yesterday = LocalDate.now().minusDays(1);
                Maintenance maintenance = createMaintenance(MaintenanceStatus.PENDING, yesterday);
                when(maintenanceRepository.findPendingMaintenancesByMaintenanceDateAfter(MaintenanceStatus.PENDING, yesterday)).thenReturn(List.of(maintenance));

                maintenanceTask.checkAllMaintenances();
                verify(maintenanceRepository).save(maintenance);
                verify(notificationService).createNotification(
                    eq(NotificationType.DELAYED_MAINTENANCE),
                    any(String.class),
                    any(String.class),
                    eq(maintenance.getMaintainer())
                );

                verify(notificationService).createNotification(
                    eq(NotificationType.DELAYED_MAINTENANCE),
                    any(String.class),
                    any(String.class),
                    eq(maintenance.getAdministrator())
                );
            }
        }
    }
}
