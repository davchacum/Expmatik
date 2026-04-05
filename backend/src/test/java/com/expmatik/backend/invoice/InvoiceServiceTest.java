package com.expmatik.backend.invoice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.Nested;
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
    // == readInvoicesFromCSV tests ==

    @Nested
    @DisplayName("readInvoicesFromCSV")
    class ReadInvoicesFromCSVTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("readInvoicesFromCSV should create multiple invoices from valid CSV")
            void testReadInvoicesFromCSV_ValidData_shouldCreateInvoices() {
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
            @DisplayName("readInvoicesFromCSV should skip blank rows and not throw error")
            void testReadInvoicesFromCSV_BlankRow_ShouldSkip() {
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
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException when barcode is not numeric")
            void testReadInvoicesFromCSV_InvalidBarcodeNotNumeric_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,ABC1234567,24,0.85,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("Line 2: invalid productBarcode. It must be numeric and contain 8 or 13 digits.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException when barcode length is not 8 or 13")
            void testReadInvoicesFromCSV_InvalidBarcodeLength_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,1234567,24,0.85,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("Line 2: invalid productBarcode. It must be numeric and contain 8 or 13 digits.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException when quantity is not a valid integer")
            void testReadInvoicesFromCSV_InvalidQuantityNotInteger_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,abc,0.85,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("Line 2: quantity is not a valid integer.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException when quantity is less than or equal to 0")
            void testReadInvoicesFromCSV_InvalidQuantityNegative_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,0,0.85,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("Line 2: quantity must be greater than 0.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException when unitPrice is not a valid decimal")
            void testReadInvoicesFromCSV_InvalidUnitPriceNotDecimal_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,24,abc,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("Line 2: unitPrice is not a valid decimal.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException when unitPrice is less than or equal to 0")
            void testReadInvoicesFromCSV_InvalidUnitPriceNegative_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,24,0,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("Line 2: unitPrice must be greater than 0.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException when unitPrice has more than 10 integer digits")
            void testReadInvoicesFromCSV_InvalidUnitPriceTooManyIntegerDigits_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,24,12345678901.00,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("Line 2: unitPrice must have at most 10 integer digits and 2 decimal places.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException when unitPrice has more than 2 decimal places")
            void testReadInvoicesFromCSV_InvalidUnitPriceTooManyDecimals_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,24,1.234,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("Line 2: unitPrice must have at most 10 integer digits and 2 decimal places.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException when invoiceDate is invalid")
            void testReadInvoicesFromCSV_InvalidInvoiceDate_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-13-10,12345678,24,0.85,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("Line 2: invalid invoiceDate. Expected format: yyyy-MM-dd.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException when expirationDate is invalid")
            void testReadInvoicesFromCSV_InvalidExpirationDate_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,12345678,24,0.85,2026-99-99").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("Line 2: invalid expirationDate. Expected format: yyyy-MM-dd.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException if no file is provided or file is empty")
            void testReadInvoicesFromCSV_NoFile_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv", new byte[0]);

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("No file uploaded or file is empty.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException if file is null")
            void testReadInvoicesFromCSV_NullFile_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = null;

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("No file uploaded or file is empty.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException if file is not a CSV")
            void testReadInvoicesFromCSV_InvalidFileType_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.txt", "text/plain",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,5000112556780,24,0.85,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("The file must have a .csv extension.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException if CSV contains no valid invoice records")
            void testReadInvoicesFromCSV_NoValidRecords_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );
                assertEquals("The CSV does not contain valid invoice records.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException if a row has an incorrect number of columns")
            void testReadInvoicesFromCSV_IncorrectColumns_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,5000112556780,24,0.85").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );

                assertEquals("Line 2: expected 8 columns but found 7.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException for inconsistent supplierName, status and invoiceDate")
            void testReadInvoicesFromCSV_InconsistentData_shouldThrowBadRequestException() {
                User user = user1;

                MultipartFile supplierNameCsv = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,5000112556780,24,0.85,2026-09-30\n" +
                    "FAC-2026-001,Supplier B,PENDING,2026-03-10,5000112556780,10,1.20,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, supplierNameCsv)
                );
                assertEquals("Line 3: inconsistent supplierName for invoice FAC-2026-001.", exception.getMessage());

                MultipartFile statusCsv = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,5000112556780,24,0.85,2026-09-30\n" +
                    "FAC-2026-001,Supplier A,RECEIVED,2026-03-10,5000112556780,10,1.20,2026-09-30").getBytes());

                BadRequestException statusException = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, statusCsv)
                );
                assertEquals("Line 3: inconsistent status for invoice FAC-2026-001.", statusException.getMessage());

                MultipartFile invoiceDateCsv = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-10,5000112556780,24,0.85,2026-09-30\n" +
                    "FAC-2026-001,Supplier A,PENDING,2026-03-11,5000112556780,10,1.20,2026-09-30").getBytes());

                BadRequestException invoiceDateException = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, invoiceDateCsv)
                );
                assertEquals("Line 3: inconsistent invoiceDate for invoice FAC-2026-001.", invoiceDateException.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException when status is empty")
            void testReadInvoicesFromCSV_EmptyStatus_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,,2026-03-10,5000112556780,24,0.85,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );
                assertEquals("Line 2: required field is empty -> status.", exception.getMessage());
            }

            @Test
            @DisplayName("readInvoicesFromCSV should throw BadRequestException when status is invalid")
            void testReadInvoicesFromCSV_InvalidStatus_shouldThrowBadRequestException() {
                User user = user1;
                MultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier A,SENT,2026-03-10,5000112556780,24,0.85,2026-09-30").getBytes());

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.readInvoicesFromCSV(user, csvContent)
                );
                assertEquals("Line 2: invalid status -> SENT. Allowed values: PENDING, RECEIVED, CANCELED.", exception.getMessage());
            }
        }
    }

    // == createInvoice tests ==

    @Nested
    @DisplayName("createInvoice")
    class CreateInvoiceTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("createInvoice should create a new invoice with valid data")
            void testCreateInvoice_ValidData_shouldCreateInvoice() {
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
            void testCreateInvoice_ValidDataReceivedStatus_shouldCreateInvoice() {

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

        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("createInvoice should throw BadRequestException if batches list is empty")
            void testCreateInvoice_EmptyBatches_shouldThrowBadRequestException() {
                User user = user1;
                InvoiceRequest request = new InvoiceRequest(
                    "INV-004",
                    "Supplier A",
                    InvoiceStatus.PENDING,
                    new ArrayList<>(),
                    LocalDate.of(2026, 3, 1)
                );

                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.createInvoice(user, request)
                );
                assertEquals("Invoice must have at least one batch", exception.getMessage());
            }

            @Test
            @DisplayName("createInvoice should throw ConflictException if invoice number already exists")
            void testCreateInvoice_DuplicateInvoiceNumber_shouldThrowConflictException() {
                User user = user1;
                InvoiceRequest request = new InvoiceRequest(
                    "INV-001",
                    "Supplier A",
                    InvoiceStatus.PENDING,
                    Arrays.asList(new BatchCreate(batch1)),
                    LocalDate.of(2026, 3, 1)
                );

                when(invoiceRepository.findByInvoiceNumber("INV-001")).thenReturn(Optional.of(invoice1));

                ConflictException exception = assertThrows(ConflictException.class, () ->
                    invoiceService.createInvoice(user, request)
                );
                assertEquals("Invoice number: " + request.invoiceNumber() + " already exists", exception.getMessage());
            }
        }
    }

    // == findInvoiceById tests ==

    @Nested
    @DisplayName("findInvoiceById")
    class FindInvoiceByIdTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("findInvoiceById should return invoice if it exists and belongs to user")
            void testFindInvoiceById_ValidId_Success() {
                UUID invoiceId = invoice1.getId();
                UUID userId = user1.getId();

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

                Invoice foundInvoice = invoiceService.findInvoiceById(invoiceId, userId);

                assertNotNull(foundInvoice);
                assertEquals(invoice1, foundInvoice);
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("findInvoiceById should throw ResourceNotFoundException if invoice does not exist")
            void testFindInvoiceById_InvalidId_shouldThrowResourceNotFoundException() {
                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");
                UUID userId = user1.getId();

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

                ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                    invoiceService.findInvoiceById(invoiceId, userId)
                );
                assertEquals("Invoice not found", exception.getMessage());
            }

            @Test
            @DisplayName("findInvoiceById should throw AccessDeniedException if invoice belongs to another user")
            void testFindInvoiceById_InvoiceNotOwnedByUser_shouldThrowAccessDeniedException() {
                UUID invoiceId = invoice1.getId();
                UUID userId = user2.getId();

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

                AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                    invoiceService.findInvoiceById(invoiceId, userId)
                );
                assertEquals("Unauthorized access to invoice", exception.getMessage());
            }
        }
    }

    // == findInvoiceByInvoiceNumber tests ==

    @Nested
    @DisplayName("findInvoiceByInvoiceNumber")
    class FindInvoiceByInvoiceNumberTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("findInvoiceByInvoiceNumber should return invoice if it exists and belongs to user")
            void testFindInvoiceByInvoiceNumber_ValidNumber_Success() {
                String invoiceNumber = invoice1.getInvoiceNumber();
                UUID userId = user1.getId();

                when(invoiceRepository.findByInvoiceNumber(invoiceNumber)).thenReturn(Optional.of(invoice1));

                Invoice foundInvoice = invoiceService.findInvoiceByInvoiceNumber(invoiceNumber, userId);

                assertNotNull(foundInvoice);
                assertEquals(invoice1, foundInvoice);
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("findInvoiceByInvoiceNumber should throw ResourceNotFoundException if invoice does not exist")
            void testFindInvoiceByInvoiceNumber_InvalidNumber_shouldThrowResourceNotFoundException() {
                String invoiceNumber = "INV-009";
                UUID userId = user1.getId();

                when(invoiceRepository.findByInvoiceNumber(invoiceNumber)).thenReturn(Optional.empty());

                ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                    invoiceService.findInvoiceByInvoiceNumber(invoiceNumber, userId)
                );
                assertEquals("Invoice not found", exception.getMessage());
            }

            @Test
            @DisplayName("findInvoiceByInvoiceNumber should throw AccessDeniedException if invoice belongs to another user")
            void testFindInvoiceByInvoiceNumber_InvoiceNotOwnedByUser_shouldThrowAccessDeniedException() {
                String invoiceNumber = invoice1.getInvoiceNumber();
                UUID userId = user2.getId();

                when(invoiceRepository.findByInvoiceNumber(invoiceNumber)).thenReturn(Optional.of(invoice1));

                AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                    invoiceService.findInvoiceByInvoiceNumber(invoiceNumber, userId)
                );
                assertEquals("Unauthorized access to invoice", exception.getMessage());
            }
        }
    }

    // == updateInvoiceStatus tests ==

    @Nested
    @DisplayName("updateInvoiceStatus")
    class UpdateInvoiceStatusTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("updateInvoiceStatus should update status if invoice exists and belongs to user")
            void testUpdateInvoiceStatus_ValidData_Success() {
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
            void testUpdateInvoiceStatus_ValidStatusReceived_Success() {
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
            @DisplayName("updateInvoiceStatus should update stock quantity and set status to RECEIVED")
            void testUpdateInvoiceStatus_Received_Success() {
                UUID invoiceId = invoice1.getId();
                Invoice invoice = invoice1;
                UUID userId = user1.getId();
                invoice.setStatus(InvoiceStatus.PENDING);

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));
                when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Invoice updatedInvoice = invoiceService.updateInvoiceStatus(invoiceId, InvoiceStatus.RECEIVED, userId);

                assertNotNull(updatedInvoice);
                assertEquals(InvoiceStatus.RECEIVED, updatedInvoice.getStatus());

                verify(batchService, times(invoice.getBatch().size())).addStockQuantity(any(Batch.class), anyInt(), eq(user1));
                verify(invoiceRepository).save(any(Invoice.class));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("updateInvoiceStatus should throw ResourceNotFoundException if invoice does not exist")
            void testUpdateInvoiceStatus_InvalidInvoiceId_shouldThrowResourceNotFoundException() {
                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");
                InvoiceStatus newStatus = InvoiceStatus.CANCELED;
                UUID userId = user1.getId();

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

                ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                    invoiceService.updateInvoiceStatus(invoiceId, newStatus, userId)
                );
                assertEquals("Invoice not found", exception.getMessage());
            }


            @Test
            @DisplayName("updateInvoiceStatus should throw AccessDeniedException if invoice belongs to another user")
            void testUpdateInvoiceStatus_Unauthorized_shouldThrowAccessDeniedException() {
                UUID invoiceId = invoice1.getId();
                InvoiceStatus newStatus = InvoiceStatus.CANCELED;
                UUID userId = user2.getId();

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

                AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                    invoiceService.updateInvoiceStatus(invoiceId, newStatus, userId)
                );
                assertEquals("Unauthorized access to invoice", exception.getMessage());
            }

            @Test
            @DisplayName("updateInvoiceStatus should throw ConflictException if trying to change status of a received invoice")
            void testUpdateInvoiceStatus_ConflictReceived_shouldThrowConflictException() {
                UUID invoiceId = invoice2.getId();
                InvoiceStatus newStatus = InvoiceStatus.CANCELED;
                UUID userId = user2.getId();

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice2));

                ConflictException exception = assertThrows(ConflictException.class, () ->
                    invoiceService.updateInvoiceStatus(invoiceId, newStatus, userId)
                );
                assertEquals("Only pending invoices can be updated", exception.getMessage());
            }

            @Test
            @DisplayName("updateInvoiceStatus should throw ConflictException if trying to change status of a canceled invoice")
            void testUpdateInvoiceStatus_ConflictCanceled_shouldThrowConflictException() {
                UUID invoiceId = invoice1.getId();
                InvoiceStatus newStatus = InvoiceStatus.RECEIVED;
                invoice1.setStatus(InvoiceStatus.CANCELED);
                UUID userId = user1.getId();

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

                ConflictException exception = assertThrows(ConflictException.class, () ->
                    invoiceService.updateInvoiceStatus(invoiceId, newStatus, userId)
                );
                assertEquals("Only pending invoices can be updated", exception.getMessage());
            }
        }
    }

    // == deleteInvoice tests ==

    @Nested
    @DisplayName("deleteInvoice")
    class DeleteInvoiceTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("deleteInvoice should delete invoice if it exists and belongs to user")
            void testDeleteInvoice_ValidData_shouldDeleteInvoice() {
                UUID invoiceId = invoice1.getId();
                UUID userId = user1.getId();

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

                invoiceService.deleteInvoice(invoiceId, userId);

                verify(invoiceRepository).findById(invoiceId);
                verify(invoiceRepository).delete(invoice1);
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("deleteInvoice should throw ResourceNotFoundException if invoice does not exist")
            void testDeleteInvoice_InvalidId_shouldThrowResourceNotFoundException() {
                UUID invoiceId = invoice1.getId();
                UUID userId = user1.getId();

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

                ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                    invoiceService.deleteInvoice(invoiceId, userId)
                );
                assertEquals("Invoice not found", exception.getMessage());
            }

            @Test
            @DisplayName("deleteInvoice should throw AccessDeniedException if invoice belongs to another user")
            void testDeleteInvoice_InvoiceNotOwnedByUser_shouldThrowAccessDeniedException() {
                UUID invoiceId = invoice1.getId();
                UUID userId = user2.getId();

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

                AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                    invoiceService.deleteInvoice(invoiceId, userId)
                );
                assertEquals("Unauthorized access to invoice", exception.getMessage());
            }

            @Test
            @DisplayName("deleteInvoice should throw AccessDeniedException if invoice is not pending")
            void testDeleteInvoice_NotPending_shouldThrowAccessDeniedException() {
                UUID invoiceId = invoice1.getId();
                UUID userId = user1.getId();
                invoice1.setStatus(InvoiceStatus.RECEIVED);

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

                ConflictException exception = assertThrows(ConflictException.class, () ->
                    invoiceService.deleteInvoice(invoiceId, userId)
                );
                assertEquals("Only pending invoices can be deleted", exception.getMessage());
            }
        }
    }

    //== updateInvoice tests ==

    @Nested
    @DisplayName("updateInvoice")
    class UpdateInvoiceTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("updateInvoice should update invoice if it exists and belongs to user")
            void testUpdateInvoice_ValidData_ShouldUpdateInvoice() {
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
            void testUpdateInvoice_ValidAndNewInvoiceNumber_ShouldUpdateInvoice() {

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
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("updateInvoice should throw ResourceNotFoundException if invoice does not exist")
            void testUpdateInvoice_InvalidInvoiceId_ShouldThrowResourceNotFoundException() {
                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");
                UUID userId = user1.getId();
                InvoiceRequestUpdate request = new InvoiceRequestUpdate(
                    "INV-001-UPDATED",
                    "Supplier A",
                    InvoiceStatus.PENDING,
                    LocalDate.of(2026, 3, 1)
                );

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

                ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                    invoiceService.updateInvoice(invoiceId, request, userId)
                );
                assertEquals("Invoice not found", exception.getMessage());
            }

            @Test
            @DisplayName("updateInvoice should throw AccessDeniedException if invoice belongs to another user")
            void testUpdateInvoice_InvoiceNotOwnedByUser_ShouldThrowAccessDeniedException() {
                UUID invoiceId = invoice1.getId();
                UUID userId = user2.getId();
                InvoiceRequestUpdate request = new InvoiceRequestUpdate(
                    "INV-001-UPDATED",
                    "Supplier A",
                    InvoiceStatus.PENDING,
                    LocalDate.of(2026, 3, 1)
                );

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice1));

                AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                    invoiceService.updateInvoice(invoiceId, request, userId)
                );
                assertEquals("Unauthorized access to invoice", exception.getMessage());
            }

            @Test
            @DisplayName("updateInvoice should throw ConflictException if trying to update a received invoice")
            void testUpdateInvoice_ReceivedInvoice_ShouldThrowConflictException() {
                UUID invoiceId = invoice2.getId();
                UUID userId = user2.getId();
                InvoiceRequestUpdate request = new InvoiceRequestUpdate(
                    "INV-002-UPDATED",
                    "Supplier B",
                    InvoiceStatus.RECEIVED,
                    LocalDate.of(2026, 3, 1)
                );

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice2));

                ConflictException exception = assertThrows(ConflictException.class, () ->
                    invoiceService.updateInvoice(invoiceId, request, userId)
                );

                assertEquals("Only pending invoices can be updated", exception.getMessage());
            }

            @Test
            @DisplayName("updateInvoice should throw ConflictException if trying to update a canceled invoice")
            void testUpdateInvoice_CanceledInvoice_ShouldThrowConflictException() {
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

                ConflictException exception = assertThrows(ConflictException.class, () ->
                    invoiceService.updateInvoice(invoiceId, request, userId)
                );

                assertEquals("Only pending invoices can be updated", exception.getMessage());
            }

            @Test
            @DisplayName("updateInvoice should throw ConflictException if trying to update invoice number to an existing one")
            void testUpdateInvoice_DuplicateNumber_ShouldThrowConflictException() {
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

                ConflictException exception = assertThrows(ConflictException.class, () ->
                    invoiceService.updateInvoice(invoiceId, request, userId)
                );
                assertEquals("Invoice number: " + request.invoiceNumber() + " already exists", exception.getMessage());
            }
        }
    } 

    // == validateInvoiceSearchInputs tests ==

    @Nested
    @DisplayName("validateInvoiceSearchInputs")
    class ValidateInvoiceSearchInputsTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("validateInvoiceSearchInputs nothing should throw exception")
            void testValidateInvoiceSearchInputs_ValidInputs_shouldNotThrowException() {
                assertDoesNotThrow(() -> invoiceService.validateInvoiceSearchInputs(null, null, null, null));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("validateInvoiceSearchInputs should throw BadRequestException if minPrice is negative")
            void testValidateInvoiceSearchInputs_NegativeMinPrice_shouldThrowBadRequestException() {
                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.validateInvoiceSearchInputs(null, null, BigDecimal.valueOf(-1.0), null)
                );
                assertEquals("Min price cannot be negative", exception.getMessage());
            }

            @Test
            @DisplayName("validateInvoiceSearchInputs should throw BadRequestException if maxPrice is negative")
            void testValidateInvoiceSearchInputs_NegativeMaxPrice_shouldThrowBadRequestException() {
                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.validateInvoiceSearchInputs(null, null, null, BigDecimal.valueOf(-1.0))
                );
                assertEquals("Max price cannot be negative", exception.getMessage());
            }

            @Test
            @DisplayName("validateInvoiceSearchInputs should throw BadRequestException if minPrice is greater than maxPrice")
            void testValidateInvoiceSearchInputs_MinPriceGreaterThanMaxPrice_shouldThrowBadRequestException() {
                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.validateInvoiceSearchInputs(null, null, BigDecimal.valueOf(10.0), BigDecimal.valueOf(5.0))
                );
                assertEquals("Min price cannot be greater than max price", exception.getMessage());
            }

            @Test
            @DisplayName("validateInvoiceSearchInputs should throw BadRequestException if startDate is after endDate")
            void testValidateInvoiceSearchInputs_StartDateAfterEndDate_shouldThrowBadRequestException() {
                BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    invoiceService.validateInvoiceSearchInputs(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 1), null, null)
                );
                assertEquals("Start date cannot be after end date", exception.getMessage());
            }
        }
    }
}
