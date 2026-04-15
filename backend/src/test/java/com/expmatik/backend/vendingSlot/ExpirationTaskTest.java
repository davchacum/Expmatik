package com.expmatik.backend.vendingSlot;

import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.expmatik.backend.notification.NotificationService;
import com.expmatik.backend.notification.NotificationType;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingMachine.VendingMachine;

@ExtendWith(MockitoExtension.class)
public class ExpirationTaskTest {

	@Mock
	private ExpirationBatchRepository expirationBatchRepository;

	@Mock
	private NotificationService notificationService;

	@Mock
	private VendingSlotRepository vendingSlotRepository;

    @Spy
	@InjectMocks
	private ExpirationTask expirationTask;

	@Nested
	@DisplayName("checkAllExpirations")
	class CheckAllExpirations {

		@Nested
		@DisplayName("Generate Notification Cases")
		class GenerateNotificationCases {

			@Test
			@DisplayName("checkAllExpirations should create warning notification for batches expiring in 3 days")
			void testCheckAllExpirations_ExpiringSoon_ShouldCreateWarningNotification() {
				LocalDate threeDaysFromNow = LocalDate.now().plusDays(3);
				LocalDate yesterday = LocalDate.now().minusDays(1);

				ExpirationBatch batch = createBatch("Coca-Cola", "VM Sevilla", 2, 3, false);

				when(expirationBatchRepository.findAllByExpirationDate(threeDaysFromNow)).thenReturn(List.of(batch));
				when(expirationBatchRepository.findAllByExpirationDate(yesterday)).thenReturn(List.of());

				expirationTask.checkAllExpirations();

				verify(notificationService, times(1)).createNotification(
					eq(NotificationType.EXPIRATION_WARNING),
					any(String.class),
					any(String.class),
					eq(batch.getVendingSlot().getVendingMachine().getUser())
				);

				verify(vendingSlotRepository, never()).save(any(VendingSlot.class));
			}

			@Test
			@DisplayName("checkAllExpirations should create expired notification and block slot for expired batches")
			void testCheckAllExpirations_Expired_ShouldNotifyAndBlockSlot() {
				LocalDate threeDaysFromNow = LocalDate.now().plusDays(3);
				LocalDate yesterday = LocalDate.now().minusDays(1);

				ExpirationBatch expiredBatch = createBatch("Sandwich", "VM ETSII", 1, 1, false);

				when(expirationBatchRepository.findAllByExpirationDate(threeDaysFromNow)).thenReturn(List.of());
				when(expirationBatchRepository.findAllByExpirationDate(yesterday)).thenReturn(List.of(expiredBatch));

				expirationTask.checkAllExpirations();

				verify(notificationService, times(1)).createNotification(
					eq(NotificationType.PRODUCT_EXPIRED),
					any(String.class),
					any(String.class),
					eq(expiredBatch.getVendingSlot().getVendingMachine().getUser())
				);

				assertTrue(expiredBatch.getVendingSlot().getIsBlocked());
				verify(vendingSlotRepository, times(1)).save(eq(expiredBatch.getVendingSlot()));
			}
		}

		@Nested
		@DisplayName("No Notification Cases")
		class NoNotificationCases {

			@Test
			@DisplayName("checkAllExpirations should do nothing when there are no expiring or expired batches")
			void testCheckAllExpirations_NoBatches_ShouldDoNothing() {
				LocalDate threeDaysFromNow = LocalDate.now().plusDays(3);
				LocalDate yesterday = LocalDate.now().minusDays(1);

				when(expirationBatchRepository.findAllByExpirationDate(threeDaysFromNow)).thenReturn(List.of());
				when(expirationBatchRepository.findAllByExpirationDate(yesterday)).thenReturn(List.of());

				expirationTask.checkAllExpirations();

				verify(notificationService, never()).createNotification(any(), any(String.class), any(String.class), any(User.class));
				verify(vendingSlotRepository, never()).save(any(VendingSlot.class));
			}	
		}

		private ExpirationBatch createBatch(String productName, String vendingMachineName, int row, int column, boolean blocked) {
			User owner = new User();

			Product product = new Product();
			product.setName(productName);

			VendingMachine vendingMachine = new VendingMachine();
			vendingMachine.setName(vendingMachineName);
			vendingMachine.setUser(owner);

			VendingSlot slot = new VendingSlot();
			slot.setProduct(product);
			slot.setVendingMachine(vendingMachine);
			slot.setRowNumber(row);
			slot.setColumnNumber(column);
			slot.setIsBlocked(blocked);

			ExpirationBatch batch = new ExpirationBatch();
			batch.setVendingSlot(slot);

			return batch;
		}
	}
}
