package com.expmatik.backend.vendingSlot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.expmatik.backend.exceptions.ExpiredProductException;
import com.expmatik.backend.exceptions.OutOfStockException;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingMachine.VendingMachine;

@ExtendWith(MockitoExtension.class)
public class ExpirationBatchServiceTest {

    @Mock
    private ExpirationBatchRepository expirationBatchRepository;

    @Spy
    @InjectMocks
    private ExpirationBatchService expirationBatchService;
    
    private VendingSlot vendingSlot;
    private User user;
    private VendingMachine vendingMachine;
    private ExpirationBatch batch1;
    private ExpirationBatch batch2;

    @BeforeEach
    //Setup
    public void setup() {
        user = new User();
        user.setId(UUID.randomUUID());
        vendingMachine = new VendingMachine();
        vendingMachine.setUser(user);
        vendingSlot = new VendingSlot();
        vendingSlot.setId(UUID.randomUUID());
        vendingSlot.setVendingMachine(vendingMachine);
        vendingSlot.setCurrentStock(5);
        vendingSlot.setIsBlocked(false);
        vendingSlot.setMaxCapacity(10);
        batch1 = new ExpirationBatch();
        batch1.setExpirationDate(LocalDate.now().plusDays(30));
        batch1.setQuantity(5);
        batch1.setVendingSlot(vendingSlot);
        batch2 = new ExpirationBatch();
        batch2.setExpirationDate(LocalDate.now().plusDays(60));
        batch2.setQuantity(3);
        batch2.setVendingSlot(vendingSlot);
        
    }

    // == Test getExpirationBatchesByVendingSlotId ==

    @Test
    @DisplayName("getExpirationBatchesByVendingSlotId should return expiration batches for a valid vending slot ID and authorized user")
    public void testGetExpirationBatchesByVendingSlotId_ValidIdAndAuthorizedUser_shouldReturnExpirationBatches() {
        UUID vendingSlotId = vendingSlot.getId();
        List<ExpirationBatch> expectedBatches = List.of(batch1, batch2);
        
        when(expirationBatchRepository.findAllByVendingSlotIdOrderByExpirationDateAsc(vendingSlotId)).thenReturn(expectedBatches);

        List<ExpirationBatch> actualBatches = expirationBatchService.getExpirationBatchesByVendingSlotId(vendingSlotId, user);

        assertEquals(expectedBatches, actualBatches);
    }

    @Test
    @DisplayName("getExpirationBatchesByVendingSlotId should return empty list for a valid vending slot ID with no expiration batches")
    public void testGetExpirationBatchesByVendingSlotId_ValidIdWithNoBatches_shouldReturnEmptyList() {
        UUID vendingSlotId = vendingSlot.getId();

        when(expirationBatchRepository.findAllByVendingSlotIdOrderByExpirationDateAsc(vendingSlotId)).thenReturn(List.of());

        List<ExpirationBatch> actualBatches = expirationBatchService.getExpirationBatchesByVendingSlotId(vendingSlotId, user);

        assertEquals(List.of(), actualBatches);
    }

    @Test
    @DisplayName("getExpirationBatchesByVendingSlotId should throw AccessDeniedException for unauthorized user")
    public void testGetExpirationBatchesByVendingSlotId_UnauthorizedUser_shouldThrowAccessDeniedException() {
        UUID vendingSlotId = vendingSlot.getId();
        User unauthorizedUser = new User();
        unauthorizedUser.setId(UUID.randomUUID());
        List<ExpirationBatch> expectedBatches = List.of(batch1, batch2);
        when(expirationBatchRepository.findAllByVendingSlotIdOrderByExpirationDateAsc(vendingSlotId)).thenReturn(expectedBatches);

        assertThrows(AccessDeniedException.class, () -> {
            expirationBatchService.getExpirationBatchesByVendingSlotId(vendingSlotId, unauthorizedUser);
        });
    }

    // == Test pushExpirationBatch ==

