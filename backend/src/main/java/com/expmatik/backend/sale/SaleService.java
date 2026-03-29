package com.expmatik.backend.sale;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ExpiredProductException;
import com.expmatik.backend.exceptions.OutOfStockException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.exceptions.SlotBlockedException;
import com.expmatik.backend.exceptions.UnauthorizedActionException;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.productInfo.ProductInfo;
import com.expmatik.backend.productInfo.ProductInfoService;
import com.expmatik.backend.sale.DTOs.SaleCreate;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingSlot.ExpirationBatch;
import com.expmatik.backend.vendingSlot.ExpirationBatchService;
import com.expmatik.backend.vendingSlot.VendingSlot;
import com.expmatik.backend.vendingSlot.VendingSlotService;


@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final VendingSlotService vendingSlotService;
    private final ProductInfoService productInfoService;
    private final ExpirationBatchService expirationBatchService;

    public SaleService(SaleRepository saleRepository, ProductService productService, VendingSlotService vendingSlotService, ProductInfoService productInfoService, ExpirationBatchService expirationBatchService) {
        this.saleRepository = saleRepository;
        this.productService = productService;
        this.vendingSlotService = vendingSlotService;
        this.productInfoService = productInfoService;
        this.expirationBatchService = expirationBatchService;
    }

    @Transactional
    public Sale save(Sale sale) {
        return saleRepository.save(sale);
    }

    private void checkUserAuthorization(Sale sale,User user) {
        if(sale.getVendingSlot().getVendingMachine().getUser().getId() != user.getId()) {
            throw new UnauthorizedActionException("You are not authorized to perform this action.");
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
        VendingSlot vendingSlot = vendingSlotService.getVendingSlotById(saleCreate.vendingSlotId(), user);
        vendingSlotService.checkUserAuthorization(vendingSlot, user);
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
            VendingSlot vendingSlot = getAndValidateVendingSlot(vendingSlotId, user);
            sale.setVendingSlot(vendingSlot);

            validateStockAndProduct(vendingSlot);  

            Product product = vendingSlot.getProduct();
            sale.setProduct(product);

            ProductInfo productInfo = getAndValidateProductInfo(product, user);

            sale.setTotalAmount(productInfo.getSaleUnitPrice());
            validateVendingSlotSuccess(vendingSlot);
            getAndValidateExpirationBatch(vendingSlotId, user);
            sale.setStatus(TransactionStatus.SUCCESS);

        } catch (OutOfStockException | SlotBlockedException | ExpiredProductException e) {

            sale.setStatus(TransactionStatus.FAILED);
            sale.setFailureReason(e.getMessage());
            System.out.println("Sale failed: " + e.getMessage());
        }
        if(sale.getStatus() == TransactionStatus.SUCCESS) {
            expirationBatchService.popUnitExpirationBatch(sale.getVendingSlot(), user);
            vendingSlotService.saveVendingSlot(sale.getVendingSlot());
        }

        return save(sale);
    }

    private VendingSlot getAndValidateVendingSlot(UUID vendingSlotId, User user) {
        VendingSlot vendingSlot = vendingSlotService.getVendingSlotById(vendingSlotId, user);
        vendingSlotService.checkUserAuthorization(vendingSlot, user);
        return vendingSlot;
    }

    private void validateStockAndProduct(VendingSlot vendingSlot) {

        if (vendingSlot.getProduct() == null) {
            throw new ConflictException("Cannot register sale because the vending slot does not have a product assigned.");
        }
    }

    private ExpirationBatch getAndValidateExpirationBatch(UUID vendingSlotId, User user) {
        ExpirationBatch batch = expirationBatchService
            .getExpirationBatchesByVendingSlotId(vendingSlotId, user)
            .get(0);

        if (batch.getExpirationDate() != null && batch.getExpirationDate().isBefore(LocalDate.now())) {
            throw new ExpiredProductException("Cannot register sale because the product is expired.");
        }

        return batch;
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

    private void validateVendingSlotSuccess(VendingSlot vendingSlot) {
        vendingSlotService.checkVendingSlotNotBlocked(vendingSlot);
        if (vendingSlot.getCurrentStock() <= 0) {
            throw new OutOfStockException("Cannot register sale because the vending slot is out of stock.");
        }
    }

    @Transactional
    public void delete(UUID id) {
        saleRepository.deleteById(id);
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

}
