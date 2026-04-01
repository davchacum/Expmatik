package com.expmatik.backend.productInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.expmatik.backend.notification.NotificationService;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.productInfo.DTOs.ProductInfoUpdate;
import com.expmatik.backend.user.User;

@ExtendWith(MockitoExtension.class)
public class ProductInfoServiceTest {

    @Mock
    ProductInfoRepository productInfoRepository;

    @Mock
    ProductService productService;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    ProductInfoService productInfoService;

    private Product productCustom;
    private Product productNoCustom;
    private ProductInfo productInfo;
    private ProductInfo productInfo2;
    private User user1;
    private User user2;

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

        productInfo2 = new ProductInfo();
        productInfo2.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        productInfo2.setStockQuantity(10);
        productInfo2.setVatRate(new BigDecimal("0.10"));
        productInfo2.setSaleUnitPrice(new BigDecimal("5.99"));
        productInfo2.setLastPurchaseUnitPrice(null);
    }

    //Verificar Entidad ProductInfo getTotalStockValue, getLastPurchaseUnitPriceWithVat, getUnitProfit y getTotalProfit
    @Test
    void testProductInfoCalculations() {
        assertThat(productInfo.getTotalStockValue()).isEqualByComparingTo(new BigDecimal("59.9"));
        assertThat(productInfo.getLastPurchaseUnitPriceWithVat()).isEqualByComparingTo(new BigDecimal("5.489"));
        assertThat(productInfo.getUnitProfit()).isEqualByComparingTo(new BigDecimal("0.501"));
        assertThat(productInfo.getTotalProfit()).isEqualByComparingTo(new BigDecimal("5.01"));

        assertThat(productInfo2.getTotalStockValue()).isEqualByComparingTo(new BigDecimal("59.9"));
        assertThat(productInfo2.getLastPurchaseUnitPriceWithVat()).isNull();
        assertThat(productInfo2.getUnitProfit()).isNull();
        assertThat(productInfo2.getTotalProfit()).isNull();
    }

    // ==================== getOrCreateProductInfo Tests ====================

    @Test
    void testGetOrCreateProductInfo_CustomProduct_OwnedByUser() {
        UUID productId = productCustom.getId();
        UUID userId = user1.getId();
        BigDecimal unitPrice = new BigDecimal("5.00");
        productInfo.setProduct(productCustom);
        productInfo.setUser(user1);

        when(productService.findById(productId)).thenReturn(productCustom);
        when(productInfoRepository.findByProductIdAndUserId(productId, userId)).thenReturn(Optional.of(productInfo));

        ProductInfo result = productInfoService.getOrCreateProductInfo(productId, user1, unitPrice);

        assertThat(result).isEqualTo(productInfo);
    }

    @Test
    void testGetOrCreateProductInfo_CustomProduct_NotOwnedByUser() {
        UUID productId = productCustom.getId();
        BigDecimal unitPrice = new BigDecimal("5.00");

        when(productService.findById(productId)).thenReturn(productCustom);

        assertThatThrownBy(() -> productInfoService.getOrCreateProductInfo(productId, user2, unitPrice))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized to view this product info.");
    }

    @Test
    void testGetOrCreateProductInfo_CustomProduct_NotExistingProductInfo() {
        UUID productId = productCustom.getId();
        UUID userId = user1.getId();
        BigDecimal unitPrice = new BigDecimal("5.00");

        when(productService.findById(productId)).thenReturn(productCustom);
        when(productInfoRepository.findByProductIdAndUserId(productId, userId)).thenReturn(Optional.empty());
        when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductInfo result = productInfoService.getOrCreateProductInfo(productId, user1, unitPrice);
        BigDecimal expectedPrice = unitPrice.multiply(new BigDecimal("1.20")).setScale(2, RoundingMode.HALF_UP);
        assertThat(result).isNotNull();
        assertThat(result.getProduct()).isEqualTo(productCustom);
        assertThat(result.getUser()).isEqualTo(user1);
        assertThat(result.getSaleUnitPrice()).isEqualTo(expectedPrice.multiply(new BigDecimal("1.21")).setScale(2,RoundingMode.HALF_UP));
    }

    @Test
    void testGetOrCreateProductInfo_NonCustomProduct() {
        UUID productId = productNoCustom.getId();
        UUID userId = user1.getId();
        BigDecimal unitPrice = new BigDecimal("2.50");

        when(productService.findById(productId)).thenReturn(productNoCustom);
        when(productInfoRepository.findByProductIdAndUserId(productId, userId)).thenReturn(Optional.empty());
        when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductInfo result = productInfoService.getOrCreateProductInfo(productId, user1, unitPrice);
        BigDecimal expectedPrice = unitPrice.multiply(new BigDecimal("1.20")).setScale(2, RoundingMode.HALF_UP);

        assertThat(result).isNotNull();
        assertThat(result.getProduct()).isEqualTo(productNoCustom);
        assertThat(result.getUser()).isEqualTo(user1);
        assertThat(result.getStockQuantity()).isEqualTo(0);
        assertThat(result.getVatRate()).isEqualTo(new BigDecimal("0.21"));
        assertThat(result.getSaleUnitPrice())
            .isEqualTo(expectedPrice.multiply(new BigDecimal("1.21")).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void testGetOrCreateProductInfo_NonCustomProduct_UnitPriceNull() {
        UUID productId = productNoCustom.getId();
        UUID userId = user1.getId();

        when(productService.findById(productId)).thenReturn(productNoCustom);
        when(productInfoRepository.findByProductIdAndUserId(productId, userId)).thenReturn(Optional.empty());
        when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductInfo result = productInfoService.getOrCreateProductInfo(productId, user1, null);
            BigDecimal expectedPrice = BigDecimal.ONE.multiply(new BigDecimal("1.20")).setScale(2, RoundingMode.HALF_UP);


        assertThat(result).isNotNull();
        assertThat(result.getProduct()).isEqualTo(productNoCustom);
        assertThat(result.getUser()).isEqualTo(user1);
        assertThat(result.getStockQuantity()).isEqualTo(0);
        assertThat(result.getVatRate()).isEqualTo(new BigDecimal("0.21"));
        assertThat(result.getSaleUnitPrice())
            .isEqualTo(expectedPrice.multiply(new BigDecimal("1.21")).setScale(2, RoundingMode.HALF_UP));
    }

    // ==================== updateProductInfo Tests ====================

    @Test
    void testUpdateProductInfo_Success() {
        UUID productInfoId = productInfo.getId();
        productInfo.setProduct(productCustom);
        ProductInfoUpdate updatedInfo = new ProductInfoUpdate(20, new BigDecimal("6.99"), new BigDecimal("0.10"));
        productInfo.setUser(user1);
        when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
        when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProductInfo result = productInfoService.updateProductInfo(productInfoId, user1, updatedInfo);
        assertThat(result.getStockQuantity()).isEqualTo(20);
        assertThat(result.getSaleUnitPrice()).isEqualTo(new BigDecimal("6.99"));
        assertThat(result.getVatRate()).isEqualTo(new BigDecimal("0.10"));
    }

    @Test
    void testUpdateProductInfo_Unauthorized() {
        UUID productInfoId = productInfo.getId();
        ProductInfoUpdate updatedInfo = new ProductInfoUpdate(20, new BigDecimal("6.99"), new BigDecimal("0.10"));
        productInfo.setUser(user1);
        when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
        assertThatThrownBy(() -> productInfoService.updateProductInfo(productInfoId, user2, updatedInfo))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized to update this product info.");
    }

    // ==================== editStockQuantity Tests ====================

    @Test
    void testEditStockQuantity_Success() {
        UUID productInfoId = productInfo.getId();
        Integer newStockQuantity = 5;
        BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
        productInfo.setUser(user1);
        when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
        when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProductInfo result = productInfoService.editStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice);
        assertThat(result.getStockQuantity()).isEqualTo(15);
    }

    @Test
    void testEditStockQuantity_SuccessNullLastPurchaseUnitPrice() {
        UUID productInfoId = productInfo.getId();
        Integer newStockQuantity = 5;
        BigDecimal lastPurchaseUnitPrice = null;
        productInfo.setUser(user1);
        when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
        when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProductInfo result = productInfoService.editStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice);
        assertThat(result.getStockQuantity()).isEqualTo(15);
    }

    @Test
    void testEditStockQuantity_Unauthorized() {
        UUID productInfoId = productInfo.getId();
        Integer newStockQuantity = 5;
        BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
        productInfo.setUser(user1);
        when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
        assertThatThrownBy(() -> productInfoService.editStockQuantity(productInfoId, user2, newStockQuantity, lastPurchaseUnitPrice))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized to update this product info.");
    }

    @Test
    void testEditStockQuantity_negativeQuantity() {
        UUID productInfoId = productInfo.getId();
        Integer newStockQuantity = -5;
        BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
        productInfo.setUser(user1);
        when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
        when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProductInfo result = productInfoService.editStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice);
        assertThat(result.getStockQuantity()).isEqualTo(5);
    }

     @Test
    void testEditStockQuantity_nullQuantity() {
        UUID productInfoId = productInfo.getId();
        Integer newStockQuantity = -20;
        BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
        productInfo.setUser(user1);
        when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
        when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProductInfo result = productInfoService.editStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice);
        assertThat(result.getStockQuantity()).isEqualTo(0);
    }

}
