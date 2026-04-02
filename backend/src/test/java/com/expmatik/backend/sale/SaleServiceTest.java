package com.expmatik.backend.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.expmatik.backend.exceptions.ConflictException;
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

        vendingSlot = new VendingSlot();
        vendingSlot.setId(UUID.randomUUID());
        vendingSlot.setVendingMachine(vendingMachine);
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

    @Test
    @DisplayName("getSaleById - success")
    void testGetSaleById_ValidId_ReturnsSale() {

        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));

        Sale result = saleService.getSaleById(sale.getId(), user);

        assertThat(result).isEqualTo(sale);
        verify(saleRepository).findById(sale.getId());
        
    }

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

    // == Test createSale ==

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
            vendingSlot.getId()
        );

        when(productService.findInternalProductByBarcode(barcode, user.getId())).thenReturn(product);
        when(vendingSlotService.getVendingSlotById(vendingSlot.getId(), user)).thenReturn(vendingSlot);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sale result = saleService.createSale(saleCreate, user);

        assertThat(result.getSaleDate()).isEqualTo(saleCreate.saleDate());
        assertThat(result.getTotalAmount()).isEqualTo(saleCreate.totalAmount());
        assertThat(result.getPaymentMethod()).isEqualTo(saleCreate.paymentMethod());
        assertThat(result.getStatus()).isEqualTo(saleCreate.status());
        assertThat(result.getProduct()).isEqualTo(product);
        assertThat(result.getVendingSlot()).isEqualTo(vendingSlot);
        verify(productService).findInternalProductByBarcode(barcode, user.getId());
        verify(vendingSlotService).getVendingSlotById(vendingSlot.getId(), user);
        verify(saleRepository).save(any(Sale.class));
    }

    // == Test realTimeSale ==

    @Test
    @DisplayName("realTimeSale - success")
    void testRealTimeSale_ValidInput_ReturnsSuccessfulSale() {
        UUID vendingSlotId = vendingSlot.getId();
        PaymentMethod paymentMethod = PaymentMethod.CASH;


        when(vendingSlotService.getVendingSlotById(vendingSlotId, user)).thenReturn(vendingSlot);
        when(productInfoService.getOrCreateProductInfo(product.getId(), user,null)).thenReturn(productInfo);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sale result = saleService.realTimeSale(vendingSlotId, paymentMethod, user);
        assertThat(result.getVendingSlot()).isEqualTo(vendingSlot);
        assertThat(result.getProduct()).isEqualTo(product);
        assertThat(result.getTotalAmount()).isEqualTo(productInfo.getSaleUnitPrice());
        assertThat(result.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        verify(vendingSlotService).getVendingSlotById(vendingSlotId, user);
        verify(saleRepository).save(any(Sale.class));
    }

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

    @Test
    @DisplayName("realTimeSale - out of stock registers failed sale and creates notification")
    void testRealTimeSale_PopStockThrowsOutOfStockException_RegistersFailedSale() {
        UUID vendingSlotId = vendingSlot.getId();
        PaymentMethod paymentMethod = PaymentMethod.CASH;
        String errorMessage = "No hay stock disponible";

        when(vendingSlotService.getVendingSlotById(vendingSlotId, user)).thenReturn(vendingSlot);
        when(productInfoService.getOrCreateProductInfo(product.getId(), user, null)).thenReturn(productInfo);
        doThrow(new OutOfStockException(errorMessage)).when(vendingSlotService).popStockFromVendingSlot(vendingSlotId, user);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sale result = saleService.realTimeSale(vendingSlotId, paymentMethod, user);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo(errorMessage);
        assertThat(result.getVendingSlot()).isEqualTo(vendingSlot);
        assertThat(result.getProduct()).isEqualTo(product);
        assertThat(result.getTotalAmount()).isEqualTo(productInfo.getSaleUnitPrice());

        verify(vendingSlotService).popStockFromVendingSlot(vendingSlotId, user);
        verify(notificationService).createNotification(
            eq(NotificationType.SALE_FAILURE),
            contains(errorMessage),
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
        doThrow(new SlotBlockedException(errorMessage)).when(vendingSlotService).popStockFromVendingSlot(vendingSlotId, user);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sale result = saleService.realTimeSale(vendingSlotId, paymentMethod, user);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo(errorMessage);
        assertThat(result.getVendingSlot()).isEqualTo(vendingSlot);
        assertThat(result.getProduct()).isEqualTo(product);
        assertThat(result.getTotalAmount()).isEqualTo(productInfo.getSaleUnitPrice());
        verify(vendingSlotService).popStockFromVendingSlot(vendingSlotId, user);
        verify(notificationService).createNotification(
            eq(NotificationType.SALE_FAILURE),
            contains(errorMessage),
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
        doThrow(new OutOfStockException(errorMessage)).when(vendingSlotService).popStockFromVendingSlot(vendingSlotId, user);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sale result = saleService.realTimeSale(vendingSlotId, paymentMethod, user);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo(errorMessage);
        assertThat(result.getVendingSlot()).isEqualTo(vendingSlot);
        assertThat(result.getProduct()).isEqualTo(product);
        assertThat(result.getTotalAmount()).isEqualTo(productInfo.getSaleUnitPrice());

        verify(vendingSlotService).popStockFromVendingSlot(vendingSlotId, user);
        verify(notificationService).createNotification(
            eq(NotificationType.SALE_FAILURE),
            contains(errorMessage),
            eq("Unknown"),
            eq(user)
        );
        verify(saleRepository).save(any(Sale.class));
    }

    // == Test searchSales ==

    @Test
    @DisplayName("searchSales - all parameters provided should call repository with same values")
    void testSearchSales_AllParams() {
        UUID userId = UUID.randomUUID();
        String barcode = "123456";
        UUID machineId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();
        PaymentMethod paymentMethod = PaymentMethod.CASH;
        TransactionStatus status = TransactionStatus.SUCCESS;
        Pageable pageable = PageRequest.of(0, 10);

        Page<Sale> expectedPage = Page.empty();

        when(saleRepository.searchAdvanced(
                userId, barcode, machineId, slotId,
                startDate, endDate,
                paymentMethod, status,
                pageable
        )).thenReturn(expectedPage);

        Page<Sale> result = saleService.searchSales(
                userId, barcode, machineId, slotId,
                startDate, endDate,
                paymentMethod, status,
                pageable
        );

        assertThat(result).isEqualTo(expectedPage);

        verify(saleRepository).searchAdvanced(
                userId, barcode, machineId, slotId,
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
                eq(userId), isNull(), isNull(), isNull(),
                isNull(), isNull(),
                isNull(), isNull(),
                eq(pageable)
        )).thenReturn(Page.empty());

        saleService.searchSales(
                userId, barcode, null, null,
                null, null,
                null, null,
                pageable
        );

        verify(saleRepository).searchAdvanced(
                eq(userId), isNull(), isNull(), isNull(),
                isNull(), isNull(),
                isNull(), isNull(),
                eq(pageable)
        );
    }

    @Test
    @DisplayName("searchSales - null barcode should remain null")
    void testSearchSales_NullBarcode() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(saleRepository.searchAdvanced(
                eq(userId), isNull(), any(), any(),
                any(), any(),
                any(), any(),
                eq(pageable)
        )).thenReturn(Page.empty());

        saleService.searchSales(
                userId, null, null, null,
                null, null,
                null, null,
                pageable
        );

        verify(saleRepository).searchAdvanced(
                eq(userId), isNull(), isNull(), isNull(),
                isNull(), isNull(),
                isNull(), isNull(),
                eq(pageable)
        );
    }

    @Test
    @DisplayName("searchSales - partial filters should pass only provided values")
    void testSearchSales_PartialFilters() {
        UUID userId = UUID.randomUUID();
        String barcode = "ABC123";
        Pageable pageable = PageRequest.of(0, 10);

        when(saleRepository.searchAdvanced(
                eq(userId), eq(barcode), isNull(), isNull(),
                isNull(), isNull(),
                isNull(), isNull(),
                eq(pageable)
        )).thenReturn(Page.empty());

        saleService.searchSales(
                userId, barcode, null, null,
                null, null,
                null, null,
                pageable
        );

        verify(saleRepository).searchAdvanced(
                eq(userId), eq(barcode), isNull(), isNull(),
                isNull(), isNull(),
                isNull(), isNull(),
                eq(pageable)
        );
    }

}
