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

import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.UnauthorizedActionException;
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

    @InjectMocks
    ProductInfoService productInfoService;

    private Product productCustom;
    private Product productNoCustom;
    private ProductInfo productInfo;
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
                .isInstanceOf(UnauthorizedActionException.class)
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

        assertThat(result).isNotNull();
        assertThat(result.getProduct()).isEqualTo(productCustom);
        assertThat(result.getUser()).isEqualTo(user1);
        assertThat(result.getSaleUnitPrice()).isEqualTo(unitPrice.multiply(new BigDecimal("1.21")).setScale(2,RoundingMode.CEILING));
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

        assertThat(result).isNotNull();
        assertThat(result.getProduct()).isEqualTo(productNoCustom);
        assertThat(result.getUser()).isEqualTo(user1);
        assertThat(result.getStockQuantity()).isEqualTo(0);
        assertThat(result.getVatRate()).isEqualTo(new BigDecimal("0.21"));
        assertThat(result.getSaleUnitPrice())
            .isEqualTo(unitPrice.multiply(new BigDecimal("1.21")).setScale(2, RoundingMode.CEILING));
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

        assertThat(result).isNotNull();
        assertThat(result.getProduct()).isEqualTo(productNoCustom);
        assertThat(result.getUser()).isEqualTo(user1);
        assertThat(result.getStockQuantity()).isEqualTo(0);
        assertThat(result.getVatRate()).isEqualTo(new BigDecimal("0.21"));
        assertThat(result.getSaleUnitPrice())
            .isEqualTo(BigDecimal.ONE.multiply(new BigDecimal("1.21")).setScale(2, RoundingMode.CEILING));
    }

    // ==================== updateProductInfo Tests ====================

    @Test
    void testUpdateProductInfo_Success() {
        UUID productInfoId = productInfo.getId();
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
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("You are not authorized to update this product info.");
    }

    // ==================== addStockQuantity Tests ====================

    @Test
    void testAddStockQuantity_Success() {
        UUID productInfoId = productInfo.getId();
        Integer newStockQuantity = 5;
        BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
        productInfo.setUser(user1);
        when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
        when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProductInfo result = productInfoService.addStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice);
        assertThat(result.getStockQuantity()).isEqualTo(15);
    }

    @Test
    void testAddStockQuantity_SuccessNullLastPurchaseUnitPrice() {
        UUID productInfoId = productInfo.getId();
        Integer newStockQuantity = 5;
        BigDecimal lastPurchaseUnitPrice = null;
        productInfo.setUser(user1);
        when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
        when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProductInfo result = productInfoService.addStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice);
        assertThat(result.getStockQuantity()).isEqualTo(15);
    }

    @Test
    void testAddStockQuantity_Unauthorized() {
        UUID productInfoId = productInfo.getId();
        Integer newStockQuantity = 5;
        BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
        productInfo.setUser(user1);
        when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
        assertThatThrownBy(() -> productInfoService.addStockQuantity(productInfoId, user2, newStockQuantity, lastPurchaseUnitPrice))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("You are not authorized to update this product info.");
    }

    @Test
    void testAddStockQuantity_negativeQuantity() {
        UUID productInfoId = productInfo.getId();
        Integer newStockQuantity = -5;
        BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
        productInfo.setUser(user1);
        when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
        assertThatThrownBy(() -> productInfoService.addStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("New stock quantity must be non-negative.");
    }

     @Test
    void testAddStockQuantity_nullQuantity() {
        UUID productInfoId = productInfo.getId();
        Integer newStockQuantity = null;
        BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
        productInfo.setUser(user1);
        when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
        assertThatThrownBy(() -> productInfoService.addStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("New stock quantity must be non-negative.");
    }

}
