package com.expmatik.backend.batch;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.expmatik.backend.batch.DTOs.BatchCreate;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.invoice.Invoice;
import com.expmatik.backend.invoice.InvoiceRepository;
import com.expmatik.backend.invoice.InvoiceStatus;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.productInfo.ProductInfo;
import com.expmatik.backend.productInfo.ProductInfoService;
import com.expmatik.backend.user.User;

import jakarta.transaction.Transactional;

@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final ProductService productService;
    private final InvoiceRepository invoiceRepository;
    private final ProductInfoService productInfoService;

    public BatchService(BatchRepository batchRepository, ProductService productService, InvoiceRepository invoiceRepository, ProductInfoService productInfoService) {
        this.batchRepository = batchRepository;
        this.productService = productService;
        this.invoiceRepository = invoiceRepository;
        this.productInfoService = productInfoService;
    }

    @Transactional
    public Batch save(Batch batch) {
        return batchRepository.save(batch);
    }

    @Transactional
    public Batch createBatch(UUID userId, BatchCreate batch, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));
        if (!invoice.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to edit this invoice.");
        }
        if(invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Cannot add batch to an invoice that is not pending.");
        }
       Product product = productService.getOrCreateProductByBarcode(batch.productBarcode(), userId);
        if(product.getIsPerishable() == true && batch.expirationDate() == null) {
            throw new ConflictException("Expiration date for product " + product.getBarcode() + " is required for perishable products.");
        } else if(product.getIsPerishable() == false && batch.expirationDate() != null) {
            throw new ConflictException("Expiration date for product " + product.getBarcode() + " should not be provided for non-perishable products.");
        }
        Batch newBatch = new Batch();
        newBatch.setExpirationDate(batch.expirationDate());
        newBatch.setUnitPrice(batch.unitPrice());
        newBatch.setQuantity(batch.quantity());
        newBatch.setProduct(product);
        newBatch.setInvoice(invoice);
        return save(newBatch);
    }

    @Transactional
    public Batch updateBatch(UUID userId, UUID batchId, BatchCreate batch) {
        
        Batch existingBatch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));
        if (!existingBatch.getInvoice().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to edit this invoice.");
        }
        if(existingBatch.getInvoice().getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Cannot edit batch from an invoice that is not pending.");
        }
        
        Product product = productService.getOrCreateProductByBarcode(batch.productBarcode(), userId);
        if(product.getIsPerishable() == true && batch.expirationDate() == null) {
                throw new ConflictException("Expiration date for product " + product.getBarcode() + " is required for perishable products.");
        } else if(product.getIsPerishable() == false && batch.expirationDate() != null) {
                throw new ConflictException("Expiration date for product " + product.getBarcode() + " should not be provided for non-perishable products.");
        }
        existingBatch.setExpirationDate(batch.expirationDate());
        existingBatch.setUnitPrice(batch.unitPrice());
        existingBatch.setQuantity(batch.quantity());
        existingBatch.setProduct(product);
        return batchRepository.save(existingBatch);
    }   

    @Transactional
    public void deleteBatch(UUID userId, UUID batchId) {
        Batch existingBatch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));
        if (!existingBatch.getInvoice().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to edit this invoice.");
        }
        if(existingBatch.getInvoice().getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Cannot delete batch from an invoice that is not pending.");
        }
        if(existingBatch.getInvoice().getBatch().size() == 1) {
            throw new ConflictException("Cannot delete the only batch in an invoice. Consider deleting the invoice instead.");
        }
        existingBatch.getInvoice().getBatch().remove(existingBatch);
        invoiceRepository.save(existingBatch.getInvoice());
    }

    @Transactional
    public void addStockQuantity(Batch batch, Integer quantity, User user) {
        ProductInfo productInfo =productInfoService.getOrCreateProductInfo(batch.getProduct().getId(), user,batch.getUnitPrice());
        productInfoService.editStockQuantity(productInfo.getId(), user, quantity, batch.getUnitPrice());
    }

}
