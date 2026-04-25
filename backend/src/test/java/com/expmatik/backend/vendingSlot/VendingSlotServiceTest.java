package com.expmatik.backend.vendingSlot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ExpiredProductException;
import com.expmatik.backend.exceptions.OutOfStockException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.maintenance.MaintenanceService;
import com.expmatik.backend.notification.NotificationService;
import com.expmatik.backend.notification.NotificationType;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingMachine.VendingMachine;

@ExtendWith(MockitoExtension.class)
public class VendingSlotServiceTest {

    @Mock
    private VendingSlotRepository vendingSlotRepository;

    @Mock
    private ProductService productService;

    @Mock
    private ExpirationBatchService expirationBatchService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MaintenanceService maintenanceService;

    @InjectMocks
    @Spy
    private VendingSlotService vendingSlotService;

    private VendingSlot vendingSlot;
    private ExpirationBatch batch1;
    private ExpirationBatch batch2;
    private User user;
    private Product product;
    private VendingMachine vendingMachine;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(UUID.randomUUID());

        vendingMachine = new VendingMachine();
        vendingMachine.setId(UUID.randomUUID());
        vendingMachine.setName("VM Test");
        vendingMachine.setUser(user);

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Producto Test");

        vendingSlot = new VendingSlot();
        vendingSlot.setId(UUID.randomUUID());
        vendingSlot.setVendingMachine(vendingMachine);
        vendingSlot.setProduct(product);
        vendingSlot.setCurrentStock(5);
        vendingSlot.setMaxCapacity(10);
        vendingSlot.setRowNumber(1);
        vendingSlot.setColumnNumber(1);

        batch1 = new ExpirationBatch();
        batch1.setId(UUID.randomUUID());
        batch1.setVendingSlot(vendingSlot);
        batch1.setExpirationDate(LocalDate.now().plusDays(5));
        batch1.setQuantity(3);

