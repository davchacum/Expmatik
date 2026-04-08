package com.expmatik.backend.vendingSlot;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.notification.NotificationService; // Asumiendo que existe
import com.expmatik.backend.notification.NotificationType;

@Component
public class ExpirationTask {

    private final ExpirationBatchRepository expirationBatchRepository;
    private final NotificationService notificationService;
    private final VendingSlotRepository vendingSlotRepository;

    public ExpirationTask(ExpirationBatchRepository expirationBatchRepository, 
                          NotificationService notificationService,
                          VendingSlotRepository vendingSlotRepository) {
        this.expirationBatchRepository = expirationBatchRepository;
        this.notificationService = notificationService;
        this.vendingSlotRepository = vendingSlotRepository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void checkAllExpirations() {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysFromNow = today.plusDays(3);
        LocalDate yesterday = today.minusDays(1);

        List<ExpirationBatch> expiringSoon = expirationBatchRepository.findAllByExpirationDate(threeDaysFromNow);
        for (ExpirationBatch batch : expiringSoon) {
            createExpirationWarningNotification(batch);
        }

        List<ExpirationBatch> expired = expirationBatchRepository.findAllByExpirationDate(yesterday);
        for (ExpirationBatch batch : expired) {
            createProductExpiredNotification(batch);
            batch.getVendingSlot().setIsBlocked(true);
            vendingSlotRepository.save(batch.getVendingSlot());
        }
    }

    private void createExpirationWarningNotification(ExpirationBatch batch) {
            String slotLabel = SlotLabelFormatter.toFrontendLabel(batch.getVendingSlot().getRowNumber(), batch.getVendingSlot().getColumnNumber());
            String message = "AVISO: El producto " + batch.getVendingSlot().getProduct().getName() + " en la ranura " + slotLabel + " de la máquina expendedora " + batch.getVendingSlot().getVendingMachine().getName() + " expirará en 3 días. Por favor, revise el stock y recargue si es necesario.";
            String link = "/vending-machines/" + batch.getVendingSlot().getVendingMachine().getId() + "/details";
            notificationService.createNotification(NotificationType.EXPIRATION_WARNING, message, link, batch.getVendingSlot().getVendingMachine().getUser());
    }

    private void createProductExpiredNotification(ExpirationBatch batch) {
            String slotLabel = SlotLabelFormatter.toFrontendLabel(batch.getVendingSlot().getRowNumber(), batch.getVendingSlot().getColumnNumber());
            String message = "CRÍTICO: El producto " + batch.getVendingSlot().getProduct().getName() + " en la ranura " + slotLabel + " de la máquina expendedora " + batch.getVendingSlot().getVendingMachine().getName() + " ha caducado. La ranura ha sido bloqueada automáticamente. Por favor, retire el producto caducado y recargue la ranura lo antes posible.";
            String link = "/vending-machines/" + batch.getVendingSlot().getVendingMachine().getId() + "/details";
            notificationService.createNotification(NotificationType.PRODUCT_EXPIRED, message, link, batch.getVendingSlot().getVendingMachine().getUser());
    }
}
