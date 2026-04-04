package com.expmatik.backend.sale;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ExpiredProductException;
import com.expmatik.backend.exceptions.OutOfStockException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.exceptions.SlotBlockedException;
import com.expmatik.backend.notification.NotificationService;
import com.expmatik.backend.notification.NotificationType;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.productInfo.ProductInfo;
import com.expmatik.backend.productInfo.ProductInfoService;
import com.expmatik.backend.sale.DTOs.SaleCreate;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingSlot.VendingSlot;
import com.expmatik.backend.vendingSlot.VendingSlotService;


@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final VendingSlotService vendingSlotService;
    private final ProductInfoService productInfoService;
    private final SaleCSVLector saleCSVLector;
    private final NotificationService notificationService;

    public SaleService(SaleRepository saleRepository, ProductService productService, VendingSlotService vendingSlotService, ProductInfoService productInfoService, SaleCSVLector saleCSVLector, NotificationService notificationService) {
        this.saleRepository = saleRepository;
        this.productService = productService;
        this.vendingSlotService = vendingSlotService;
        this.productInfoService = productInfoService;
        this.saleCSVLector = saleCSVLector;
        this.notificationService = notificationService;
    }

    @Transactional
    public Sale save(Sale sale) {
        return saleRepository.save(sale);
    }

    private void checkUserAuthorization(Sale sale,User user) {
        if(sale.getVendingSlot().getVendingMachine().getUser().getId() != user.getId()) {
            throw new AccessDeniedException("You are not authorized to perform this action.");
        }
    }

    @Transactional(readOnly = true)
    public Sale getSaleById(UUID id, User user) {
        Sale sale = saleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));
        checkUserAuthorization(sale, user);
        return sale;
    }

    @Transactional
    public Sale createSale(SaleCreate saleCreate, User user) {
        //Logica muy basica, en donde no tiene en cuenta la asignacion de stock ni nada, 
        //simplemente crea la venta con los datos que le llegan
        Sale sale = new Sale();
        sale.setSaleDate(saleCreate.saleDate());
        sale.setTotalAmount(saleCreate.totalAmount());
        sale.setPaymentMethod(saleCreate.paymentMethod());
        sale.setStatus(saleCreate.status());
        Product product = productService.findInternalProductByBarcode(saleCreate.barcode(), user.getId());
        VendingSlot vendingSlot = vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(saleCreate.machineName(), saleCreate.rowNumber(), saleCreate.columnNumber(), user);
        sale.setProduct(product);
        sale.setVendingSlot(vendingSlot);
        return save(sale);
    }

    @Transactional(noRollbackFor = {
        OutOfStockException.class,
        SlotBlockedException.class,
        ExpiredProductException.class
    })
    public Sale realTimeSale(UUID vendingSlotId, PaymentMethod paymentMethod, User user) {

        Sale sale = new Sale();
        sale.setPaymentMethod(paymentMethod);
        sale.setSaleDate(LocalDateTime.now());

        try {
            VendingSlot vendingSlot = vendingSlotService.getVendingSlotById(vendingSlotId, user);
            sale.setVendingSlot(vendingSlot);

            Product product = vendingSlot.getProduct();
            if (product == null) {
                throw new ConflictException("Cannot register sale because there is no product assigned to the vending slot.");
            }
            sale.setProduct(product);

            ProductInfo productInfo = getAndValidateProductInfo(product, user);

            sale.setTotalAmount(productInfo.getSaleUnitPrice());
            
            vendingSlotService.popStockFromVendingSlot(vendingSlotId, user);
            sale.setStatus(TransactionStatus.SUCCESS);

        } catch (OutOfStockException | SlotBlockedException | ExpiredProductException e) {

            sale.setStatus(TransactionStatus.FAILED);
            sale.setFailureReason(e.getMessage());
            String message = "La venta ha fallado porque " + e.getMessage() + " en la máquina expendedora " + vendingSlotId.toString() + ". Por favor, revise el estado de la máquina y el producto para solucionar el problema.";
            String link = "Unknown";
            notificationService.createNotification(NotificationType.SALE_FAILURE, message, link, user);
            
            System.out.println("Sale failed: " + e.getMessage());
        }

        return save(sale);
    }

    private ProductInfo getAndValidateProductInfo(Product product, User user) {
        ProductInfo productInfo = productInfoService
                .getOrCreateProductInfo(product.getId(), user, null);

        if (productInfo.getNeedUpdate()) {
            throw new ConflictException(
                "Cannot register sale because the product info needs to be updated."
            );
        }

        return productInfo;
    }

    @Transactional(readOnly = true)
    public Page<Sale> searchSales(UUID userId, String barcode, UUID machineId, UUID slotId, LocalDateTime startDate, LocalDateTime endDate, PaymentMethod paymentMethod, TransactionStatus status, Pageable pageable) {
        String barcodeParam = (barcode != null && !barcode.isBlank()) ? barcode : null;
        UUID machineIdParam = (machineId != null) ? machineId : null;
        UUID slotIdParam = (slotId != null) ? slotId : null;
        return saleRepository.searchAdvanced(userId, barcodeParam, machineIdParam, slotIdParam
            , startDate, endDate
            , paymentMethod, status
            , pageable);
    }

    @Transactional
    public List<SaleCreate> createSalesFromCSV(MultipartFile csvContent) {

        if (csvContent == null || csvContent.isEmpty()) {
            throw new BadRequestException("No file uploaded or file is empty.");
        }

        String originalFilename = csvContent.getOriginalFilename();

        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new BadRequestException("The file must have a .csv extension.");
        }

        File tempCsv = null;

        try {
            tempCsv = File.createTempFile("sales-", ".csv");
            csvContent.transferTo(tempCsv);

            List<SaleCreate> sales = saleCSVLector.readCSV(tempCsv);

            return sales;

        } catch (IOException ex) {
            throw new BadRequestException("Could not process file");
        } finally {
            if (tempCsv != null && tempCsv.exists()) {
                tempCsv.delete();
            }
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportSalesCSV(UUID userId, String barcode, UUID machineId, UUID slotId, LocalDateTime startDate, LocalDateTime endDate, PaymentMethod paymentMethod, TransactionStatus status) {
        String barcodeParam = (barcode != null && !barcode.isBlank()) ? barcode : null;
        UUID machineIdParam = (machineId != null) ? machineId : null;
        UUID slotIdParam = (slotId != null) ? slotId : null;
        List<Sale> sales = saleRepository.searchAdvanced(userId, barcodeParam, machineIdParam, slotIdParam
            , startDate, endDate
            , paymentMethod, status);
        byte[] csvData = saleCSVLector.generateCSV(sales);
        return csvData;
    }

}
