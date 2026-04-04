package com.expmatik.backend.invoice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.batch.Batch;
import com.expmatik.backend.batch.BatchService;
import com.expmatik.backend.batch.DTOs.BatchCreate;
import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.invoice.DTOs.InvoiceRequest;
import com.expmatik.backend.invoice.DTOs.InvoiceRequestUpdate;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.productInfo.ProductInfo;
import com.expmatik.backend.user.User;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private SupplierService supplierService;

    @Mock
    private BatchService batchService;

    @Spy
    private InvoiceCSVLector invoiceCSVLector = new InvoiceCSVLector();

    @Spy
    @InjectMocks
    private InvoiceService invoiceService;

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
        invoice1.getBatch().add(batch2);

        batch3 = new Batch();
        batch3.setId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        batch3.setProduct(productCustom);
        batch3.setQuantity(2);
        batch3.setUnitPrice(new BigDecimal("4.99"));
        batch3.setInvoice(invoice2);
        invoice2.getBatch().add(batch3);
    }

        @Test
        @DisplayName("readInvoicesFromCSV should throw BadRequestException when barcode is not numeric")
        void testReadInvoicesFromCSV_InvalidBarcode_NotNumeric() {
            User user = user1;
            MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                 "FAC-2026-001,Supplier A,PENDING,2026-03-10,ABC1234567,24,0.85,2026-09-30").getBytes());

            assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Line 2: invalid productBarcode. It must be numeric and contain 8 or 13 digits.");
        }

        @Test
        @DisplayName("readInvoicesFromCSV should throw BadRequestException when barcode length is not 8 or 13")
        void testReadInvoicesFromCSV_InvalidBarcode_Length() {
            User user = user1;
            MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                 "FAC-2026-001,Supplier A,PENDING,2026-03-10,1234567,24,0.85,2026-09-30").getBytes());

            assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Line 2: invalid productBarcode. It must be numeric and contain 8 or 13 digits.");
        }

        @Test
        @DisplayName("readInvoicesFromCSV should throw BadRequestException when quantity is not a valid integer")
        void testReadInvoicesFromCSV_InvalidQuantity_NotInteger() {
            User user = user1;
            MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                 "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,abc,0.85,2026-09-30").getBytes());

            assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Line 2: quantity is not a valid integer.");
        }

        @Test
        @DisplayName("readInvoicesFromCSV should throw BadRequestException when quantity is less than or equal to 0")
        void testReadInvoicesFromCSV_InvalidQuantity_Negative() {
            User user = user1;
            MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                 "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,0,0.85,2026-09-30").getBytes());

            assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Line 2: quantity must be greater than 0.");
        }

        @Test
        @DisplayName("readInvoicesFromCSV should throw BadRequestException when unitPrice is not a valid decimal")
        void testReadInvoicesFromCSV_InvalidUnitPrice_NotDecimal() {
            User user = user1;
            MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                 "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,24,abc,2026-09-30").getBytes());

            assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Line 2: unitPrice is not a valid decimal.");
        }

        @Test
        @DisplayName("readInvoicesFromCSV should throw BadRequestException when unitPrice is less than or equal to 0")
        void testReadInvoicesFromCSV_InvalidUnitPrice_Negative() {
            User user = user1;
            MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                 "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,24,0,2026-09-30").getBytes());

            assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Line 2: unitPrice must be greater than 0.");
        }

        @Test
        @DisplayName("readInvoicesFromCSV should throw BadRequestException when unitPrice has more than 10 integer digits")
        void testReadInvoicesFromCSV_InvalidUnitPrice_TooManyIntegerDigits() {
            User user = user1;
            MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                 "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,24,12345678901.00,2026-09-30").getBytes());

            assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Line 2: unitPrice must have at most 10 integer digits and 2 decimal places.");
        }

        @Test
        @DisplayName("readInvoicesFromCSV should throw BadRequestException when unitPrice has more than 2 decimal places")
        void testReadInvoicesFromCSV_InvalidUnitPrice_TooManyDecimals() {
            User user = user1;
            MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                 "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,24,1.234,2026-09-30").getBytes());

            assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Line 2: unitPrice must have at most 10 integer digits and 2 decimal places.");
        }

        @Test
        @DisplayName("readInvoicesFromCSV should throw BadRequestException when invoiceDate is invalid")
        void testReadInvoicesFromCSV_InvalidInvoiceDate() {
            User user = user1;
            MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                 "FAC-2026-001,Supplier A,PENDING,2026-13-10,12345678,24,0.85,2026-09-30").getBytes());

            assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Line 2: invalid invoiceDate. Expected format: yyyy-MM-dd.");
        }

        @Test
        @DisplayName("readInvoicesFromCSV should throw BadRequestException when expirationDate is invalid")
        void testReadInvoicesFromCSV_InvalidExpirationDate() {
            User user = user1;
            MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                 "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,24,0.85,2026-99-99").getBytes());

            assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Line 2: invalid expirationDate. Expected format: yyyy-MM-dd.");
        }

        @Test
        @DisplayName("readInvoicesFromCSV should skip blank rows and not throw error")
        void testReadInvoicesFromCSV_BlankRow() {
            User user = user1;
            MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                 "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,24,0.85,2026-09-30\n" +
                 ",,,,,,,\n" + // blank row
                 "FAC-2026-002,Supplier B,PENDING,2026-03-11,12345678,12,0.95,2026-08-01").getBytes());

            List<InvoiceRequest> createdInvoices = invoiceService.readInvoicesFromCSV(user, csvContent);
            assertNotNull(createdInvoices);
            assertEquals(2, createdInvoices.size());
        }
    // == createInvoice tests ==

    @Test
    @DisplayName("createInvoice should create a new invoice with valid data")
    void testCreateInvoice_ValidData() {
        User user = user1;
        BatchCreate batchCreate1 = new BatchCreate(batch1);
        UUID createdInvoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        InvoiceRequest request = new InvoiceRequest(
            "INV-001",
            "Supplier A",
            InvoiceStatus.PENDING,
            Arrays.asList(batchCreate1),
            LocalDate.of(2026, 3, 1)
        );

        when(invoiceRepository.findByInvoiceNumber("INV-001")).thenReturn(Optional.empty());
        when(supplierService.findOrRegister("Supplier A")).thenReturn(supplier1);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice savedInvoice = invocation.getArgument(0);
            if (savedInvoice.getId() == null) {
                savedInvoice.setId(createdInvoiceId);
            }
            return savedInvoice;
        });
        when(batchService.createBatch(any(UUID.class), any(BatchCreate.class), any(UUID.class)))
            .thenAnswer(invocation -> {
                BatchCreate batchCreate = invocation.getArgument(1);
                Batch batch = new Batch();
                batch.setId(UUID.randomUUID());
                batch.setProduct(batch1.getProduct());
                batch.setQuantity(batchCreate.quantity());
                batch.setUnitPrice(batchCreate.unitPrice());
                return batch;
            });

            Invoice createdInvoice = invoiceService.createInvoice(user, request);

            assertNotNull(createdInvoice);
            assertEquals(request.invoiceDate(), createdInvoice.getInvoiceDate());
            assertEquals(request.status(), createdInvoice.getStatus());
            assertEquals(request.invoiceNumber(), createdInvoice.getInvoiceNumber());
            assertEquals(request.supplierName(), createdInvoice.getSupplier().getName());
            assertEquals(user, createdInvoice.getUser());
            assertEquals(1, createdInvoice.getBatch().size());
            assertEquals(batchCreate1.quantity(), createdInvoice.getBatch().get(0).getQuantity());

            verify(supplierService).findOrRegister("Supplier A");
            verify(invoiceRepository).findByInvoiceNumber("INV-001");
            verify(batchService).createBatch(eq(user.getId()), eq(batchCreate1), eq(createdInvoiceId));
            verify(invoiceRepository, times(2)).save(any(Invoice.class));
    }
    @Test
    @DisplayName("createInvoice should create a new invoice with status RECEIVED and mark it as received")
    void testCreateInvoice_ValidDataReceivedStatus() {

        User user = user1;
        BatchCreate batchCreate1 = new BatchCreate(batch1);
        UUID createdInvoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        InvoiceRequest request = new InvoiceRequest(
            "INV-004",
            "Supplier A",
            InvoiceStatus.RECEIVED,
            Arrays.asList(batchCreate1),
            LocalDate.of(2026, 3, 1)
        );

        when(invoiceRepository.findByInvoiceNumber("INV-004")).thenReturn(Optional.empty());
        when(supplierService.findOrRegister("Supplier A")).thenReturn(supplier1);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice savedInvoice = invocation.getArgument(0);
            if (savedInvoice.getId() == null) {
                savedInvoice.setId(createdInvoiceId);
            }
            return savedInvoice;
        });
        when(batchService.createBatch(any(UUID.class), any(BatchCreate.class), any(UUID.class)))
            .thenAnswer(invocation -> {
                BatchCreate batchCreate = invocation.getArgument(1);
                Batch batch = new Batch();
                batch.setId(UUID.randomUUID());
                batch.setProduct(batch1.getProduct());
                batch.setQuantity(batchCreate.quantity());
                batch.setUnitPrice(batchCreate.unitPrice());
                return batch;
            });

            Invoice createdInvoice = invoiceService.createInvoice(user, request);

            assertNotNull(createdInvoice);
            assertEquals(request.invoiceDate(), createdInvoice.getInvoiceDate());
            assertEquals(request.status(), createdInvoice.getStatus());
            assertEquals(request.invoiceNumber(), createdInvoice.getInvoiceNumber());
            assertEquals(request.supplierName(), createdInvoice.getSupplier().getName());
            assertEquals(user, createdInvoice.getUser());
            assertEquals(1, createdInvoice.getBatch().size());
            assertEquals(batchCreate1.quantity(), createdInvoice.getBatch().get(0).getQuantity());
            assertEquals(InvoiceStatus.RECEIVED, createdInvoice.getStatus());

            verify(supplierService).findOrRegister("Supplier A");
            verify(invoiceRepository).findByInvoiceNumber("INV-004");
            verify(batchService).createBatch(eq(user.getId()), eq(batchCreate1), eq(createdInvoiceId));
            verify(invoiceRepository, times(3)).save(any(Invoice.class));
    
    }

    @Test
    @DisplayName("createInvoice should throw BadRequestException if batches list is empty")
    void testCreateInvoice_EmptyBatches() {
        User user = user1;
        InvoiceRequest request = new InvoiceRequest(
            "INV-004",
            "Supplier A",
            InvoiceStatus.PENDING,
            new ArrayList<>(),
            LocalDate.of(2026, 3, 1)
        );

        assertThatThrownBy(() -> invoiceService.createInvoice(user, request))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Invoice must have at least one batch");
    }

    @Test
    @DisplayName("createInvoice should throw ConflictException if invoice number already exists")
    void testCreateInvoice_DuplicateInvoiceNumber() {
        User user = user1;
        InvoiceRequest request = new InvoiceRequest(
            "INV-001",
            "Supplier A",
            InvoiceStatus.PENDING,
            Arrays.asList(new BatchCreate(batch1)),
            LocalDate.of(2026, 3, 1)
        );

        when(invoiceRepository.findByInvoiceNumber("INV-001")).thenReturn(Optional.of(invoice1));

        assertThatThrownBy(() -> invoiceService.createInvoice(user, request))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Invoice number: " + request.invoiceNumber() + " already exists");
    }

    // == findInvoiceById tests ==

    @Test
    @DisplayName("findInvoiceById should return invoice if it exists and belongs to user")
    void testFindInvoiceById_ValidId() {
        UUID invoiceId = invoice1.getId();
        UUID userId = user1.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

        Invoice foundInvoice = invoiceService.findInvoiceById(invoiceId, userId);

        assertNotNull(foundInvoice);
        assertEquals(invoice1, foundInvoice);
    }

    @Test
    @DisplayName("findInvoiceById should throw ResourceNotFoundException if invoice does not exist")
    void testFindInvoiceById_NotFound() {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        UUID userId = user1.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.findInvoiceById(invoiceId, userId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Invoice not found");
    }

    @Test
    @DisplayName("findInvoiceById should throw AccessDeniedException if invoice belongs to another user")
    void testFindInvoiceById_Unauthorized() {
        UUID invoiceId = invoice1.getId();
        UUID userId = user2.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

        assertThatThrownBy(() -> invoiceService.findInvoiceById(invoiceId, userId))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Unauthorized access to invoice");
    }

    // == findInvoiceByInvoiceNumber tests ==

    @Test
    @DisplayName("findInvoiceByInvoiceNumber should return invoice if it exists and belongs to user")
    void testFindInvoiceByInvoiceNumber_ValidNumber() {
        String invoiceNumber = invoice1.getInvoiceNumber();
        UUID userId = user1.getId();

        when(invoiceRepository.findByInvoiceNumber(invoiceNumber)).thenReturn(Optional.of(invoice1));

        Invoice foundInvoice = invoiceService.findInvoiceByInvoiceNumber(invoiceNumber, userId);

        assertNotNull(foundInvoice);
        assertEquals(invoice1, foundInvoice);
    }

    @Test
    @DisplayName("findInvoiceByInvoiceNumber should throw ResourceNotFoundException if invoice does not exist")
    void testFindInvoiceByInvoiceNumber_NotFound() {
        String invoiceNumber = "INV-009";
        UUID userId = user1.getId();

        when(invoiceRepository.findByInvoiceNumber(invoiceNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.findInvoiceByInvoiceNumber(invoiceNumber, userId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Invoice not found");
    }

    @Test
    @DisplayName("findInvoiceByInvoiceNumber should throw AccessDeniedException if invoice belongs to another user")
    void testFindInvoiceByInvoiceNumber_Unauthorized() {
        String invoiceNumber = invoice1.getInvoiceNumber();
        UUID userId = user2.getId();

        when(invoiceRepository.findByInvoiceNumber(invoiceNumber)).thenReturn(Optional.of(invoice1));

        assertThatThrownBy(() -> invoiceService.findInvoiceByInvoiceNumber(invoiceNumber, userId))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Unauthorized access to invoice");
    }

    // == updateInvoiceStatus tests ==

    @Test
    @DisplayName("updateInvoiceStatus should update status if invoice exists and belongs to user")
    void testUpdateInvoiceStatus_Valid() {
        UUID invoiceId = invoice1.getId();
        InvoiceStatus newStatus = InvoiceStatus.CANCELED;
        UUID userId = user1.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice updatedInvoice = invoiceService.updateInvoiceStatus(invoiceId, newStatus, userId);

        assertNotNull(updatedInvoice);
        assertEquals(newStatus, updatedInvoice.getStatus());

        verify(invoiceRepository).findById(invoiceId);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("updateInvoiceStatus should update status if invoice exists and belongs to user")
    void testUpdateInvoiceStatus_ValidStatusReceived() {
        UUID invoiceId = invoice1.getId();
        InvoiceStatus newStatus = InvoiceStatus.RECEIVED;
        UUID userId = user1.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice updatedInvoice = invoiceService.updateInvoiceStatus(invoiceId, newStatus, userId);

        assertNotNull(updatedInvoice);
        assertEquals(newStatus, updatedInvoice.getStatus());

        verify(invoiceRepository).findById(invoiceId);
        verify(batchService, times(invoice1.getBatch().size())).addStockQuantity(any(Batch.class), anyInt(), eq(user1));
        verify(invoiceRepository).save(any(Invoice.class));
    }
    @Test
    @DisplayName("updateInvoiceStatus should throw ResourceNotFoundException if invoice does not exist")
    void testUpdateInvoiceStatus_NotFound() {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        InvoiceStatus newStatus = InvoiceStatus.CANCELED;
        UUID userId = user1.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.updateInvoiceStatus(invoiceId, newStatus, userId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Invoice not found");
    }


    @Test
    @DisplayName("updateInvoiceStatus should throw AccessDeniedException if invoice belongs to another user")
    void testUpdateInvoiceStatus_Unauthorized() {
        UUID invoiceId = invoice1.getId();
        InvoiceStatus newStatus = InvoiceStatus.CANCELED;
        UUID userId = user2.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

        assertThatThrownBy(() -> invoiceService.updateInvoiceStatus(invoiceId, newStatus, userId))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Unauthorized access to invoice");
    }

    @Test
    @DisplayName("updateInvoiceStatus should throw ConflictException if trying to change status of a received invoice")
    void testUpdateInvoiceStatus_ConflictReceived() {
        UUID invoiceId = invoice2.getId();
        InvoiceStatus newStatus = InvoiceStatus.CANCELED;
        UUID userId = user2.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice2));

        assertThatThrownBy(() -> invoiceService.updateInvoiceStatus(invoiceId, newStatus, userId))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Only pending invoices can be updated");
    }

    @Test
    @DisplayName("updateInvoiceStatus should throw ConflictException if trying to change status of a canceled invoice")
    void testUpdateInvoiceStatus_ConflictCanceled() {
        UUID invoiceId = invoice1.getId();
        InvoiceStatus newStatus = InvoiceStatus.RECEIVED;
        invoice1.setStatus(InvoiceStatus.CANCELED);
        UUID userId = user1.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

        assertThatThrownBy(() -> invoiceService.updateInvoiceStatus(invoiceId, newStatus, userId))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Only pending invoices can be updated");
    }
    // == markInvoiceAsReceived tests ==

    @Test
    @DisplayName("markInvoiceAsReceived should update stock quantity and set status to RECEIVED")
    void testMarkInvoiceAsReceived() {
        Invoice invoice = invoice1;
        invoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice updatedInvoice = invoiceService.markInvoiceAsReceived(invoice);

        assertNotNull(updatedInvoice);
        assertEquals(InvoiceStatus.RECEIVED, updatedInvoice.getStatus());

        verify(batchService, times(invoice.getBatch().size())).addStockQuantity(any(Batch.class), anyInt(), eq(user1));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    // == deleteInvoice tests ==

    @Test
    @DisplayName("deleteInvoice should delete invoice if it exists and belongs to user")
    void testDeleteInvoice_Valid() {
        UUID invoiceId = invoice1.getId();
        UUID userId = user1.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

        invoiceService.deleteInvoice(invoiceId, userId);

        verify(invoiceRepository).findById(invoiceId);
        verify(invoiceRepository).delete(invoice1);
    }

    @Test
    @DisplayName("deleteInvoice should throw ResourceNotFoundException if invoice does not exist")
    void testDeleteInvoice_NotFound() {
        UUID invoiceId = invoice1.getId();
        UUID userId = user1.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.deleteInvoice(invoiceId, userId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Invoice not found");
    }

    @Test
    @DisplayName("deleteInvoice should throw AccessDeniedException if invoice belongs to another user")
    void testDeleteInvoice_Unauthorized() {
        UUID invoiceId = invoice1.getId();
        UUID userId = user2.getId();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

        assertThatThrownBy(() -> invoiceService.deleteInvoice(invoiceId, userId))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Unauthorized access to invoice");
    }

    @Test
    @DisplayName("deleteInvoice should throw AccessDeniedException if invoice is not pending")
    void testDeleteInvoice_NotPending() {
        UUID invoiceId = invoice1.getId();
        UUID userId = user1.getId();
        invoice1.setStatus(InvoiceStatus.RECEIVED);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

        assertThatThrownBy(() -> invoiceService.deleteInvoice(invoiceId, userId))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Only pending invoices can be deleted");
    }

    //== updateInvoice tests ==

    @Test
    @DisplayName("updateInvoice should update invoice if it exists and belongs to user")
    void testUpdateInvoice_Valid() {
        UUID invoiceId = invoice1.getId();
        UUID userId = user1.getId();
        InvoiceRequestUpdate request = new InvoiceRequestUpdate(
            "INV-001-UPDATED",
            "Supplier A",
            InvoiceStatus.PENDING,
            LocalDate.of(2026, 3, 1)
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));
        when(supplierService.findOrRegister("Supplier A")).thenReturn(supplier1);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice updatedInvoice = invoiceService.updateInvoice(invoiceId, request, userId);

        assertNotNull(updatedInvoice);
        assertEquals(request.invoiceDate(), updatedInvoice.getInvoiceDate());
        assertEquals(request.status(), updatedInvoice.getStatus());
        assertEquals(request.invoiceNumber(), updatedInvoice.getInvoiceNumber());
        assertEquals(request.supplierName(), updatedInvoice.getSupplier().getName());
        assertEquals(user1, updatedInvoice.getUser());
        assertEquals(2, updatedInvoice.getBatch().size());
        assertEquals(batch1.getQuantity(), updatedInvoice.getBatch().get(0).getQuantity());
        assertEquals(batch2.getQuantity(), updatedInvoice.getBatch().get(1).getQuantity());

        verify(invoiceRepository).findById(invoiceId);
        verify(supplierService).findOrRegister("Supplier A");

        
    }

    @Test
    @DisplayName("updateInvoice should update invoice with a new invoice number if it does not conflict with existing invoices")
    void testUpdateInvoice_ValidAndNewInvoiceNumber() {

        UUID invoiceId = invoice1.getId();
        UUID userId = user1.getId();
        InvoiceRequestUpdate request = new InvoiceRequestUpdate(
            "INV-001-UPDATED",
            "Supplier A",
            InvoiceStatus.PENDING,
            LocalDate.of(2026, 3, 1)
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));
        when(supplierService.findOrRegister("Supplier A")).thenReturn(supplier1);
        when(invoiceRepository.findByInvoiceNumber("INV-001-UPDATED")).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice updatedInvoice = invoiceService.updateInvoice(invoiceId, request, userId);

        assertNotNull(updatedInvoice);
        assertEquals(request.invoiceDate(), updatedInvoice.getInvoiceDate());
        assertEquals(request.status(), updatedInvoice.getStatus());
        assertEquals(request.invoiceNumber(), updatedInvoice.getInvoiceNumber());
        assertEquals(request.supplierName(), updatedInvoice.getSupplier().getName());

        verify(invoiceRepository).findById(invoiceId);
        verify(supplierService).findOrRegister("Supplier A");
        verify(invoiceRepository).findByInvoiceNumber("INV-001-UPDATED");

        
    }

    @Test
    @DisplayName("updateInvoice should throw ResourceNotFoundException if invoice does not exist")
    void testUpdateInvoice_NotFound() {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        UUID userId = user1.getId();
        InvoiceRequestUpdate request = new InvoiceRequestUpdate(
            "INV-001-UPDATED",
            "Supplier A",
            InvoiceStatus.PENDING,
            LocalDate.of(2026, 3, 1)
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.updateInvoice(invoiceId, request, userId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Invoice not found");
    }

    @Test
    @DisplayName("updateInvoice should throw AccessDeniedException if invoice belongs to another user")
    void testUpdateInvoice_Unauthorized() {
        UUID invoiceId = invoice1.getId();
        UUID userId = user2.getId();
        InvoiceRequestUpdate request = new InvoiceRequestUpdate(
            "INV-001-UPDATED",
            "Supplier A",
            InvoiceStatus.PENDING,
            LocalDate.of(2026, 3, 1)
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

        assertThatThrownBy(() -> invoiceService.updateInvoice(invoiceId, request, userId))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Unauthorized access to invoice");
    }

    @Test
    @DisplayName("updateInvoice should throw ConflictException if trying to update a received invoice")
    void testUpdateInvoice_ConflictReceived() {
        UUID invoiceId = invoice2.getId();
        UUID userId = user2.getId();
        InvoiceRequestUpdate request = new InvoiceRequestUpdate(
            "INV-002-UPDATED",
            "Supplier B",
            InvoiceStatus.RECEIVED,
            LocalDate.of(2026, 3, 1)
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice2));

        assertThatThrownBy(() -> invoiceService.updateInvoice(invoiceId, request, userId))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Only pending invoices can be updated");
    }

    @Test
    @DisplayName("updateInvoice should throw ConflictException if trying to update a canceled invoice")
    void testUpdateInvoice_ConflictCanceled() {
        UUID invoiceId = invoice1.getId();
        UUID userId = user1.getId();
        InvoiceRequestUpdate request = new InvoiceRequestUpdate(
            "INV-001-UPDATED",
            "Supplier A",
            InvoiceStatus.CANCELED,
            LocalDate.of(2026, 3, 1)
        );
        invoice1.setStatus(InvoiceStatus.CANCELED);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

        assertThatThrownBy(() -> invoiceService.updateInvoice(invoiceId, request, userId))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Only pending invoices can be updated");
    }

    @Test
    @DisplayName("updateInvoice should throw ConflictException if trying to update invoice number to an existing one")
    void testUpdateInvoice_ConflictDuplicateNumber() {
        UUID invoiceId = invoice1.getId();
        UUID userId = user1.getId();
        InvoiceRequestUpdate request = new InvoiceRequestUpdate(
            "INV-002",
            "Supplier A",
            InvoiceStatus.PENDING,
            LocalDate.of(2026, 3, 1)
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));
        when(invoiceRepository.findByInvoiceNumber("INV-002")).thenReturn(Optional.of(invoice2));

        assertThatThrownBy(() -> invoiceService.updateInvoice(invoiceId, request, userId))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Invoice number: " + request.invoiceNumber() + " already exists");
    }

    // == readInvoicesFromCSV tests ==

    @Test
    @DisplayName("readInvoicesFromCSV should create multiple invoices from valid CSV")
    void testReadInvoicesFromCSV_Valid() {
        User user = user1;
        MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
             "FAC-2026-001,Supplier A,PENDING,2026-03-10,5000112556780,24,0.85,2026-09-30\n" +
             "FAC-2026-001,Supplier A,PENDING,2026-03-10,5000112556780,10,1.20,2026-09-30\n" +
             "FAC-2026-002,Supplier B,PENDING,2026-03-11,8410494300050,12,0.95,2026-08-01").getBytes());

        List<InvoiceRequest> createdInvoices = invoiceService.readInvoicesFromCSV(user, csvContent);

        assertNotNull(createdInvoices);
        assertEquals(2, createdInvoices.size());

        InvoiceRequest firstInvoice = createdInvoices.get(0);
        InvoiceRequest secondInvoice = createdInvoices.get(1);

        assertEquals("FAC-2026-001", firstInvoice.invoiceNumber());
        assertEquals("Supplier A", firstInvoice.supplierName());
        assertEquals(2, firstInvoice.batches().size());

        assertEquals("FAC-2026-002", secondInvoice.invoiceNumber());
        assertEquals("Supplier B", secondInvoice.supplierName());
        assertEquals(1, secondInvoice.batches().size());
    }

    @Test
    @DisplayName("readInvoicesFromCSV should throw BadRequestException if no file is provided or file is empty")
    void testReadInvoicesFromCSV_NoFile() {
        User user = user1;
        MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("No file uploaded or file is empty.");
    }

    @Test
    @DisplayName("readInvoicesFromCSV should throw BadRequestException if file is null")
    void testReadInvoicesFromCSV_NullFile() {
        User user = user1;
        MultipartFile csvContent = null;

        assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("No file uploaded or file is empty.");
    }

    @Test
    @DisplayName("readInvoicesFromCSV should throw BadRequestException if file is not a CSV")
    void testReadInvoicesFromCSV_InvalidFileType() {
        User user = user1;
        MultipartFile csvContent = new MockMultipartFile("file", "invoices.txt", "text/plain",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
             "FAC-2026-001,Supplier A,PENDING,2026-03-10,5000112556780,24,0.85,2026-09-30").getBytes());

        assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("The file must have a .csv extension.");
    }

    @Test
    @DisplayName("readInvoicesFromCSV should throw BadRequestException if CSV contains no valid invoice records")
    void testReadInvoicesFromCSV_NoValidRecords() {
        User user = user1;
        MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate").getBytes());

        assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("The CSV does not contain valid invoice records.");
    }

    @Test
    @DisplayName("readInvoicesFromCSV should throw BadRequestException if a row has an incorrect number of columns")
    void testReadInvoicesFromCSV_IncorrectColumns() {
        User user = user1;
        MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
             "FAC-2026-001,Supplier A,PENDING,2026-03-10,5000112556780,24,0.85").getBytes());

        assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Line 2: expected 8 columns but found 7.");
    }

    @Test
    @DisplayName("readInvoicesFromCSV should throw BadRequestException for inconsistent supplierName, status and invoiceDate")
    void testReadInvoicesFromCSV_InconsistentData() {
        User user = user1;

        MultipartFile supplierNameCsv = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
             "FAC-2026-001,Supplier A,PENDING,2026-03-10,5000112556780,24,0.85,2026-09-30\n" +
             "FAC-2026-001,Supplier B,PENDING,2026-03-10,5000112556780,10,1.20,2026-09-30").getBytes());

        assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, supplierNameCsv))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Line 3: inconsistent supplierName for invoice FAC-2026-001.");

        MultipartFile statusCsv = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
             "FAC-2026-001,Supplier A,PENDING,2026-03-10,5000112556780,24,0.85,2026-09-30\n" +
             "FAC-2026-001,Supplier A,RECEIVED,2026-03-10,5000112556780,10,1.20,2026-09-30").getBytes());

        assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, statusCsv))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Line 3: inconsistent status for invoice FAC-2026-001.");

        MultipartFile invoiceDateCsv = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
             "FAC-2026-001,Supplier A,PENDING,2026-03-10,5000112556780,24,0.85,2026-09-30\n" +
             "FAC-2026-001,Supplier A,PENDING,2026-03-11,5000112556780,10,1.20,2026-09-30").getBytes());

        assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, invoiceDateCsv))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Line 3: inconsistent invoiceDate for invoice FAC-2026-001.");
    }

    @Test
    @DisplayName("readInvoicesFromCSV should throw BadRequestException when status is empty")
    void testReadInvoicesFromCSV_EmptyStatus() {
        User user = user1;
        MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
             "FAC-2026-001,Supplier A,,2026-03-10,5000112556780,24,0.85,2026-09-30").getBytes());

        assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Line 2: required field is empty -> status.");
    }

    @Test
    @DisplayName("readInvoicesFromCSV should throw BadRequestException when status is invalid")
    void testReadInvoicesFromCSV_InvalidStatus() {
        User user = user1;
        MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
             "FAC-2026-001,Supplier A,SENT,2026-03-10,5000112556780,24,0.85,2026-09-30").getBytes());

        assertThatThrownBy(() -> invoiceService.readInvoicesFromCSV(user, csvContent))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Line 2: invalid status -> SENT. Allowed values: PENDING, RECEIVED, CANCELED.");
    }

    // == validateInvoiceSearchInputs tests ==

    @Test
    @DisplayName("validateInvoiceSearchInputs should throw BadRequestException if minPrice is negative")
    void testValidateInvoiceSearchInputs_NegativeMinPrice() {
        assertThatThrownBy(() -> invoiceService.validateInvoiceSearchInputs(null, null, BigDecimal.valueOf(-1.0), null))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Min price cannot be negative");
    }

    @Test
    @DisplayName("validateInvoiceSearchInputs should throw BadRequestException if maxPrice is negative")
    void testValidateInvoiceSearchInputs_NegativeMaxPrice() {
        assertThatThrownBy(() -> invoiceService.validateInvoiceSearchInputs(null, null, null, BigDecimal.valueOf(-1.0)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Max price cannot be negative");
    }

    @Test
    @DisplayName("validateInvoiceSearchInputs should throw BadRequestException if minPrice is greater than maxPrice")
    void testValidateInvoiceSearchInputs_MinPriceGreaterThanMaxPrice() {
        assertThatThrownBy(() -> invoiceService.validateInvoiceSearchInputs(null, null, BigDecimal.valueOf(10.0), BigDecimal.valueOf(5.0)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Min price cannot be greater than max price");
    }

    @Test
    @DisplayName("validateInvoiceSearchInputs should throw BadRequestException if startDate is after endDate")
    void testValidateInvoiceSearchInputs_StartDateAfterEndDate() {
        assertThatThrownBy(() -> invoiceService.validateInvoiceSearchInputs(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 1), null, null))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Start date cannot be after end date");
    }

    
}
