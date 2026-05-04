package com.expmatik.backend.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.maintenance.DTOs.MaintenanceCreate;
import com.expmatik.backend.maintenance.DTOs.MaintenanceUpdate;
import com.expmatik.backend.maintenanceDetail.MaintenanceDetail;
import com.expmatik.backend.maintenanceDetail.MaintenanceDetailService;
import com.expmatik.backend.maintenanceDetail.DTOs.MaintenanceDetailCreate;
import com.expmatik.backend.notification.NotificationService;
import com.expmatik.backend.notification.NotificationType;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.user.Role;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;
import com.expmatik.backend.vendingMachine.VendingMachine;
import com.expmatik.backend.vendingMachine.VendingMachineService;
import com.expmatik.backend.vendingSlot.VendingSlot;

@ExtendWith(MockitoExtension.class)
public class MaintenanceServiceTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private UserService userService;

    @Mock
    private MaintenanceDetailService maintenanceDetailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private VendingMachineService vendingMachineService;

    @Spy
    @InjectMocks
    private MaintenanceService maintenanceService;

    private Maintenance maintenance;
    private User maintainer;
    private User administrator;
    private User otherUser;
    private List<MaintenanceDetail> maintenanceDetails;
    private Product product;
    private VendingMachine vendingMachine;
    private VendingSlot vendingSlot;

    @BeforeEach
    void setUp() {
        maintainer = new User();
        maintainer.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        maintainer.setRole(Role.MAINTAINER);

        administrator = new User();
        administrator.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        administrator.setRole(Role.ADMINISTRATOR);

        otherUser = new User();
        otherUser.setId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        otherUser.setRole(Role.MAINTAINER);

        product = new Product();
        product.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        vendingMachine = new VendingMachine();
        vendingMachine.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        vendingSlot = new VendingSlot();
        vendingSlot.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        vendingSlot.setRowNumber(1);
        vendingSlot.setColumnNumber(1);
        vendingSlot.setProduct(product);
        vendingSlot.setVendingMachine(vendingMachine);

        MaintenanceDetail detail = new MaintenanceDetail();
        detail.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        detail.setQuantityToRestock(10);
        detail.setRowNumber(1);
        detail.setColumnNumber(1);
        detail.setProduct(product);


        maintenanceDetails = new LinkedList<>();
        maintenanceDetails.add(detail);

        maintenance = new Maintenance();
        maintenance.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        maintenance.setMaintenanceDate(java.time.LocalDate.now());
        maintenance.setStatus(MaintenanceStatus.DRAFT);
        maintenance.setDescription("Test maintenance");
        maintenance.setMaintainer(maintainer);
        maintenance.setAdministrator(administrator);
        maintenance.setVendingMachine(vendingMachine);
        maintenance.setMaintenanceDetails(maintenanceDetails);
    }

    @Nested
    @DisplayName("findById")
    class FindById {
        
        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should return maintenance when ID is valid and user is authorized as maintainer")
            void testFindById_validIdAndAuthorizedMaintainerUser_shouldReturnMaintenance() {
                maintenance.setStatus(MaintenanceStatus.PENDING);

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                Maintenance result = maintenanceService.findById(UUID.randomUUID(), maintainer);
                assertEquals(result.getMaintainer().getId(), maintainer.getId());
            }

            @Test
            @DisplayName("should return maintenance when ID is valid and user is authorized as administrator")
            void testFindById_validIdAndAuthorizedAdministratorUser_shouldReturnMaintenance() {
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                Maintenance result = maintenanceService.findById(UUID.randomUUID(), administrator);
                assertEquals(result.getAdministrator().getId(), administrator.getId());
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {
            @Test
            @DisplayName("should throw ResourceNotFoundException when maintenance does not exist")
            void testFindById_maintenanceDoesNotExist_shouldThrowResourceNotFoundException() {
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> maintenanceService.findById(UUID.randomUUID(), maintainer));

            }

            @Test
            @DisplayName("should throw AccessDeniedException when user is authorized as maintainer and maintenance is in DRAFT status")
            void testFindById_authorizedMaintainerAndDraftStatus_shouldThrowAccessDeniedException() {
                maintenance.setStatus(MaintenanceStatus.DRAFT);
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.findById(UUID.randomUUID(), maintainer));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when user is not authorized")
            void testFindById_userNotAuthorized_shouldThrowAccessDeniedException() {
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.findById(UUID.randomUUID(), otherUser));
            }

        }
    }

    @Nested
    @DisplayName("pendingMaintenance")
    class PendingMaintenance {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should update status to PENDING when current status is DRAFT and user is maintainer")
            void testPendingMaintenance_toPendingFromDraftByMaintainer_shouldUpdateStatus() {
                maintenance.setStatus(MaintenanceStatus.DRAFT);
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));
                when(notificationService.createNotification(any(NotificationType.class), any(String.class),any(String.class), any(User.class))).thenReturn(null);
                when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(maintenance);

                Maintenance result = maintenanceService.pendingMaintenance(UUID.randomUUID(), administrator);
                assertEquals(result.getStatus(), MaintenanceStatus.PENDING);
                verify(notificationService).createNotification(
                    eq(NotificationType.ASSIGNED_RESTOCKING),
                    any(String.class),
                    any(String.class),
                    eq(maintenance.getMaintainer())
                );
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should throw AccessDeniedException when user is not authorized to update status")
            void testPendingMaintenance_userNotAuthorized_shouldThrowAccessDeniedException() {
                maintenance.setStatus(MaintenanceStatus.PENDING);
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.pendingMaintenance(UUID.randomUUID(), otherUser));
            }

            @ParameterizedTest
            @ValueSource(strings = {"REJECTED_EXPIRED", "PENDING", "DELAYED", "COMPLETED", "CANCELED"})
            void testPendingMaintenance_toPendingFromInvalidStatus_shouldThrowBadRequestException(String invalidStatus) {
                maintenance.setStatus(MaintenanceStatus.valueOf(invalidStatus));
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(BadRequestException.class, () -> maintenanceService.pendingMaintenance(UUID.randomUUID(), administrator));
            }


            @Test
            @DisplayName("should throw BadRequestException when trying to update to PENDING from DRAFT status without any maintenance details")
            void testPendingMaintenance_toPendingFromDraftWithoutDetails_shouldThrowBadRequestException() {
                maintenance.setStatus(MaintenanceStatus.DRAFT);
                maintenance.setMaintenanceDetails(List.of());
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(BadRequestException.class, () -> maintenanceService.pendingMaintenance(UUID.randomUUID(), administrator));
            }
        }
    }

    @Nested
    @DisplayName("completedMaintenance")
    class CompletedMaintenance {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should update status to COMPLETED when current status is PENDING and user is maintainer")
            void testCompletedMaintenance_fromPendingByMaintainer_shouldUpdateStatus() {
                maintenance.setStatus(MaintenanceStatus.PENDING);
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));
                when(notificationService.createNotification(any(NotificationType.class), any(String.class),any(String.class), any(User.class))).thenReturn(null);
                when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(maintenance);

                Maintenance result = maintenanceService.completedMaintenance(UUID.randomUUID(), maintainer);
                assertEquals(result.getStatus(), MaintenanceStatus.COMPLETED);
                verify(notificationService).createNotification(
                    eq(NotificationType.COMPLETED_RESTOCKING),
                    any(String.class),
                    any(String.class),
                    eq(maintenance.getAdministrator())
                );
            }

            @Test
            @DisplayName("should update status to COMPLETED when current status is DELAYED and user is maintainer")
            void testCompletedMaintenance_fromDelayedByMaintainer_shouldUpdateStatus() {
                maintenance.setStatus(MaintenanceStatus.DELAYED);
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));
                when(notificationService.createNotification(any(NotificationType.class), any(String.class),any(String.class), any(User.class))).thenReturn(null);
                when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(maintenance);
                
                Maintenance result = maintenanceService.completedMaintenance(UUID.randomUUID(), maintainer);
                assertEquals(result.getStatus(), MaintenanceStatus.COMPLETED);
                verify(notificationService).createNotification(
                    eq(NotificationType.COMPLETED_RESTOCKING),
                    any(String.class),
                    any(String.class),
                    eq(maintenance.getAdministrator())
                );
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should throw AccessDeniedException when user is not authorized to complete maintenance")
            void testCompleteMaintenance_userNotAuthorized_shouldThrowAccessDeniedException() {
                maintenance.setStatus(MaintenanceStatus.PENDING);
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.completedMaintenance(UUID.randomUUID(), otherUser));
            }

            @ParameterizedTest
            @ValueSource(strings = {"REJECTED_EXPIRED", "DRAFT", "COMPLETED", "CANCELED"})
            void testCompletedMaintenance_toCompletedFromInvalidStatus_shouldThrowBadRequestException(String invalidStatus) {
                maintenance.setStatus(MaintenanceStatus.valueOf(invalidStatus));
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(BadRequestException.class, () -> maintenanceService.completedMaintenance(UUID.randomUUID(), administrator));
            }
        }
    }

    @Nested
    @DisplayName("createMaintenance")
    class CreateMaintenance {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should create maintenance successfully")
            void testCreateMaintenance_validInput_shouldCreateMaintenance() {

                MaintenanceCreate maintenanceCreate = new MaintenanceCreate(
                    LocalDate.now(),
                    "Test maintenance",
                    "maintainer@example.com",
                    "Vending Machine 1"
                );
                when(userService.findByEmail(any(String.class))).thenReturn(Optional.of(maintainer));
                when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(maintenance);
                when(vendingMachineService.findVendingMachineByNameAndUserId(any(String.class), any(User.class))).thenReturn(vendingMachine);

                Maintenance result = maintenanceService.createMaintenance(maintenanceCreate, administrator);
                assertEquals(result.getId(), maintenance.getId());
                assertEquals(result.getStatus(), MaintenanceStatus.DRAFT);
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should throw BadRequestException when maintainer has ADMINISTRATOR role")
            void testCreateMaintenance_maintainerHasAdministratorRole_shouldThrowBadRequestException() {
                MaintenanceCreate maintenanceCreate = new MaintenanceCreate(
                    LocalDate.now(),
                    "Test maintenance",
                    "administrator@example.com",
                    "Vending Machine 1"
                );
                when(userService.findByEmail(any(String.class))).thenReturn(Optional.of(administrator));

                assertThrows(BadRequestException.class, () -> maintenanceService.createMaintenance(maintenanceCreate, administrator));
            }

            @Test
            @DisplayName("should throw ResourceNotFoundException when maintainer email does not exist")
            void testCreateMaintenance_maintainerEmailDoesNotExist_shouldThrowResourceNotFoundException() {
                MaintenanceCreate maintenanceCreate = new MaintenanceCreate(
                    LocalDate.now(),
                    "Test maintenance",
                    "nonexistent@example.com",
                    "Vending Machine 1"
                );
                when(userService.findByEmail(any(String.class))).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> maintenanceService.createMaintenance(maintenanceCreate, administrator));
            }

        }
    }

    @Nested
    @DisplayName("searchMaintenances")
    class SearchMaintenances {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should call repository with admin filters")
            void testSearchMaintenances_adminPath() {
                when(maintenanceRepository.searchMaintenances(
                        any(), any(), anyBoolean(), any(), any() ,any(), any(), any(), any()
                )).thenReturn(Page.empty());

                maintenanceService.searchMaintenances(
                        administrator, null, null, null, null, Pageable.unpaged()
                );

                verify(maintenanceRepository).searchMaintenances(
                        eq(administrator.getId()), 
                        isNull(),
                        eq(false),               
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Pageable.class)
                );
            }

            @Test
            @DisplayName("should call repository with maintainer filters")
            void testSearchMaintenances_maintainerPath() {
                when(maintenanceRepository.searchMaintenances(
                        any(), any(), anyBoolean(), any(), any() ,any(), any(), any(), any()
                )).thenReturn(Page.empty());

                maintenanceService.searchMaintenances(
                        maintainer, MaintenanceStatus.DRAFT, null, null,null, Pageable.unpaged()
                );

                verify(maintenanceRepository).searchMaintenances(
                        isNull(),
                        eq(maintainer.getId()),
                        eq(true),
                        eq(MaintenanceStatus.DRAFT),
                        eq(MaintenanceStatus.DRAFT),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Pageable.class)
                );
            }
        }
    }

    @Nested
    @DisplayName("deleteMaintenance")
    class DeleteMaintenance {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should delete maintenance successfully")
            void testDeleteMaintenance_validIdAndAuthorizedUser_shouldDeleteMaintenance() {
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                maintenanceService.deleteMaintenance(UUID.randomUUID(), administrator);
                verify(maintenanceRepository).delete(any(Maintenance.class));
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should throw ResourceNotFoundException when maintenance does not exist")
            void testDeleteMaintenance_maintenanceDoesNotExist_shouldThrowResourceNotFoundException() {
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> maintenanceService.deleteMaintenance(UUID.randomUUID(), administrator));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when user is not authorized to delete")
            void testDeleteMaintenance_userNotAuthorized_shouldThrowAccessDeniedException() {
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.deleteMaintenance(UUID.randomUUID(), otherUser));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when maintainer tries to delete")
            void testDeleteMaintenance_maintainerTriesToDelete_shouldThrowAccessDeniedException() {
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.deleteMaintenance(UUID.randomUUID(), maintainer));
            }

            @Test
            @DisplayName("should throw BadRequestException when trying to delete a non-DRAFT maintenance")
            void testDeleteMaintenance_nonDraftMaintenance_shouldThrowBadRequestException() {
                maintenance.setStatus(MaintenanceStatus.PENDING);
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(BadRequestException.class, () -> maintenanceService.deleteMaintenance(UUID.randomUUID(), administrator));
            }
        }
    }

    @Nested
    @DisplayName("cancelMaintenance")
    class CancelMaintenance {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should cancel maintenance and release stock when status is PENDING")
            void testCancelMaintenance_fromPendingByAdministrator_shouldCancelAndReleaseStock() {
                maintenance.setStatus(MaintenanceStatus.PENDING);
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));
                when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(maintenance);

                Maintenance result = maintenanceService.cancelMaintenance(UUID.randomUUID(), administrator);

                assertEquals(MaintenanceStatus.CANCELED, result.getStatus());
                verify(maintenanceDetailService).releaseReservedStock(eq(maintenanceDetails.get(0)), eq(administrator));
            }

            @Test
            @DisplayName("should cancel maintenance and release stock when status is DELAYED")
            void testCancelMaintenance_fromDelayedByAdministrator_shouldCancelAndReleaseStock() {
                maintenance.setStatus(MaintenanceStatus.DELAYED);
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));
                when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(maintenance);

                Maintenance result = maintenanceService.cancelMaintenance(UUID.randomUUID(), administrator);

                assertEquals(MaintenanceStatus.CANCELED, result.getStatus());
                verify(maintenanceDetailService).releaseReservedStock(eq(maintenanceDetails.get(0)), eq(administrator));
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should throw ResourceNotFoundException when maintenance does not exist")
            void testCancelMaintenance_maintenanceDoesNotExist_shouldThrowResourceNotFoundException() {
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> maintenanceService.cancelMaintenance(UUID.randomUUID(), administrator));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when user is not authorized")
            void testCancelMaintenance_userNotAuthorized_shouldThrowAccessDeniedException() {
                maintenance.setStatus(MaintenanceStatus.PENDING);
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.cancelMaintenance(UUID.randomUUID(), otherUser));
            }

            @ParameterizedTest
            @ValueSource(strings = {"DRAFT", "COMPLETED", "REJECTED_EXPIRED", "CANCELED"})
            @DisplayName("should throw BadRequestException when status is not PENDING or DELAYED")
            void testCancelMaintenance_fromInvalidStatus_shouldThrowBadRequestException(String invalidStatus) {
                maintenance.setStatus(MaintenanceStatus.valueOf(invalidStatus));
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(BadRequestException.class, () -> maintenanceService.cancelMaintenance(UUID.randomUUID(), administrator));
            }
        }
    }

    @Nested
    @DisplayName("updateMaintenance")
    class UpdateMaintenance {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should update maintenance successfully")
            void testUpdateMaintenance_validInput_shouldUpdateMaintenance() {
                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "updated.maintainer@example.com"
                );

                User otherMaintainer = otherUser;
                otherMaintainer.setRole(Role.MAINTAINER);

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));
                when(userService.findByEmail(any(String.class))).thenReturn(Optional.of(otherMaintainer));
                when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(maintenance);

                Maintenance result = maintenanceService.updateMaintenance(UUID.randomUUID(), maintenanceUpdate, administrator);
                assertEquals(result.getDescription(), maintenanceUpdate.description());
                assertEquals(result.getMaintainer().getId(), otherUser.getId());
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should throw ResourceNotFoundException when maintenance does not exist")
            void testUpdateMaintenance_maintenanceDoesNotExist_shouldThrowResourceNotFoundException() {
                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "other.maintainer@example.com"
                );

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> maintenanceService.updateMaintenance(UUID.randomUUID(), maintenanceUpdate, administrator));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when user is not authorized to update")
            void testUpdateMaintenance_userNotAuthorized_shouldThrowAccessDeniedException() {
                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "other.maintainer@example.com"
                );

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.updateMaintenance(UUID.randomUUID(), maintenanceUpdate, otherUser));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when maintainer tries to update")
            void testUpdateMaintenance_maintainerTriesToUpdate_shouldThrowAccessDeniedException() {
                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "other.maintainer@example.com"
                );

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.updateMaintenance(UUID.randomUUID(), maintenanceUpdate, maintainer));
            }

            @Test
            @DisplayName("should throw BadRequestException when new maintainer has ADMINISTRATOR role")
            void testUpdateMaintenance_newMaintainerHasAdministratorRole_shouldThrowBadRequestException() {
                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "other.admin@example.com"
                );
                User otherAdmin = otherUser;
                otherAdmin.setRole(Role.ADMINISTRATOR);

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));
                when(userService.findByEmail(any(String.class))).thenReturn(Optional.of(otherAdmin));

                assertThrows(BadRequestException.class, () -> maintenanceService.updateMaintenance(UUID.randomUUID(), maintenanceUpdate, administrator));

            }

            @Test
            @DisplayName("should throwBadRequestException when trying to update a non-DRAFT maintenance")
            void testUpdateMaintenance_nonDraftMaintenance_shouldThrowBadRequestException() {
                maintenance.setStatus(MaintenanceStatus.PENDING);
                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "other.maintainer@example.com"
                );

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(BadRequestException.class, () -> maintenanceService.updateMaintenance(UUID.randomUUID(), maintenanceUpdate, administrator));
            }
        }
    }

    @Nested
    @DisplayName("addMaintenanceDetails")
    class AddMaintenanceDetails {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should add maintenance details successfully")
            void testAddMaintenanceDetails_validInput_shouldAddDetails() {

                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                    1, LocalDate.now(), 1,1, product.getBarcode()
                );

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));
                when(maintenanceDetailService.createMaintenanceDetail(any(Maintenance.class), any(MaintenanceDetailCreate.class), any(User.class))).thenReturn(maintenanceDetails.get(0));
                when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(maintenance);

                Maintenance result = maintenanceService.addMaintenanceDetail(UUID.randomUUID(), maintenanceDetailCreate, administrator);
                assertEquals(result.getMaintenanceDetails().size(), 2);
                assertEquals(result.getMaintenanceDetails().get(0).getId(), maintenanceDetails.get(0).getId());
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should throw ResourceNotFoundException when maintenance does not exist")
            void testAddMaintenanceDetails_maintenanceDoesNotExist_shouldThrowResourceNotFoundException() {

                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                    1, LocalDate.now(), 1,1, product.getBarcode()
                );

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> maintenanceService.addMaintenanceDetail(UUID.randomUUID(), maintenanceDetailCreate, administrator));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when user is not authorized to add details")
            void testAddMaintenanceDetails_userNotAuthorized_shouldThrowAccessDeniedException() {
                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                    1, LocalDate.now(), 1,1, product.getBarcode()
                );

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.addMaintenanceDetail(UUID.randomUUID(), maintenanceDetailCreate, otherUser));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when maintainer tries to add details")
            void testAddMaintenanceDetails_maintainerTriesToAddDetails_shouldThrowAccessDeniedException() {
                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                    1, LocalDate.now(), 1,1, product.getBarcode()
                );

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.addMaintenanceDetail(UUID.randomUUID(), maintenanceDetailCreate, maintainer));
            }

            @Test
            @DisplayName("should throw BadRequestException when trying to add details to a non-DRAFT maintenance")
            void testAddMaintenanceDetails_nonDraftMaintenance_shouldThrowBadRequestException() {
                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                    1, LocalDate.now(), 1,1, product.getBarcode()
                );

                maintenance.setStatus(MaintenanceStatus.PENDING);
                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(BadRequestException.class, () -> maintenanceService.addMaintenanceDetail(UUID.randomUUID(), maintenanceDetailCreate, administrator));
            }
        }
    }

    @Nested
    @DisplayName("deleteMaintenanceDetails")
    class DeleteMaintenanceDetails {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should delete maintenance details successfully")
            void testDeleteMaintenanceDetails_validInput_shouldDeleteDetails() {
                UUID detailId = maintenanceDetails.get(0).getId();

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));
                when(maintenanceDetailService.findById(any(UUID.class))).thenReturn(maintenanceDetails.get(0));
                when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(maintenance);

                Maintenance result = maintenanceService.deleteMaintenanceDetail(UUID.randomUUID(), detailId, administrator);
                assertEquals(result.getMaintenanceDetails().size(),0);
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should throw ResourceNotFoundException when maintenance does not exist")
            void testDeleteMaintenanceDetails_maintenanceDoesNotExist_shouldThrowResourceNotFoundException() {
                UUID detailId = maintenanceDetails.get(0).getId();

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> maintenanceService.deleteMaintenanceDetail(UUID.randomUUID(), detailId, administrator));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when user is not authorized to delete details")
            void testDeleteMaintenanceDetails_userNotAuthorized_shouldThrowAccessDeniedException() {
                UUID detailId = maintenanceDetails.get(0).getId();

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.deleteMaintenanceDetail(UUID.randomUUID(), detailId, otherUser));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when maintainer tries to delete details")
            void testDeleteMaintenanceDetails_maintainerTriesToDeleteDetails_shouldThrowAccessDeniedException() {
                UUID detailId = maintenanceDetails.get(0).getId();

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(AccessDeniedException.class, () -> maintenanceService.deleteMaintenanceDetail(UUID.randomUUID(), detailId, maintainer));
            }

            @Test
            @DisplayName("should throw BadRequestException when trying to delete a detail that does not belong to the maintenance")
            void testDeleteMaintenanceDetails_detailDoesNotBelongToMaintenance_shouldThrowBadRequestException() {
                UUID detailId = UUID.fromString("00000000-0000-0000-0000-000000000002");

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));
                when(maintenanceDetailService.findById(any(UUID.class))).thenReturn(new MaintenanceDetail());

                assertThrows(BadRequestException.class, () -> maintenanceService.deleteMaintenanceDetail(UUID.randomUUID(), detailId, administrator));
            }

            @Test
            @DisplayName("should throw BadRequestException when trying to delete details from a non-DRAFT maintenance")
            void testDeleteMaintenanceDetails_nonDraftMaintenance_shouldThrowBadRequestException() {
                UUID detailId = maintenanceDetails.get(0).getId();
                maintenance.setStatus(MaintenanceStatus.PENDING);

                when(maintenanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(maintenance));

                assertThrows(BadRequestException.class, () -> maintenanceService.deleteMaintenanceDetail(UUID.randomUUID(), detailId, administrator));
            }
        }
    }
}
