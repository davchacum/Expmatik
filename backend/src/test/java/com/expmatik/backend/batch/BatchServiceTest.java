package com.expmatik.backend.batch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedList;
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

import com.expmatik.backend.batch.DTOs.BatchCreate;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.invoice.Invoice;
import com.expmatik.backend.invoice.InvoiceRepository;
import com.expmatik.backend.invoice.InvoiceStatus;
import com.expmatik.backend.invoice.Supplier;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.productInfo.ProductInfo;
import com.expmatik.backend.productInfo.ProductInfoService;
import com.expmatik.backend.user.User;

@ExtendWith(MockitoExtension.class)
public class BatchServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private ProductService productService;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ProductInfoService productInfoService;

    @Spy
    @InjectMocks
    private BatchService batchService;

    private Product productCustom;
    private Product productNoCustom;
    private ProductInfo productInfo;
    private User user1;
    private User user2;
    private Invoice invoice1;
    private Invoice invoice2;
    private Batch batch1;
    private Batch batch2;
    private Batch batch3;
    private Supplier supplier1;
    private Supplier supplier2;

    @BeforeEach
    public void setUp() {
        user1 = new User();
        user1.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        user1.setEmail("user1@example.com");
        user1.setPassword("password1");

        user2 = new User();
        user2.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        user2.setEmail("user2@example.com");
        user2.setPassword("password2");

        productCustom = new Product();
        productCustom.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        productCustom.setName("Custom Product");
        productCustom.setBrand("Brand A");
        productCustom.setDescription("This is a custom product.");
        productCustom.setImageUrl("http://example.com/image.jpg");
        productCustom.setIsPerishable(false);
        productCustom.setBarcode("1234567890123");
        productCustom.setIsCustom(true);
        productCustom.setCreatedBy(user1);

        productNoCustom = new Product();
        productNoCustom.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        productNoCustom.setName("Non-Custom Product");
        productNoCustom.setBrand("Brand B");
        productNoCustom.setDescription("This is a non-custom product.");
        productNoCustom.setImageUrl("");
        productNoCustom.setIsPerishable(true);
        productNoCustom.setBarcode("9876543210123");
        productNoCustom.setIsCustom(false);

        productInfo = new ProductInfo();
        productInfo.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        productInfo.setStockQuantity(10);
        productInfo.setVatRate(new BigDecimal("0.10"));
        productInfo.setSaleUnitPrice(new BigDecimal("5.99"));
        productInfo.setLastPurchaseUnitPrice(new BigDecimal("4.99"));

        supplier1 = new Supplier();
        supplier1.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        supplier1.setName("Supplier A");

        supplier2 = new Supplier();
        supplier2.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        supplier2.setName("Supplier B");

        invoice1 = new Invoice();
        invoice1.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        invoice1.setInvoiceNumber("INV-001");
        invoice1.setStatus(InvoiceStatus.PENDING);
        invoice1.setUser(user1);
        invoice1.setInvoiceDate(LocalDate.of(2026, 1, 1));
        invoice1.setSupplier(supplier1);
        invoice1.setBatch(new LinkedList<>());

        invoice2 = new Invoice();
        invoice2.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        invoice2.setInvoiceNumber("INV-002");
        invoice2.setStatus(InvoiceStatus.RECEIVED);
        invoice2.setUser(user2);
        invoice2.setInvoiceDate(LocalDate.of(2026, 2, 1));
        invoice2.setSupplier(supplier2);
        invoice2.setBatch(new LinkedList<>());

        batch1 = new Batch();
        batch1.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        batch1.setProduct(productCustom);
        batch1.setQuantity(5);
        batch1.setUnitPrice(new BigDecimal("4.99"));
        batch1.setInvoice(invoice1);
        invoice1.getBatch().add(batch1);

        batch2 = new Batch();
        batch2.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        batch2.setProduct(productNoCustom);
        batch2.setQuantity(3);
        batch2.setUnitPrice(new BigDecimal("3.99"));
        batch2.setInvoice(invoice1);
        batch2.setExpirationDate(LocalDate.of(2026, 3, 1));
        invoice1.getBatch().add(batch2);

        batch3 = new Batch();
        batch3.setId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        batch3.setProduct(productCustom);
        batch3.setQuantity(2);
        batch3.setUnitPrice(new BigDecimal("4.99"));
        batch3.setInvoice(invoice2);
        invoice2.getBatch().add(batch3);
    }

    // == createBatch tests ==

    @Test
    @DisplayName("Create batch with valid data should succeed")
    void testCreateBatch_ValidData_ShouldSucceed() {

        BatchCreate batchCreate = new BatchCreate(batch1);

        when(invoiceRepository.findById(invoice1.getId())).thenReturn(Optional.of(invoice1));
        when(productService.getOrCreateProductByBarcode("1234567890123", user1.getId())).thenReturn(productCustom);
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Batch createdBatch = batchService.createBatch(user1.getId(), batchCreate, invoice1.getId());

        assert(batch1.getExpirationDate() == null);
        assert(batch1.getUnitPrice().equals(createdBatch.getUnitPrice()));
        assert(batch1.getQuantity() == createdBatch.getQuantity());
    }

    @Test
    @DisplayName("Create batch with non-existing invoice should throw ResourceNotFoundException")
    void testCreateBatch_NonExistingInvoice_ShouldThrowResourceNotFoundException() {

        BatchCreate batchCreate = new BatchCreate(batch1);

        when(invoiceRepository.findById(invoice1.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.createBatch(user1.getId(), batchCreate, invoice1.getId()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Invoice not found with id: " + invoice1.getId());
    }

    @Test
    @DisplayName("Create batch with perishable product and null expiration date should throw ConflictException")
    void testCreateBatch_PerishableProductNullExpirationDate_ShouldThrowConflictException() {

        BatchCreate batchCreate = new BatchCreate(null, batch2.getUnitPrice(), batch2.getQuantity(), batch2.getProduct().getBarcode());

        when(invoiceRepository.findById(invoice1.getId())).thenReturn(Optional.of(invoice1));
        when(productService.getOrCreateProductByBarcode("9876543210123", user1.getId())).thenReturn(productNoCustom);

        assertThatThrownBy(() -> batchService.createBatch(user1.getId(), batchCreate, invoice1.getId()))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Expiration date for product " + batch2.getProduct().getBarcode() + " is required for perishable products.");
    }

    @Test
    @DisplayName("Create batch with non-perishable product and non-null expiration date should throw ConflictException")
    void testCreateBatch_NonPerishableProductNonNullExpirationDate_ShouldThrowConflictException() {

        BatchCreate batchCreate = new BatchCreate(LocalDate.of(2026, 3, 1), batch1.getUnitPrice(), batch1.getQuantity(), batch1.getProduct().getBarcode());

        when(invoiceRepository.findById(invoice1.getId())).thenReturn(Optional.of(invoice1));
        when(productService.getOrCreateProductByBarcode("1234567890123", user1.getId())).thenReturn(productCustom);

        assertThatThrownBy(() -> batchService.createBatch(user1.getId(), batchCreate, invoice1.getId()))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Expiration date for product " + batch1.getProduct().getBarcode() + " should not be provided for non-perishable products.");
    }

    @Test
    @DisplayName("Create batch with invoice belonging to another user should throw AccessDeniedException")
    void testCreateBatch_InvoiceBelongsToAnotherUser_ShouldThrowAccessDeniedException() {

        BatchCreate batchCreate = new BatchCreate(batch1);

        when(invoiceRepository.findById(invoice2.getId())).thenReturn(Optional.of(invoice2));

        assertThatThrownBy(() -> batchService.createBatch(user1.getId(), batchCreate, invoice2.getId()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("You don't have permission to edit this invoice.");
    }

    @Test
    @DisplayName("Create batch with invoice that is not pending should throw ConflictException")
    void testCreateBatch_InvoiceNotPending_ShouldThrowConflictException() {

        BatchCreate batchCreate = new BatchCreate(batch3);

        when(invoiceRepository.findById(invoice2.getId())).thenReturn(Optional.of(invoice2));

        assertThatThrownBy(() -> batchService.createBatch(user2.getId(), batchCreate, invoice2.getId()))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Cannot add batch to an invoice that is not pending.");
    }

    // == updateBatch tests ==

    @Test
    @DisplayName("Update batch with valid data should succeed")
    void testUpdateBatch_ValidData_ShouldSucceed() {
        BatchCreate batchCreate = new BatchCreate(batch1);
        batchCreate = new BatchCreate(batchCreate.expirationDate(), new BigDecimal("5.99"), 10, batchCreate.productBarcode());

        when(batchRepository.findById(batch1.getId())).thenReturn(Optional.of(batch1));
        when(productService.getOrCreateProductByBarcode("1234567890123", user1.getId())).thenReturn(productCustom);
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Batch updatedBatch = batchService.updateBatch(user1.getId(), batch1.getId(), batchCreate);

        assert(batch1.getExpirationDate() == null);
        assert(updatedBatch.getUnitPrice().equals(new BigDecimal("5.99")));
        assert(updatedBatch.getQuantity() == 10);
    }

    @Test
    @DisplayName("Update batch with valid data should succeed and product info does not exist, product is created from OpenFoodFacts")
    void testUpdateBatch_ValidData_ShouldSucceedAndProductInfoDoesNotExist(){
        BatchCreate batchCreate = new BatchCreate(batch2);
        batchCreate = new BatchCreate(batchCreate.expirationDate(), new BigDecimal("4.99"), 5, batchCreate.productBarcode());

        when(batchRepository.findById(batch2.getId())).thenReturn(Optional.of(batch2));
        when(productService.getOrCreateProductByBarcode("9876543210123", user1.getId())).thenReturn(productNoCustom);
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Batch updatedBatch = batchService.updateBatch(user1.getId(), batch2.getId(), batchCreate);

        assert(updatedBatch.getExpirationDate().equals(batch2.getExpirationDate()));
        assert(updatedBatch.getUnitPrice().equals(new BigDecimal("4.99")));
        assert(updatedBatch.getQuantity() == 5);
    }

    @Test
    @DisplayName("Update batch with non-existing batch should throw ResourceNotFoundException")
    void testUpdateBatch_NonExistingBatch_ShouldThrowResourceNotFoundException() {
        BatchCreate batchCreate = new BatchCreate(batch1);

        when(batchRepository.findById(batch1.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.updateBatch(user1.getId(), batch1.getId(), batchCreate))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Batch not found with id: " + batch1.getId());
    }

    @Test
    @DisplayName("Update batch with perishable product and null expiration date should throw ConflictException")
    void testUpdateBatch_PerishableProduct_NullExpirationDate_ShouldThrowConflictException() {
        BatchCreate batchCreate = new BatchCreate(null, batch2.getUnitPrice(), batch2.getQuantity(), batch2.getProduct().getBarcode());

        when(batchRepository.findById(batch2.getId())).thenReturn(Optional.of(batch2));
        when(productService.getOrCreateProductByBarcode("9876543210123", user1.getId())).thenReturn(productNoCustom);

        assertThatThrownBy(() -> batchService.updateBatch(user1.getId(), batch2.getId(), batchCreate))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Expiration date for product " + batch2.getProduct().getBarcode() + " is required for perishable products.");
    }

    @Test
    @DisplayName("Update batch with non-perishable product and non-null expiration date should throw ConflictException")
    void testUpdateBatch_NonPerishableProduct_NonNullExpirationDate_ShouldThrowConflictException() {
        BatchCreate batchCreate = new BatchCreate(LocalDate.of(2026, 3, 1), batch1.getUnitPrice(), batch1.getQuantity(), batch1.getProduct().getBarcode());

        when(batchRepository.findById(batch1.getId())).thenReturn(Optional.of(batch1));
        when(productService.getOrCreateProductByBarcode("1234567890123", user1.getId())).thenReturn(productCustom);

        assertThatThrownBy(() -> batchService.updateBatch(user1.getId(), batch1.getId(), batchCreate))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Expiration date for product " + batch1.getProduct().getBarcode() + " should not be provided for non-perishable products.");
    }

    @Test
    @DisplayName("Update batch with invoice that is not pending should throw ConflictException")
    void testUpdateBatch_InvoiceNotPending_ShouldThrowConflictException() {
        BatchCreate batchCreate = new BatchCreate(batch3);

        when(batchRepository.findById(batch3.getId())).thenReturn(Optional.of(batch3));

        assertThatThrownBy(() -> batchService.updateBatch(user2.getId(), batch3.getId(), batchCreate))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Cannot edit batch from an invoice that is not pending.");
    }

    @Test
    @DisplayName("Update batch with invoice belonging to another user should throw AccessDeniedException")
    void testUpdateBatch_InvoiceBelongsToAnotherUser_ShouldThrowAccessDeniedException() {
        BatchCreate batchCreate = new BatchCreate(batch3);

        when(batchRepository.findById(batch3.getId())).thenReturn(Optional.of(batch3));

        assertThatThrownBy(() -> batchService.updateBatch(user1.getId(), batch3.getId(), batchCreate))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("You don't have permission to edit this invoice.");
    }

    // == deleteBatch tests ==

    @Test
    @DisplayName("Delete batch with valid data should succeed")
    void testDeleteBatch_ValidData_ShouldSucceed() {

        when(batchRepository.findById(batch1.getId())).thenReturn(Optional.of(batch1));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(new Invoice());
        batchService.deleteBatch(user1.getId(), batch1.getId());

        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Delete batch with non-existing batch should throw ResourceNotFoundException")
    void testDeleteBatch_NonExistingBatch_ShouldThrowResourceNotFoundException() {
        when(batchRepository.findById(batch1.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.deleteBatch(user1.getId(), batch1.getId()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Batch not found with id: " + batch1.getId());
    }

    @Test
    @DisplayName("Delete batch with invoice belonging to another user should throw AccessDeniedException")
    void testDeleteBatch_InvoiceBelongsToAnotherUser_ShouldThrowAccessDeniedException() {

        when(batchRepository.findById(batch3.getId())).thenReturn(Optional.of(batch3));

        assertThatThrownBy(() -> batchService.deleteBatch(user1.getId(), batch3.getId()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("You don't have permission to edit this invoice.");
    }

    @Test
    @DisplayName("Delete batch with invoice that is not pending should throw ConflictException")
    void testDeleteBatch_InvoiceNotPending_ShouldThrowConflictException() {

        when(batchRepository.findById(batch3.getId())).thenReturn(Optional.of(batch3));

        assertThatThrownBy(() -> batchService.deleteBatch(user2.getId(), batch3.getId()))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Cannot delete batch from an invoice that is not pending.");
    }

    @Test
    @DisplayName("Delete batch when it's the only batch in the invoice should throw ConflictException")
    void testDeleteBatch_OnlyBatchInInvoice_ShouldThrowConflictException() {
        when(batchRepository.findById(batch1.getId())).thenReturn(Optional.of(batch1));
        batch1.getInvoice().setBatch(new LinkedList<>());
        batch1.getInvoice().getBatch().add(batch1);

        assertThatThrownBy(() -> batchService.deleteBatch(user1.getId(), batch1.getId()))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Cannot delete the only batch in an invoice. Consider deleting the invoice instead.");
    }

    @Test
    @DisplayName("Add stock quantity with valid data should succeed")
    void testAddStockQuantity_ValidData_ShouldSucceed() {
        when(productInfoService.getOrCreateProductInfo(batch1.getProduct().getId(), user1, batch1.getUnitPrice())).thenReturn(productInfo);
        batchService.addStockQuantity(batch1, 5, user1);

        verify(productInfoService, times(1)).editStockQuantity(productInfo.getId(), user1, 5, batch1.getUnitPrice());    
    }

}