    @Test
    @DisplayName("pushExpirationBatch should add new expiration batch when there is no existing batch with the same expiration date")
    public void testPushExpirationBatch_NoExistingBatch_shouldAddNewBatch() {
        UUID vendingSlotId = vendingSlot.getId();
        LocalDate expirationDate = LocalDate.now().plusDays(30);
        Integer quantity = 5;
        Integer initialStock = vendingSlot.getCurrentStock();

        when(expirationBatchRepository.findFirstByVendingSlotIdAndExpirationDate(vendingSlotId, expirationDate))
            .thenReturn(Optional.empty());

        expirationBatchService.pushExpirationBatch(vendingSlot, expirationDate, quantity, user);

        assertEquals(initialStock + quantity, vendingSlot.getCurrentStock());
    }

    @Test
    @DisplayName("pushExpirationBatch should update existing expiration batch when there is a batch with the same expiration date")
    public void testPushExpirationBatch_ExistingBatch_shouldUpdateBatch() {
        UUID vendingSlotId = vendingSlot.getId();
        LocalDate expirationDate = batch1.getExpirationDate();
        Integer quantity = 2;
        Integer initialStock = vendingSlot.getCurrentStock();
        batch1.setQuantity(5);


        when(expirationBatchRepository.findFirstByVendingSlotIdAndExpirationDate(vendingSlotId, expirationDate))
            .thenReturn(Optional.of(batch1));

        expirationBatchService.pushExpirationBatch(vendingSlot, expirationDate, quantity, user);

        assertEquals(initialStock + quantity, batch1.getQuantity());
        assertEquals(initialStock + quantity, vendingSlot.getCurrentStock());
    }

    // == Test popUnitExpirationBatch ==

    @Test
    @DisplayName("popUnitExpirationBatch should remove one unit from the earliest expiration batch")
    public void testPopUnitExpirationBatch_ShouldRemoveOneUnitFromEarliestBatch() {
        UUID vendingSlotId = vendingSlot.getId();
        Integer initialBatchStock = batch1.getQuantity();
        Integer initialSlotStock = vendingSlot.getCurrentStock();

        when(expirationBatchRepository.findAllByVendingSlotIdOrderByExpirationDateAsc(vendingSlotId))
            .thenReturn(List.of(batch1, batch2));

        expirationBatchService.popUnitExpirationBatch(vendingSlot, user);

        assertEquals(initialBatchStock - 1, batch1.getQuantity());
        assertEquals(initialSlotStock - 1, vendingSlot.getCurrentStock());
    }

        @Test
    @DisplayName("popUnitExpirationBatch should remove batch from repository when quantity reaches zero")
    public void testPopUnitExpirationBatch_QuantityReachesZero_shouldRemoveBatchFromRepository() {
        UUID vendingSlotId = vendingSlot.getId();
        batch1.setQuantity(1);


        when(expirationBatchRepository.findAllByVendingSlotIdOrderByExpirationDateAsc(vendingSlotId))
            .thenReturn(List.of(batch1));

        expirationBatchService.popUnitExpirationBatch(vendingSlot, user);

        assertEquals(0, batch1.getQuantity());
    }

    @Test
    @DisplayName("popUnitExpirationBatch should throw OutOfStockException when there is no stock with the specified expiration date")
    public void testPopUnitExpirationBatch_NoStockWithSpecifiedExpirationDate_shouldThrowOutOfStockException() {
        UUID vendingSlotId = vendingSlot.getId();
        batch1.setQuantity(0);

        when(expirationBatchRepository.findAllByVendingSlotIdOrderByExpirationDateAsc(vendingSlotId))
            .thenReturn(List.of(batch1, batch2));

        assertThrows(OutOfStockException.class, () -> {
            expirationBatchService.popUnitExpirationBatch(vendingSlot, user);
        });
    }

    @Test
    @DisplayName("popUnitExpirationBatch should throw ExpiredProductException when the earliest batch is expired")
    public void testPopUnitExpirationBatch_EarliestBatchExpired_shouldThrowExpiredProductException() {
        UUID vendingSlotId = vendingSlot.getId();
        batch1.setExpirationDate(LocalDate.now().minusDays(1));

        when(expirationBatchRepository.findAllByVendingSlotIdOrderByExpirationDateAsc(vendingSlotId))
            .thenReturn(List.of(batch1, batch2));

        assertThrows(ExpiredProductException.class, () -> {
            expirationBatchService.popUnitExpirationBatch(vendingSlot, user);
        });
    }

}
