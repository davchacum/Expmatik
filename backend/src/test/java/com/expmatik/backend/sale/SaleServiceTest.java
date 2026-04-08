package com.expmatik.backend.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

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
import com.expmatik.backend.vendingMachine.VendingMachine;
import com.expmatik.backend.vendingSlot.VendingSlot;
import com.expmatik.backend.vendingSlot.VendingSlotService;

@ExtendWith(MockitoExtension.class)
public class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductService productService;

    @Mock
    private VendingSlotService vendingSlotService;

    @Mock
    private ProductInfoService productInfoService;

    @Mock
    private SaleCSVLector saleCSVLector;

    @Mock
    private NotificationService notificationService;

    @Spy
    @InjectMocks
    private SaleService saleService;

    private Sale sale;

    private User user;

    private VendingSlot vendingSlot;

    private Product product;

    private ProductInfo productInfo;

    private VendingMachine vendingMachine;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());

        product = new Product();
        product.setId(UUID.randomUUID());

        vendingMachine = new VendingMachine();
        vendingMachine.setId(UUID.randomUUID());
        vendingMachine.setUser(user);
        vendingMachine.setName("Máquina 1");

        vendingSlot = new VendingSlot();
        vendingSlot.setId(UUID.randomUUID());
        vendingSlot.setVendingMachine(vendingMachine);
        vendingSlot.setRowNumber(1);
        vendingSlot.setColumnNumber(1);
        vendingSlot.setProduct(product);

        productInfo = new ProductInfo();
        productInfo.setId(UUID.randomUUID());
        productInfo.setProduct(product);
        productInfo.setSaleUnitPrice(new BigDecimal("10.00"));
        productInfo.setUser(user);
        productInfo.setNeedUpdate(false);

        sale = new Sale();
        sale.setId(UUID.randomUUID());
        sale.setVendingSlot(vendingSlot);
    }

    // == Test findById ==

    @Nested
    @DisplayName("getSaleById")
    class GetSaleById {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("getSaleById - success")
            void testGetSaleById_ValidId_ReturnsSale() {

                when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));

                Sale result = saleService.getSaleById(sale.getId(), user);

                assertEquals(result, sale);
                verify(saleRepository).findById(sale.getId());
                
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("getSaleById - not found")
            void testGetSaleById_InvalidId_ThrowsResourceNotFoundException() {
                UUID invalidId = UUID.randomUUID();

                when(saleRepository.findById(invalidId)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> {
                    saleService.getSaleById(invalidId, user);
                });

                verify(saleRepository).findById(invalidId);
            }

            @Test
            @DisplayName("getSaleById - access denied")
            void testGetSaleById_AccessDenied_ThrowsAccessDeniedException() {
                User otherUser = new User();
                otherUser.setId(UUID.randomUUID());

                VendingMachine otherVendingMachine = new VendingMachine();
                otherVendingMachine.setId(UUID.randomUUID());
                otherVendingMachine.setUser(otherUser);

                VendingSlot otherVendingSlot = new VendingSlot();
                otherVendingSlot.setId(UUID.randomUUID());
                otherVendingSlot.setVendingMachine(otherVendingMachine);

                Sale otherSale = new Sale();
                otherSale.setId(UUID.randomUUID());
                otherSale.setVendingSlot(otherVendingSlot);

                when(saleRepository.findById(otherSale.getId())).thenReturn(Optional.of(otherSale));

                assertThrows(AccessDeniedException.class, () -> {
                    saleService.getSaleById(otherSale.getId(), user);
                });

                verify(saleRepository).findById(otherSale.getId());
            }
        }
    }

    // == Test createSale ==

    @Nested
    @DisplayName("createSale")
    class CreateSale {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("createSale - success")
            void testCreateSale_ValidInput_ReturnsCreatedSale() {
                String barcode = "1234567890123";
                SaleCreate saleCreate = new SaleCreate(
                    LocalDateTime.now(),
                    new BigDecimal("10.00"),
                    PaymentMethod.CASH,
                    TransactionStatus.SUCCESS,
                    barcode,
                    "Máquina 1",
                    1,
                    1
                );

                when(productService.findInternalProductByBarcode(barcode, user.getId())).thenReturn(product);
                when(vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(saleCreate.machineName(), saleCreate.rowNumber(), saleCreate.columnNumber(), user)).thenReturn(vendingSlot);
                when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Sale result = saleService.createSale(saleCreate, user);

                assertEquals(result.getSaleDate(), saleCreate.saleDate());
                assertEquals(result.getTotalAmount(), saleCreate.totalAmount());
                assertEquals(result.getPaymentMethod(), saleCreate.paymentMethod());
                assertEquals(result.getStatus(), saleCreate.status());
                assertEquals(result.getProduct(), product);
                assertEquals(result.getVendingSlot(), vendingSlot);
                verify(productService).findInternalProductByBarcode(barcode, user.getId());
                verify(vendingSlotService).getVendingSlotByMachineNameAndRowAndColumn(saleCreate.machineName(), saleCreate.rowNumber(), saleCreate.columnNumber(), user);
                verify(saleRepository).save(any(Sale.class));
            }
        }
    }
    

    // == Test realTimeSale ==

    @Nested
    @DisplayName("realTimeSale")
    class RealTimeSale {

        @Nested
        @DisplayName("Success SaleCases")
        class SuccessCases {
        
            @Test
            @DisplayName("realTimeSale - success")
            void testRealTimeSale_ValidInput_ReturnsSuccessfulSale() {
                UUID vendingSlotId = vendingSlot.getId();
                PaymentMethod paymentMethod = PaymentMethod.CASH;


                when(vendingSlotService.getVendingSlotById(vendingSlotId, user)).thenReturn(vendingSlot);
                when(productInfoService.getOrCreateProductInfo(product.getId(), user,null)).thenReturn(productInfo);
                when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Sale result = saleService.realTimeSale(vendingSlotId, paymentMethod, user);
                assertEquals(result.getVendingSlot(), vendingSlot);
                assertEquals(result.getProduct(), product);
                assertEquals(result.getTotalAmount(), productInfo.getSaleUnitPrice());
                assertEquals(result.getPaymentMethod(), paymentMethod);
                assertEquals(result.getStatus(), TransactionStatus.SUCCESS);
                verify(vendingSlotService).getVendingSlotById(vendingSlotId, user);
                verify(saleRepository).save(any(Sale.class));
            }
        
        }

        @Nested
        @DisplayName("Failed Sale Cases")
        class FailedSaleCases {

            @Test
            @DisplayName("realTimeSale - out of stock registers failed sale and creates notification")
            void testRealTimeSale_PopStockThrowsOutOfStockException_RegistersFailedSale() {
                UUID vendingSlotId = vendingSlot.getId();
                PaymentMethod paymentMethod = PaymentMethod.CASH;
                String errorMessage = "No hay stock disponible";

                when(vendingSlotService.getVendingSlotById(vendingSlotId, user)).thenReturn(vendingSlot);
                when(productInfoService.getOrCreateProductInfo(product.getId(), user, null)).thenReturn(productInfo);
                doThrow(new OutOfStockException(errorMessage)).when(vendingSlotService).popStockFromVendingSlotForSale(vendingSlotId, user);
                when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Sale result = saleService.realTimeSale(vendingSlotId, paymentMethod, user);

                assertEquals(result.getStatus(), TransactionStatus.FAILED);
                assertEquals(result.getFailureReason(), errorMessage);
                assertEquals(result.getVendingSlot(), vendingSlot);
                assertEquals(result.getProduct(), product);
                assertEquals(result.getTotalAmount(), productInfo.getSaleUnitPrice());

                verify(vendingSlotService).popStockFromVendingSlotForSale(vendingSlotId, user);
                verify(notificationService).createNotification(
                    eq(NotificationType.FAILURE_SALE),
                    argThat(message -> message.contains("ranura A1") && message.contains(errorMessage)),
                    eq("Unknown"),
                    eq(user)
                );
                verify(saleRepository).save(any(Sale.class));
            }

            @Test
            @DisplayName("realTimeSale - SlotBlockedException registers failed sale and creates notification")
            void testRealTimeSale_PopStockThrowsSlotBlockedException_RegistersFailedSale() {
                UUID vendingSlotId = vendingSlot.getId();
                PaymentMethod paymentMethod = PaymentMethod.CASH;
                String errorMessage = "La ranura está bloqueada por mantenimiento";

                when(vendingSlotService.getVendingSlotById(vendingSlotId, user)).thenReturn(vendingSlot);
                when(productInfoService.getOrCreateProductInfo(product.getId(), user, null)).thenReturn(productInfo);
                doThrow(new SlotBlockedException(errorMessage)).when(vendingSlotService).popStockFromVendingSlotForSale(vendingSlotId, user);
                when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Sale result = saleService.realTimeSale(vendingSlotId, paymentMethod, user);
                assertEquals(result.getStatus(), TransactionStatus.FAILED);
                assertEquals(result.getFailureReason(), errorMessage);
                assertEquals(result.getVendingSlot(), vendingSlot);
                assertEquals(result.getProduct(), product);
                assertEquals(result.getTotalAmount(), productInfo.getSaleUnitPrice());
                verify(vendingSlotService).popStockFromVendingSlotForSale(vendingSlotId, user);
                verify(notificationService).createNotification(
                    eq(NotificationType.FAILURE_SALE),
                    argThat(message -> message.contains("ranura A1") && message.contains(errorMessage)),
                    eq("Unknown"),
                    eq(user)
                );
                verify(saleRepository).save(any(Sale.class));
            }


            @Test
            @DisplayName("realTimeSale - ExpiredProductException registers failed sale and creates notification")
            void testRealTimeSale_PopStockThrowsExpiredProductException_RegistersFailedSale() {
                UUID vendingSlotId = vendingSlot.getId();
                PaymentMethod paymentMethod = PaymentMethod.CASH;
                String errorMessage = "El producto está caducado";

                when(vendingSlotService.getVendingSlotById(vendingSlotId, user)).thenReturn(vendingSlot);
                when(productInfoService.getOrCreateProductInfo(product.getId(), user, null)).thenReturn(productInfo);
                doThrow(new ExpiredProductException(errorMessage)).when(vendingSlotService).popStockFromVendingSlotForSale(vendingSlotId, user);
                when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Sale result = saleService.realTimeSale(vendingSlotId, paymentMethod, user);

                assertEquals(result.getStatus(), TransactionStatus.FAILED);
                assertEquals(result.getFailureReason(), errorMessage);
                assertEquals(result.getVendingSlot(), vendingSlot);
                assertEquals(result.getProduct(), product);
                assertEquals(result.getTotalAmount(), productInfo.getSaleUnitPrice());

                verify(vendingSlotService).popStockFromVendingSlotForSale(vendingSlotId, user);
                verify(notificationService).createNotification(
                    eq(NotificationType.FAILURE_SALE),
                    argThat(message -> message.contains("ranura A1") && message.contains(errorMessage)),
                    eq("Unknown"),
                    eq(user)
                );
                verify(saleRepository).save(any(Sale.class));
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("realTimeSale - vending slot not found")
            void testRealTimeSale_VendingSlotNotFound_ThrowsResourceNotFoundException() {
                UUID vendingSlotId = UUID.randomUUID();
                PaymentMethod paymentMethod = PaymentMethod.CASH;

                when(vendingSlotService.getVendingSlotById(vendingSlotId, user)).thenThrow(new ResourceNotFoundException("The vending slot does not exist."));

                assertThrows(ResourceNotFoundException.class, () -> {
                    saleService.realTimeSale(vendingSlotId, paymentMethod, user);
                });

                verify(vendingSlotService).getVendingSlotById(vendingSlotId, user);
            }

            @Test
            @DisplayName("realTimeSale - access denied")
            void testRealTimeSale_AccessDenied_ThrowsAccessDeniedException() {
                UUID vendingSlotId = vendingSlot.getId();
                PaymentMethod paymentMethod = PaymentMethod.CASH;

                when(vendingSlotService.getVendingSlotById(vendingSlotId, user)).thenThrow(new AccessDeniedException("You are not authorized to perform this action."));

                assertThrows(AccessDeniedException.class, () -> {
                    saleService.realTimeSale(vendingSlotId, paymentMethod, user);
                });

                verify(vendingSlotService).getVendingSlotById(vendingSlotId, user);
            }

            @Test
            @DisplayName("realTimeSale - product info need update")
            void testRealTimeSale_ProductInfoNeedUpdate_ThrowsConflictException() {
                UUID vendingSlotId = vendingSlot.getId();
                PaymentMethod paymentMethod = PaymentMethod.CASH;
                productInfo.setNeedUpdate(true);

                when(vendingSlotService.getVendingSlotById(vendingSlotId, user)).thenReturn(vendingSlot);
                when(productInfoService.getOrCreateProductInfo(product.getId(), user,null)).thenReturn(productInfo);
                assertThrows(ConflictException.class, () -> {
                    saleService.realTimeSale(vendingSlotId, paymentMethod, user);
                });

                verify(vendingSlotService).getVendingSlotById(vendingSlotId, user);
                verify(productInfoService).getOrCreateProductInfo(product.getId(), user, null);
            }
        }
    }   

    // == Test searchSales ==

    @Nested
    @DisplayName("searchSales")
    class SearchSales {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("searchSales - all parameters provided should call repository with same values")
            void testSearchSales_AllParams_ReturnsExpectedPage() {
                UUID userId = UUID.randomUUID();
                String barcode = "123456";
                String machineName = "Máquina 1";
                Integer rowNumber = 1;
                Integer columnNumber = 1;
                LocalDateTime startDate = LocalDateTime.now().minusDays(1);
                LocalDateTime endDate = LocalDateTime.now();
                PaymentMethod paymentMethod = PaymentMethod.CASH;
                TransactionStatus status = TransactionStatus.SUCCESS;
                Pageable pageable = PageRequest.of(0, 10);

                Page<Sale> expectedPage = Page.empty();

                when(saleRepository.searchAdvanced(
                        userId, barcode, machineName, rowNumber, columnNumber,
                        startDate, endDate,
                        paymentMethod, status,
                        pageable
                )).thenReturn(expectedPage);

                Page<Sale> result = saleService.searchSales(
                        userId, barcode, machineName, rowNumber, columnNumber,
                        startDate, endDate,
                        paymentMethod, status,
                        pageable
                );

                assertEquals(result, expectedPage);

                verify(saleRepository).searchAdvanced(
                        userId, barcode, machineName, rowNumber, columnNumber,
                        startDate, endDate,
                        paymentMethod, status,
                        pageable
                );
            }

            @Test
            @DisplayName("searchSales - blank barcode should be converted to null")
            void testSearchSales_BlankBarcode_ShouldPassNull() {
                UUID userId = UUID.randomUUID();
                String barcode = "   ";
                Pageable pageable = PageRequest.of(0, 10);

                when(saleRepository.searchAdvanced(
                        eq(userId), isNull(), isNull(), isNull(), isNull(),
                        isNull(), isNull(),
                        isNull(), isNull(),
                        eq(pageable)
                )).thenReturn(Page.empty());

                saleService.searchSales(
                        userId, barcode, null, null,null,
                        null, null,
                        null, null,
                        pageable
                );

                verify(saleRepository).searchAdvanced(
                        eq(userId), isNull(), isNull(), isNull(), isNull(),
                        isNull(), isNull(),
                        isNull(), isNull(),
                        eq(pageable)
                );
            }

            @Test
            @DisplayName("searchSales - null barcode should remain null")
            void testSearchSales_NullBarcode_ReturnsExpectedPage() {
                UUID userId = UUID.randomUUID();
                Pageable pageable = PageRequest.of(0, 10);

                when(saleRepository.searchAdvanced(
                        eq(userId), isNull(), any(), any(),any(),
                        any(), any(),
                        any(), any(),
                        eq(pageable)
                )).thenReturn(Page.empty());

                saleService.searchSales(
                        userId, null, null, null,null,
                        null, null,
                        null, null,
                        pageable
                );

                verify(saleRepository).searchAdvanced(
                        eq(userId), isNull(), isNull(), isNull(),isNull(),
                        isNull(), isNull(),
                        isNull(), isNull(),
                        eq(pageable)
                );
            }

            @Test
            @DisplayName("searchSales - blank machine name should be converted to null")
            void testSearchSales_BlankMachineName_ShouldPassNull() {
                UUID userId = UUID.randomUUID();
                String machineName = "   ";
                Pageable pageable = PageRequest.of(0, 10);

                when(saleRepository.searchAdvanced(
                        eq(userId), isNull(), isNull(), isNull(), isNull(),
                        isNull(), isNull(),
                        isNull(), isNull(),
                        eq(pageable)
                )).thenReturn(Page.empty());

                saleService.searchSales(
                        userId, null, machineName, null,null,
                        null, null,
                        null, null,
                        pageable
                );

                verify(saleRepository).searchAdvanced(
                        eq(userId), isNull(), isNull(), isNull(), isNull(),
                        isNull(), isNull(),
                        isNull(), isNull(),
                        eq(pageable)
                );
            }

            @Test
            @DisplayName("searchSales - null machine name should remain null")
            void testSearchSales_NullMachineName_ReturnsExpectedPage() {
                UUID userId = UUID.randomUUID();
                Pageable pageable = PageRequest.of(0, 10);

                when(saleRepository.searchAdvanced(
                        eq(userId), any(), isNull(), any(),any(),
                        any(), any(),
                        any(), any(),
                        eq(pageable)
                )).thenReturn(Page.empty());

                saleService.searchSales(
                        userId, null, null, null,null,
                        null, null,
                        null, null,
                        pageable
                );

                verify(saleRepository).searchAdvanced(
                        eq(userId), isNull(), isNull(), isNull(),isNull(),
                        isNull(), isNull(),
                        isNull(), isNull(),
                        eq(pageable)
                );
            }

            @Test
            @DisplayName("searchSales - partial filters should pass only provided values")
            void testSearchSales_PartialFilters_ReturnsExpectedPage() {
                UUID userId = UUID.randomUUID();
                String barcode = "ABC123";
                Pageable pageable = PageRequest.of(0, 10);

                when(saleRepository.searchAdvanced(
                        eq(userId), eq(barcode), isNull(), isNull(),isNull(),
                        isNull(), isNull(),
                        isNull(), isNull(),
                        eq(pageable)
                )).thenReturn(Page.empty());

                saleService.searchSales(
                        userId, barcode, null, null,null,
                        null, null,
                        null, null,
                        pageable
                );

                verify(saleRepository).searchAdvanced(
                        eq(userId), eq(barcode), isNull(), isNull(),isNull(),
                        isNull(), isNull(),
                        isNull(), isNull(),
                        eq(pageable)
                );
            }
        }
    }

    // == Test readSalesFromCSV ==

    @Nested
    @DisplayName("readSalesFromCSV")
    class ReadSalesFromCSV {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("readSalesFromCSV - valid CSV content should return list of SaleCreate")
            void testReadSalesFromCSV_ValidContent_ReturnsSaleCreateList() {

                String csvContent = "saleDate,totalAmount,paymentMethod,status,barcode,vendingMachineName,rowNumber,columnNumber\n" +
                        "2024-03-01T10:00:00,2.50,CREDIT_CARD,SUCCESS,20000001,Máquina 1,1,1\n" +
                        "2024-03-02T11:30:00,5.00,CASH,SUCCESS,20000002,Máquina 1,1,2";

                SaleCreate saleCreate1 = new SaleCreate(
                    LocalDateTime.parse("2024-03-01T10:00:00"),
                    new BigDecimal("2.50"),
                    PaymentMethod.CREDIT_CARD,
                    TransactionStatus.SUCCESS,
                    "20000001",
                    "Máquina 1",
                    1,
                    1
                );

                SaleCreate saleCreate2 = new SaleCreate(
                    LocalDateTime.parse("2024-03-02T11:30:00"),
                    new BigDecimal("5.00"),
                    PaymentMethod.CASH,
                    TransactionStatus.SUCCESS,
                    "20000002",
                    "Máquina 1",
                    1,
                    2
                );

                MockMultipartFile csvFile = new MockMultipartFile(
                    "csv",
                    "sales.csv",
                    "text/csv",
                    csvContent.getBytes()
                );

                when(saleCSVLector.readCSV(any(File.class))).thenReturn(List.of(saleCreate1, saleCreate2));

                List<SaleCreate> result = saleService.readSalesFromCSV(csvFile);

                assertEquals(result, List.of(saleCreate1, saleCreate2));
                verify(saleCSVLector).readCSV(any(File.class));
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("readSalesFromCSV - empty file should throw BadRequestException")
            void testReadSalesFromCSV_EmptyFile_ThrowsBadRequestException() {
                MockMultipartFile emptyFile = new MockMultipartFile(
                    "csv",
                    "empty.csv",
                    "text/csv",
                    new byte[0]
                );

                assertThrows(BadRequestException.class, () -> {
                    saleService.readSalesFromCSV(emptyFile);
                });
            }

            @Test
            @DisplayName("readSalesFromCSV - null file should throw BadRequestException")
            void testReadSalesFromCSV_NullFile_ThrowsBadRequestException() {
                assertThrows(BadRequestException.class, () -> {
                    saleService.readSalesFromCSV(null);
                });
            }

            @Test
            @DisplayName("readSalesFromCSV - file with non-csv extension should throw BadRequestException")
            void testReadSalesFromCSV_NonCSVFile_ThrowsBadRequestException() {
                MockMultipartFile nonCSVFile = new MockMultipartFile(
                    "file",
                    "sales.txt",
                    "text/plain",
                    "Some content".getBytes()
                );

                assertThrows(BadRequestException.class, () -> {
                    saleService.readSalesFromCSV(nonCSVFile);
                });
            }
        }
    }

    // == Test exportSalesCSV ==

    @Nested
    @DisplayName("exportSalesCSV")
    class ExportSalesCSV {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("exportSalesCSV - valid filters should call repository and CSV generator")
            void testExportSalesCSV_ValidFilters_ReturnsCSVData() {
                UUID userId = UUID.randomUUID();
                String barcode = "123456";
                String machineName = "Máquina 1";
                Integer rowNumber = 1;
                Integer columnNumber = 1;
                LocalDateTime startDate = LocalDateTime.now().minusDays(1);
                LocalDateTime endDate = LocalDateTime.now();
                PaymentMethod paymentMethod = PaymentMethod.CASH;
                TransactionStatus status = TransactionStatus.SUCCESS;

                sale.setSaleDate(LocalDateTime.parse("2024-03-01T10:00:00"));
                sale.setTotalAmount(new BigDecimal("2.50"));
                sale.setPaymentMethod(PaymentMethod.CREDIT_CARD);
                sale.setStatus(TransactionStatus.SUCCESS);
                product.setBarcode("123456");
                sale.setProduct(product);

                List<Sale> sales = List.of(sale);

                when(saleRepository.searchAdvanced(
                        userId, barcode, machineName, rowNumber, columnNumber,
                        startDate, endDate,
                        paymentMethod, status
                )).thenReturn(sales);
                when(saleCSVLector.generateCSV(sales)).thenCallRealMethod();
                byte[] result = saleService.exportSalesCSV(userId, barcode, machineName, rowNumber, columnNumber, startDate, endDate, paymentMethod, status);
                String csv = new String(result, StandardCharsets.UTF_8);

                assertEquals(result.length, 255);
                assertTrue(csv.contains("\"saleId\",\"saleDate\",\"totalAmount\",\"paymentMethod\",\"status\",\"barcode\",\"vendingMachineName\",\"rowNumber\",\"columnNumber\",\"failureReason\""),
                "CSV header is missing or incorrect");
                assertTrue(csv.contains("\"2024-03-01T10:00\",\"2.50\",\"CREDIT_CARD\",\"SUCCESS\",\"123456\",\"Máquina 1\",\"1\",\"1\",\"\""),
                "CSV row is missing or incorrect");     
                verify(saleRepository).searchAdvanced(
                        userId, barcode, machineName, rowNumber, columnNumber,
                        startDate, endDate,
                        paymentMethod, status
                );
                verify(saleCSVLector).generateCSV(sales);
            }

            @Test
            @DisplayName("exportSalesCSV - blank filters should be treated as null")
            void testExportSalesCSV_BlankFilters_TreatedAsNull() {
                UUID userId = UUID.randomUUID();
                String barcode = "   ";
                String machineName = "   ";
                Integer rowNumber = null;
                Integer columnNumber = null;
                LocalDateTime startDate = null;
                LocalDateTime endDate = null;
                PaymentMethod paymentMethod = null;
                TransactionStatus status = null;

                when(saleRepository.searchAdvanced(
                        eq(userId), isNull(), isNull(), isNull(), isNull(),
                        isNull(), isNull(),
                        isNull(), isNull()
                )).thenReturn(List.of());

                saleService.exportSalesCSV(userId, barcode, machineName, rowNumber, columnNumber, startDate, endDate, paymentMethod, status);

                verify(saleRepository).searchAdvanced(
                        eq(userId), isNull(), isNull(), isNull(), isNull(),
                        isNull(), isNull(),
                        isNull(), isNull()
                );
            }

            @Test
            @DisplayName("exportSalesCSV - specific filters should be passed to repository")
            void testExportSalesCSV_SpecificFilters_PassedToRepository() {
                UUID userId = UUID.randomUUID();
                String barcode = "ABC123";
                String machineName = "Máquina 1";
                Integer rowNumber = null;
                Integer columnNumber = null;
                LocalDateTime startDate = null;
                LocalDateTime endDate = null;
                PaymentMethod paymentMethod = null;
                TransactionStatus status = null;

                when(saleRepository.searchAdvanced(
                        eq(userId), eq(barcode), eq(machineName), isNull(), isNull(),
                        isNull(), isNull(),
                        isNull(), isNull()
                )).thenReturn(List.of());

                saleService.exportSalesCSV(userId, barcode, machineName, rowNumber, columnNumber, startDate, endDate, paymentMethod, status);

                verify(saleRepository).searchAdvanced(
                        eq(userId), eq(barcode), eq(machineName), isNull(), isNull(),
                        isNull(), isNull(),
                        isNull(), isNull()
                );
            }
        }
    }
}