        batch2 = new ExpirationBatch();
        batch2.setId(UUID.randomUUID());
        batch2.setVendingSlot(vendingSlot);
        batch2.setExpirationDate(LocalDate.now().plusDays(10));
        batch2.setQuantity(2);
    }

    // == Test getVendingSlotById ==

    @Nested
    @DisplayName("getVendingSlotById")
    class GetVendingSlotById {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("getVendingSlotById - valid ID and authorized user")
            void testGetVendingSlotById_validIdAndAuthorizedUser_shouldReturnVendingSlot() {
                UUID vendingSlotId = vendingSlot.getId();

                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                VendingSlot result = vendingSlotService.getVendingSlotById(vendingSlotId, user);
                assertEquals(vendingSlot, result);
                verify(vendingSlotRepository).findById(vendingSlotId);
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("getVendingSlotById - valid ID but unauthorized user")
            void testGetVendingSlotById_validIdButUnauthorizedUser_shouldThrowUnauthorizedAccessException() {
                UUID vendingSlotId = vendingSlot.getId();
                User unauthorizedUser = new User();
                unauthorizedUser.setId(UUID.randomUUID());

                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                assertThrows(AccessDeniedException.class, () -> {
                    vendingSlotService.getVendingSlotById(vendingSlotId, unauthorizedUser);
                });
            }

            @Test
            @DisplayName("getVendingSlotById - invalid ID")
            void testGetVendingSlotById_invalidId_shouldThrowResourceNotFoundException() {
                UUID invalidId = UUID.randomUUID();

                when(vendingSlotRepository.findById(invalidId)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> {
                    vendingSlotService.getVendingSlotById(invalidId, user);
                });
            }
        }
    }

    // == Test getVendingSlotsByUserIdAndMachineId ==

    @Nested
    @DisplayName("getVendingSlotsByUserIdAndMachineId")
    class GetVendingSlotsByUserIdAndMachineId {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("getVendingSlotsByUserIdAndMachineId - valid machine ID and authorized user")
            void testGetVendingSlotsByUserIdAndMachineId_validMachineIdAndAuthorizedUser_shouldReturnVendingSlots() {
                UUID machineId = vendingMachine.getId();

                when(vendingSlotRepository.findAllByVendingMachineId(machineId)).thenReturn(List.of(vendingSlot));
                List<VendingSlot> result = vendingSlotService.getVendingSlotsByUserIdAndMachineId(machineId, user);
                assertEquals(List.of(vendingSlot), result);
                verify(vendingSlotRepository).findAllByVendingMachineId(machineId);
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("getVendingSlotsByUserIdAndMachineId - valid machine ID but unauthorized user")
            void testGetVendingSlotsByUserIdAndMachineId_validMachineIdButUnauthorizedUser_shouldThrowUnauthorizedAccessException() {
                UUID machineId = vendingMachine.getId();
                User unauthorizedUser = new User();
                unauthorizedUser.setId(UUID.randomUUID());

                when(vendingSlotRepository.findAllByVendingMachineId(machineId)).thenReturn(List.of(vendingSlot));

                assertThrows(AccessDeniedException.class, () -> {
                    vendingSlotService.getVendingSlotsByUserIdAndMachineId(machineId, unauthorizedUser);
                });
            }

            @Test
            @DisplayName("getVendingSlotsByUserIdAndMachineId - invalid machine ID")
            void testGetVendingSlotsByUserIdAndMachineId_invalidMachineId_shouldThrowResourceNotFoundException() {
                UUID invalidMachineId = UUID.randomUUID();

                when(vendingSlotRepository.findAllByVendingMachineId(invalidMachineId)).thenThrow(ResourceNotFoundException.class);

                assertThrows(ResourceNotFoundException.class, () -> {
                    vendingSlotService.getVendingSlotsByUserIdAndMachineId(invalidMachineId, user);
                });
            }
        }
    }

    // == Test assignProductToVendingSlot ==

    @Nested
    @DisplayName("assignProductToVendingSlot")
    class AssignProductToVendingSlot {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("assignProductToVendingSlot - valid vending slot ID, valid barcode, and authorized user")
            void testAssignProductToVendingSlot_validVendingSlotIdValidBarcodeAndAuthorizedUser_shouldAssignProductToVendingSlot() {
                UUID vendingSlotId = vendingSlot.getId();
                vendingSlot.setCurrentStock(0);
                vendingSlot.setIsBlocked(false);
                String barcode = "1234567890";
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(productService.findInternalProductByBarcode(barcode, user.getId())).thenReturn(product);
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);

                VendingSlot result = vendingSlotService.assignProductToVendingSlot(vendingSlotId, barcode, user);
                assertEquals(vendingSlot, result);
                verify(productService).findInternalProductByBarcode(barcode, user.getId());
                verify(vendingSlotRepository).save(vendingSlot);
            }

            @Test
            @DisplayName("assignProductToVendingSlot - valid vending slot ID, null barcode, and authorized user")
            void testAssignProductToVendingSlot_validVendingSlotIdNullBarcodeAndAuthorizedUser_shouldUnassignProductFromVendingSlot() {
                UUID vendingSlotId = vendingSlot.getId();
                vendingSlot.setCurrentStock(0);
                vendingSlot.setIsBlocked(false);
                String barcode = null;
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);

                VendingSlot result = vendingSlotService.assignProductToVendingSlot(vendingSlotId, barcode, user);
                assertEquals(vendingSlot, result);
                assertEquals(null, vendingSlot.getProduct());
                verify(vendingSlotRepository).save(vendingSlot);
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("assignProductToVendingSlot - valid vending slot ID, valid barcode but blocked vending slot and have products with stock should throw ConflictException")
            void testAssignProductToVendingSlot_validVendingSlotIdValidBarcodeButBlockedVendingSlotAndHaveProducts_shouldThrowConflictException() {
                UUID vendingSlotId = vendingSlot.getId();
                vendingSlot.setIsBlocked(true);
                String barcode = "1234567890";
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                ConflictException exception = assertThrows(ConflictException.class, () -> {
                    vendingSlotService.assignProductToVendingSlot(vendingSlotId, barcode, user);
                });

                assertEquals("The vending slot is not empty.", exception.getMessage());
            }

            @Test
            @DisplayName("assignProductToVendingSlot - valid vending slot ID, valid barcode but vending slot with stock")
            void testAssignProductToVendingSlot_validVendingSlotIdValidBarcodeButVendingSlotWithStock_shouldThrowConflictException() {
                UUID vendingSlotId = vendingSlot.getId();
                vendingSlot.setCurrentStock(5);
                vendingSlot.setIsBlocked(false);
                String barcode = "1234567890";
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                ConflictException exception = assertThrows(ConflictException.class, () -> {
                    vendingSlotService.assignProductToVendingSlot(vendingSlotId, barcode, user);
                });

                assertEquals("The vending slot is not empty.", exception.getMessage());
            }

            @Test
            @DisplayName("assignProductToVendingSlot - valid vending slot ID, valid barcode but unauthorized user")
            void testAssignProductToVendingSlot_validVendingSlotIdValidBarcodeButUnauthorizedUser_shouldThrowUnauthorizedAccessException() {
                UUID vendingSlotId = vendingSlot.getId();
                User unauthorizedUser = new User();
                unauthorizedUser.setId(UUID.randomUUID());
                vendingSlot.setCurrentStock(0);
                vendingSlot.setIsBlocked(false);
                String barcode = "1234567890";
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                assertThrows(AccessDeniedException.class, () -> {
                    vendingSlotService.assignProductToVendingSlot(vendingSlotId, barcode, unauthorizedUser);
                });
            }

            @Test
            @DisplayName("assignProductToVendingSlot - valid vending slot ID, but slot is blocked for maintenance")
            void testAssignProductToVendingSlot_validVendingSlotIdButSlotBlockedForMaintenance_shouldThrowConflictException() {
                UUID vendingSlotId = vendingSlot.getId();
                String barcode = "1234567890";
                vendingSlot.setIsBlocked(true);
                vendingSlot.setCurrentStock(0);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                ConflictException exception = assertThrows(ConflictException.class, () -> {
                    vendingSlotService.assignProductToVendingSlot(vendingSlotId, barcode, user);
                });

                assertEquals("Cannot assign or unassign a product to a vending slot that is blocked for maintenance.", exception.getMessage());
            }
        }
    }

    // == Test cases for updateBlockStatus ==

    @Nested
    @DisplayName("updateBlockStatus")
    class UpdateBlockStatus {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("updateBlockStatus - valid vending slot ID, valid block status, and authorized user")
            void testUpdateBlockStatus_validVendingSlotIdValidBlockStatusAndAuthorizedUser_shouldUpdateBlockStatus() {
                UUID vendingSlotId = vendingSlot.getId();
                Boolean newBlockStatus = true;
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);

                VendingSlot result = vendingSlotService.updateBlockStatus(vendingSlotId, newBlockStatus, user);
                assertEquals(vendingSlot, result);
                assertEquals(newBlockStatus, vendingSlot.getIsBlocked());
                verify(vendingSlotRepository).save(vendingSlot);
            }

            @Test
            @DisplayName("updateBlockStatus - same true block status, valid vending slot ID, and authorized user")
            void testUpdateBlockStatus_sameTrueBlockStatusValidVendingSlotIdAndAuthorizedUser_shouldUpdateBlockStatus() {
                UUID vendingSlotId = vendingSlot.getId();
                Boolean newBlockStatus = true;
                vendingSlot.setIsBlocked(true);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);

                VendingSlot result = vendingSlotService.updateBlockStatus(vendingSlotId, newBlockStatus, user);
                assertEquals(vendingSlot, result);
                assertEquals(newBlockStatus, vendingSlot.getIsBlocked());
                verify(vendingSlotRepository).save(vendingSlot);
            }

            @Test
            @DisplayName("updateBlockStatus - same false block status, valid vending slot ID, and authorized user")
            void testUpdateBlockStatus_sameFalseBlockStatusValidVendingSlotIdAndAuthorizedUser_shouldUpdateBlockStatus() {
                UUID vendingSlotId = vendingSlot.getId();
                Boolean newBlockStatus = false;
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);

                VendingSlot result = vendingSlotService.updateBlockStatus(vendingSlotId, newBlockStatus, user);
                assertEquals(vendingSlot, result);
                assertEquals(newBlockStatus, vendingSlot.getIsBlocked());
                verify(vendingSlotRepository).save(vendingSlot);
            }

            @Test
            @DisplayName("updateBlockStatus - expiration batch empty")
            void testUpdateBlockStatus_expirationBatchEmpty_shouldUpdateBlockStatus() {
                UUID vendingSlotId = vendingSlot.getId();
                Boolean newBlockStatus = false;
                vendingSlot.setIsBlocked(true);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(expirationBatchService.getExpirationBatchesByVendingSlotId(vendingSlotId, user)).thenReturn(List.of());
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);

                VendingSlot result = vendingSlotService.updateBlockStatus(vendingSlotId, newBlockStatus, user);
                assertEquals(vendingSlot, result);
                assertEquals(newBlockStatus, vendingSlot.getIsBlocked());
                verify(vendingSlotRepository).save(vendingSlot);
            }

            @Test
            @DisplayName("updateBlockStatus - with non-expired products")
            void testUpdateBlockStatus_withExpiredProducts_shouldUpdateBlockStatus() {
                UUID vendingSlotId = vendingSlot.getId();
                Boolean newBlockStatus = false;
                vendingSlot.setIsBlocked(true);
                ExpirationBatch expiredBatch = new ExpirationBatch();
                expiredBatch.setExpirationDate(LocalDate.now().plusDays(1));
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(expirationBatchService.getExpirationBatchesByVendingSlotId(vendingSlotId, user)).thenReturn(List.of(expiredBatch));
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);

                VendingSlot result = vendingSlotService.updateBlockStatus(vendingSlotId, newBlockStatus, user);
                assertEquals(vendingSlot, result);
                assertEquals(newBlockStatus, vendingSlot.getIsBlocked());
                verify(vendingSlotRepository).save(vendingSlot);
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("updateBlockStatus - valid vending slot ID, but unauthorized user")
            void testUpdateBlockStatus_validVendingSlotIdButUnauthorizedUser_shouldThrowUnauthorizedAccessException() {
                UUID vendingSlotId = vendingSlot.getId();
                User unauthorizedUser = new User();
                unauthorizedUser.setId(UUID.randomUUID());
                Boolean newBlockStatus = true;
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                assertThrows(AccessDeniedException.class, () -> {
                    vendingSlotService.updateBlockStatus(vendingSlotId, newBlockStatus, unauthorizedUser);
                });
            }

            @Test
            @DisplayName("updateBlockStatus - valid vending slot ID, but slot has expired products")
            void testUpdateBlockStatus_validVendingSlotIdButSlotHasExpiredProducts_shouldThrowConflictException() {
                UUID vendingSlotId = vendingSlot.getId();
                Boolean newBlockStatus = false;
                vendingSlot.setIsBlocked(true);
                ExpirationBatch expiredBatch = new ExpirationBatch();
                expiredBatch.setExpirationDate(LocalDate.now().minusDays(1));
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(expirationBatchService.getExpirationBatchesByVendingSlotId(vendingSlotId, user)).thenReturn(List.of(expiredBatch));

                ConflictException conflictException = assertThrows(ConflictException.class, () -> {
                    vendingSlotService.updateBlockStatus(vendingSlotId, newBlockStatus, user);
                });

                assertEquals("Cannot unblock a vending slot with expired products.", conflictException.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("addStockToVendingSlot")
    class AddStockToVendingSlot {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("addStockToVendingSlot - valid vending slot ID, valid quantity, valid expiration date, and authorized user")
            void testAddStockToVendingSlot_validVendingSlotIdValidQuantityValidExpirationDateAndAuthorizedUser_shouldAddStockToVendingSlot() {
                UUID vendingSlotId = vendingSlot.getId();
                Integer quantityToAdd = 3;
                LocalDate expirationDate = LocalDate.now().plusDays(5);
                vendingSlot.setIsBlocked(false);
                product.setIsPerishable(true);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);

                VendingSlot result = vendingSlotService.addStockToVendingSlot(vendingSlotId, quantityToAdd, expirationDate, user);
                assertEquals(vendingSlot, result);
                verify(expirationBatchService).pushExpirationBatch(vendingSlot, expirationDate, quantityToAdd, user);
                verify(vendingSlotRepository).save(vendingSlot);
            }

            @Test
            @DisplayName("addStockToVendingSlot - valid vending slot ID, valid quantity, null expiration date, and authorized user")
            void testAddStockToVendingSlot_validVendingSlotIdValidQuantityNullExpirationDateAndAuthorizedUser_shouldAddStockToVendingSlot() {
                UUID vendingSlotId = vendingSlot.getId();
                Integer quantityToAdd = 3;
                LocalDate expirationDate = null;
                product.setIsPerishable(false);
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);

                VendingSlot result = vendingSlotService.addStockToVendingSlot(vendingSlotId, quantityToAdd, expirationDate, user);
                assertEquals(vendingSlot, result);
                verify(expirationBatchService).pushExpirationBatch(vendingSlot, expirationDate, quantityToAdd, user);
                verify(vendingSlotRepository).save(vendingSlot);
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("addStockToVendingSlot - valid vending slot ID, but no assigned product")
            void testAddStockToVendingSlot_validVendingSlotIdButNoAssignedProduct_shouldThrowConflictException() {
                UUID vendingSlotId = vendingSlot.getId();
                Integer quantityToAdd = 3;
                LocalDate expirationDate = LocalDate.now().plusDays(5);
                vendingSlot.setProduct(null);
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                ConflictException exception = assertThrows(ConflictException.class, () -> {
                    vendingSlotService.addStockToVendingSlot(vendingSlotId, quantityToAdd, expirationDate, user);
                });

                assertEquals("Cannot add stock to a vending slot that does not have an assigned product.", exception.getMessage());
            }

            @Test
            @DisplayName("addStockToVendingSlot - valid vending slot ID, but quantity exceeds maximum capacity")
            void testAddStockToVendingSlot_validVendingSlotIdButQuantityExceedsMaximumCapacity_shouldThrowConflictException() {
                UUID vendingSlotId = vendingSlot.getId();
                Integer quantityToAdd = 6;
                LocalDate expirationDate = LocalDate.now().plusDays(5);
                vendingSlot.setCurrentStock(5);
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                ConflictException exception = assertThrows(ConflictException.class, () -> {
                    vendingSlotService.addStockToVendingSlot(vendingSlotId, quantityToAdd, expirationDate, user);
                });

                assertEquals("Cannot add stock to a vending slot that exceeds its maximum capacity.", exception.getMessage());
            }

            @Test
            @DisplayName("addStockToVendingSlot - valid vending slot ID, but expiration date in the past")
            void testAddStockToVendingSlot_validVendingSlotIdButInvalidExpirationDate_shouldThrowExpiredProductException() {
                UUID vendingSlotId = vendingSlot.getId();
                Integer quantityToAdd = 3;
                LocalDate expirationDate = LocalDate.now().minusDays(1);
                vendingSlot.setIsBlocked(false);
                product.setIsPerishable(true);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                assertThrows(ExpiredProductException.class, () -> {
                    vendingSlotService.addStockToVendingSlot(vendingSlotId, quantityToAdd, expirationDate, user);
                });
            }

            @Test
            @DisplayName("addStockToVendingSlot - valid vending slot ID, but expiration date provided for non-perishable product")
            void testAddStockToVendingSlot_validVendingSlotIdButExpirationDateProvidedForNonPerishableProduct_shouldThrowConflictException() {
                UUID vendingSlotId = vendingSlot.getId();
                Integer quantityToAdd = 3;
                LocalDate expirationDate = LocalDate.now().plusDays(5);
                product.setIsPerishable(false);
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                ConflictException exception = assertThrows(ConflictException.class, () -> {
                    vendingSlotService.addStockToVendingSlot(vendingSlotId, quantityToAdd, expirationDate, user);
                });

                assertEquals("Expiration date should not be provided for non-perishable products.", exception.getMessage());

            }

            @Test
            @DisplayName("addStockToVendingSlot - valid vending slot ID, but null expiration date for perishable product")
            void testAddStockToVendingSlot_validVendingSlotIdButNullExpirationDateForPerishableProduct_shouldThrowConflictException() {
                UUID vendingSlotId = vendingSlot.getId();
                Integer quantityToAdd = 3;
                LocalDate expirationDate = null;
                product.setIsPerishable(true);
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                ConflictException exception = assertThrows(ConflictException.class, () -> {
                    vendingSlotService.addStockToVendingSlot(vendingSlotId, quantityToAdd, expirationDate, user);
                });

                assertEquals("Expiration date is required for perishable products.", exception.getMessage());
            }
        }
    }

    // == Test popStockFromVendingSlot ==

    @Nested
    @DisplayName("popStockFromVendingSlot")
    class PopStockFromVendingSlot {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("popStockFromVendingSlot - valid vending slot ID, and authorized user")
            void testPopStockFromVendingSlot_validVendingSlotIdAndAuthorizedUser_shouldPopStockFromVendingSlot() {
                UUID vendingSlotId = vendingSlot.getId();
                vendingSlot.setCurrentStock(5);
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);

                VendingSlot result = vendingSlotService.popStockFromVendingSlot(vendingSlotId, user);
                assertEquals(vendingSlot, result);
                verify(expirationBatchService).popUnitExpirationBatch(vendingSlot, user);
                verify(vendingSlotRepository).save(vendingSlot);
            }
        }
        
        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {
        
            @Test
            @DisplayName("popStockFromVendingSlot - valid vending slot ID, but unauthorized user")
            void testPopStockFromVendingSlot_validVendingSlotIdButUnauthorizedUser_shouldThrowUnauthorizedAccessException() {
                UUID vendingSlotId = vendingSlot.getId();
                User unauthorizedUser = new User();
                unauthorizedUser.setId(UUID.randomUUID());
                vendingSlot.setCurrentStock(5);
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                assertThrows(AccessDeniedException.class, () -> {
                    vendingSlotService.popStockFromVendingSlot(vendingSlotId, unauthorizedUser);
                });
            }

            @Test
            @DisplayName("popStockFromVendingSlot - valid vending slot ID, but no assigned product")
            void testPopStockFromVendingSlot_validVendingSlotIdButNoAssignedProduct_shouldThrowConflictException() {
                UUID vendingSlotId = vendingSlot.getId();
                vendingSlot.setProduct(null);
                vendingSlot.setCurrentStock(5);
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                ConflictException exception = assertThrows(ConflictException.class, () -> {
                    vendingSlotService.popStockFromVendingSlot(vendingSlotId, user);
                });
                assertEquals("Cannot register sale because the vending slot does not have a product assigned.", exception.getMessage());
            }

            @Test
            @DisplayName("popStockFromVendingSlot - valid vending slot ID, but vending slot is empty")
            void testPopStockFromVendingSlot_validVendingSlotIdButVendingSlotEmpty_shouldThrowOutOfStockException() {
                UUID vendingSlotId = vendingSlot.getId();
                vendingSlot.setCurrentStock(0);
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));

                assertThrows(OutOfStockException.class, () -> {
                    vendingSlotService.popStockFromVendingSlot(vendingSlotId, user);
                });
            }

        }

        @Nested
        @DisplayName("Notification Cases")
        class NotificationCases {
            @Test
            @DisplayName("popStockFromVendingSlot - stock reaches 3, should send low stock notification")
            void testPopStockFromVendingSlot_stockReaches3_shouldSendLowStockNotification() {
                UUID vendingSlotId = vendingSlot.getId();
                vendingSlot.setCurrentStock(4);
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);
                doAnswer(invocation -> {
                    vendingSlot.setCurrentStock(vendingSlot.getCurrentStock() - 1);
                    return null;
                }).when(expirationBatchService).popUnitExpirationBatch(vendingSlot, user);

                vendingSlotService.popStockFromVendingSlot(vendingSlotId, user);
                verify(notificationService).createNotification(
                    eq(NotificationType.PRODUCT_LOW_STOCK),
                    any(String.class),
                    any(String.class),
                    eq(user)
                );
            }

            @Test
            @DisplayName("popStockFromVendingSlot - stock reaches 0, should send out-of-stock notification")
            void testPopStockFromVendingSlot_stockReaches0_shouldSendOutOfStockNotification() {
                UUID vendingSlotId = vendingSlot.getId();
                vendingSlot.setCurrentStock(1);
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);
                doAnswer(invocation -> {
                    vendingSlot.setCurrentStock(vendingSlot.getCurrentStock() - 1);
                    return null;
                }).when(expirationBatchService).popUnitExpirationBatch(vendingSlot, user);

                vendingSlotService.popStockFromVendingSlot(vendingSlotId, user);

                verify(notificationService).createNotification(
                    eq(NotificationType.PRODUCT_OUT_OF_STOCK),
                    any(String.class),
                    any(String.class),
                    any(User.class)
                );
            }

            @Test
            @DisplayName("popStockFromVendingSlot - stock does not reach threshold, should not send stock notification")
            void testPopStockFromVendingSlot_stockWithoutThreshold_shouldNotSendStockNotification() {
                UUID vendingSlotId = vendingSlot.getId();
                vendingSlot.setCurrentStock(5);
                vendingSlot.setIsBlocked(false);
                when(vendingSlotRepository.findById(vendingSlotId)).thenReturn(Optional.of(vendingSlot));
                when(vendingSlotRepository.save(vendingSlot)).thenReturn(vendingSlot);
                doAnswer(invocation -> {
                    vendingSlot.setCurrentStock(vendingSlot.getCurrentStock() - 1);
                    return null;
                }).when(expirationBatchService).popUnitExpirationBatch(vendingSlot, user);

                vendingSlotService.popStockFromVendingSlot(vendingSlotId, user);

                verify(notificationService, never()).createNotification(
                    any(NotificationType.class),
                    any(String.class),
                    any(String.class),
                    any(User.class)
                );
            }
        }
    }

    // == Test createVendingSlotsForMachine ==

    @Nested
    @DisplayName("createVendingSlotsForMachine")
    class CreateVendingSlotsForMachine {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("createVendingSlotsForMachine - valid machine, row count, column count, and max capacity per slot")
            void testCreateVendingSlotsForMachine_validMachineRowCountColumnCountAndMaxCapacityPerSlot_shouldCreateVendingSlotsForMachine() {
                VendingMachine machine = new VendingMachine();
                machine.setId(UUID.randomUUID());
                machine.setUser(user);
                Integer rowCount = 2;
                Integer columnCount = 3;
                Integer maxCapacityPerSlot = 10;

                vendingSlotService.createVendingSlotsForMachine(machine, rowCount, columnCount, maxCapacityPerSlot);

                verify(vendingSlotRepository, times(rowCount * columnCount)).save(any(VendingSlot.class));
            }
        }
    }

    // == Test getVendingSlotByMachineNameAndRowAndColumn ==

    @Nested
    @DisplayName("getVendingSlotByMachineNameAndRowAndColumn")
    class GetVendingSlotByMachineNameAndRowAndColumn {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("getVendingSlotByMachineNameAndRowAndColumn - valid machine name, row, column, and authorized user")
            void testGetVendingSlotByMachineNameAndRowAndColumn_validMachineNameRowColumnAndAuthorizedUser_shouldReturnVendingSlot() {
                String machineName = vendingMachine.getName();
                Integer row = vendingSlot.getRowNumber();
                Integer column = vendingSlot.getColumnNumber();

                when(vendingSlotRepository.findByVendingMachineNameAndRowAndColumn(machineName, row, column)).thenReturn(Optional.of(vendingSlot));
                VendingSlot result = vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(machineName, row, column, user);
                assertEquals(vendingSlot, result);
                verify(vendingSlotRepository).findByVendingMachineNameAndRowAndColumn(machineName, row, column);
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("getVendingSlotByMachineNameAndRowAndColumn - valid machine name, row, column but unauthorized user")
            void testGetVendingSlotByMachineNameAndRowAndColumn_validMachineNameRowColumnButUnauthorizedUser_shouldThrowUnauthorizedAccessException() {
                String machineName = vendingMachine.getName();
                Integer row = vendingSlot.getRowNumber();
                Integer column = vendingSlot.getColumnNumber();
                User unauthorizedUser = new User();
                unauthorizedUser.setId(UUID.randomUUID());

                when(vendingSlotRepository.findByVendingMachineNameAndRowAndColumn(machineName, row, column)).thenReturn(Optional.of(vendingSlot));

                assertThrows(AccessDeniedException.class, () -> {
                    vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(machineName, row, column, unauthorizedUser);
                });
            }

            @Test
            @DisplayName("getVendingSlotByMachineNameAndRowAndColumn - notFound case")
            void testGetVendingSlotByMachineNameAndRowAndColumn_notFoundCase_shouldThrowResourceNotFoundException() {
                String machineName = vendingMachine.getName();
                Integer row = vendingSlot.getRowNumber();
                Integer column = vendingSlot.getColumnNumber();

                when(vendingSlotRepository.findByVendingMachineNameAndRowAndColumn(machineName, row, column)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> {
                    vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(machineName, row, column, user);
                });
            }
        }
    }
}
