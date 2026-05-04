package com.expmatik.backend.maintenance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.expmatik.backend.maintenanceDetail.MaintenanceDetail;
import com.expmatik.backend.maintenanceDetail.MaintenanceDetailService;
import com.expmatik.backend.notification.NotificationService;
import com.expmatik.backend.notification.NotificationType;
import com.expmatik.backend.user.User;

@ExtendWith(MockitoExtension.class)
public class MaintenanceTaskTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MaintenanceDetailService maintenanceDetailService;

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

            @Test
            @DisplayName("checkAllMaintenances should release stock when expiration date is null")
            void testCheckAllMaintenances_NullExpiration_ShouldReleaseReservedStock() {
                LocalDate today = LocalDate.now();
                Maintenance delayedMaintenance = createMaintenance(MaintenanceStatus.DELAYED, today.minusDays(2));
                MaintenanceDetail detail = new MaintenanceDetail();
                detail.setExpirationDate(null);
                detail.setQuantityToRestock(2);
                delayedMaintenance.setMaintenanceDetails(List.of(detail));

                when(maintenanceRepository.findPendingMaintenancesByMaintenanceDateAfter(MaintenanceStatus.PENDING, today.minusDays(1))).thenReturn(List.of());
                when(maintenanceRepository.findDelayedMaintenanceByDetailsExpirationDate(MaintenanceStatus.DELAYED, today)).thenReturn(List.of(delayedMaintenance));

                maintenanceTask.checkAllMaintenances();

                verify(maintenanceDetailService).releaseReservedStock(detail, delayedMaintenance.getAdministrator());
            }

            @Test
            @DisplayName("checkAllMaintenances should release stock when expiration date is not expired yet")
            void testCheckAllMaintenances_FutureExpiration_ShouldReleaseReservedStock() {
                LocalDate today = LocalDate.now();
                Maintenance delayedMaintenance = createMaintenance(MaintenanceStatus.DELAYED, today.minusDays(2));
                MaintenanceDetail detail = new MaintenanceDetail();
                detail.setExpirationDate(today.plusDays(1));
                detail.setQuantityToRestock(2);
                delayedMaintenance.setMaintenanceDetails(List.of(detail));

                when(maintenanceRepository.findPendingMaintenancesByMaintenanceDateAfter(MaintenanceStatus.PENDING, today.minusDays(1))).thenReturn(List.of());
                when(maintenanceRepository.findDelayedMaintenanceByDetailsExpirationDate(MaintenanceStatus.DELAYED, today)).thenReturn(List.of(delayedMaintenance));

                maintenanceTask.checkAllMaintenances();

                verify(maintenanceDetailService, times(1)).releaseReservedStock(detail, delayedMaintenance.getAdministrator());
            }

            @Test
            @DisplayName("checkAllMaintenances should not release stock when expiration date is already expired")
            void testCheckAllMaintenances_ExpiredExpiration_ShouldNotReleaseReservedStock() {
                LocalDate today = LocalDate.now();
                Maintenance delayedMaintenance = createMaintenance(MaintenanceStatus.DELAYED, today.minusDays(2));
                MaintenanceDetail detail = new MaintenanceDetail();
                detail.setExpirationDate(today.minusDays(1));
                detail.setQuantityToRestock(2);
                delayedMaintenance.setMaintenanceDetails(List.of(detail));

                when(maintenanceRepository.findPendingMaintenancesByMaintenanceDateAfter(MaintenanceStatus.PENDING, today.minusDays(1))).thenReturn(List.of());
                when(maintenanceRepository.findDelayedMaintenanceByDetailsExpirationDate(MaintenanceStatus.DELAYED, today)).thenReturn(List.of(delayedMaintenance));

                maintenanceTask.checkAllMaintenances();

                verify(maintenanceDetailService, never()).releaseReservedStock(detail, delayedMaintenance.getAdministrator());
            }

            @Test
            @DisplayName("checkAllMaintenances should release only non-expired stock when maintenance becomes REJECTED_EXPIRED")
            void testCheckAllMaintenances_MixedExpiration_ShouldReleaseOnlyNonExpiredReservedStock() {
                LocalDate today = LocalDate.now();
                Maintenance delayedMaintenance = createMaintenance(MaintenanceStatus.DELAYED, today.minusDays(2));

                MaintenanceDetail expiredDetail = new MaintenanceDetail();
                expiredDetail.setExpirationDate(today.minusDays(1));
                expiredDetail.setQuantityToRestock(2);

                MaintenanceDetail futureDetail = new MaintenanceDetail();
                futureDetail.setExpirationDate(today.plusDays(1));
                futureDetail.setQuantityToRestock(3);

                delayedMaintenance.setMaintenanceDetails(List.of(expiredDetail, futureDetail));

                when(maintenanceRepository.findPendingMaintenancesByMaintenanceDateAfter(MaintenanceStatus.PENDING, today.minusDays(1))).thenReturn(List.of());
                when(maintenanceRepository.findDelayedMaintenanceByDetailsExpirationDate(MaintenanceStatus.DELAYED, today)).thenReturn(List.of(delayedMaintenance));

                maintenanceTask.checkAllMaintenances();

                verify(maintenanceDetailService, never()).releaseReservedStock(expiredDetail, delayedMaintenance.getAdministrator());
                verify(maintenanceDetailService).releaseReservedStock(futureDetail, delayedMaintenance.getAdministrator());
                verify(maintenanceRepository).save(delayedMaintenance);
            }
        }
    }
}
