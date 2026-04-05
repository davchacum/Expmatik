package com.expmatik.backend.productInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.expmatik.backend.notification.NotificationService;
import com.expmatik.backend.notification.NotificationType;
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

    @Nested
    @DisplayName("Tests for ProductInfo calculated properties")
    class CalculatedPropertiesTests {
        @Test
        void testProductInfoCalculations() {
            assertEquals(new BigDecimal("59.90"), productInfo.getTotalStockValue());
            assertEquals(new BigDecimal("5.4890"), productInfo.getLastPurchaseUnitPriceWithVat());
            assertEquals(new BigDecimal("0.5010"), productInfo.getUnitProfit());
            assertEquals(new BigDecimal("5.0100"), productInfo.getTotalProfit());

            assertEquals(new BigDecimal("59.90"), productInfo2.getTotalStockValue());
            assertNull(productInfo2.getLastPurchaseUnitPriceWithVat());
            assertNull(productInfo2.getUnitProfit());
            assertNull(productInfo2.getTotalProfit());
        }
    }

    // ==================== getOrCreateProductInfo Tests ====================

    @Nested
    @DisplayName("Tests for getOrCreateProductInfo method")
    class GetOrCreateProductInfoTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("When product is custom and user owns it, should return existing ProductInfo")
            void testGetOrCreateProductInfo_CustomProduct_OwnedByUser() {
                UUID productId = productCustom.getId();
                UUID userId = user1.getId();
                BigDecimal unitPrice = new BigDecimal("5.00");
                productInfo.setProduct(productCustom);
                productInfo.setUser(user1);

                when(productService.findById(productId)).thenReturn(productCustom);
                when(productInfoRepository.findByProductIdAndUserId(productId, userId)).thenReturn(Optional.of(productInfo));

                ProductInfo result = productInfoService.getOrCreateProductInfo(productId, user1, unitPrice);

                assertEquals(productInfo.getProduct(), result.getProduct());
            }



            @Test
            @DisplayName("When product is custom and user owns it, should create new ProductInfo if it does not exist")
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
                
                assertNotNull(result);
                assertEquals(productCustom, result.getProduct());
                assertEquals(user1, result.getUser());
                BigDecimal expectedSalePrice = expectedPrice
                        .multiply(new BigDecimal("1.21"))
                        .setScale(2, RoundingMode.HALF_UP);

                assertEquals(0, result.getSaleUnitPrice().compareTo(expectedSalePrice));            
            }

            @Test
            @DisplayName("When product is non-custom, should create new ProductInfo with default values")
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

                assertNotNull(result);
                assertEquals(productNoCustom, result.getProduct());
                assertEquals(user1, result.getUser());
                assertEquals(0, result.getStockQuantity());
                assertEquals(new BigDecimal("0.21"), result.getVatRate());
                assertEquals(0, result.getSaleUnitPrice().compareTo(expectedPrice.multiply(new BigDecimal("1.21")).setScale(2, RoundingMode.HALF_UP)));
            }

            @Test
            @DisplayName("When product is non-custom and unit price is null, should create new ProductInfo with default values and calculate sale price based on default unit price")
            void testGetOrCreateProductInfo_NonCustomProduct_UnitPriceNull() {
                UUID productId = productNoCustom.getId();
                UUID userId = user1.getId();

                when(productService.findById(productId)).thenReturn(productNoCustom);
                when(productInfoRepository.findByProductIdAndUserId(productId, userId)).thenReturn(Optional.empty());
                when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                ProductInfo result = productInfoService.getOrCreateProductInfo(productId, user1, null);
                    BigDecimal expectedPrice = BigDecimal.ONE.multiply(new BigDecimal("1.20")).setScale(2, RoundingMode.HALF_UP);


                assertNotNull(result);
                assertEquals(productNoCustom, result.getProduct());
                assertEquals(user1, result.getUser());
                assertEquals(0, result.getStockQuantity());
                assertEquals(new BigDecimal("0.21"), result.getVatRate());
                assertEquals(0, result.getSaleUnitPrice().compareTo(expectedPrice.multiply(new BigDecimal("1.21")).setScale(2, RoundingMode.HALF_UP)));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("When product is custom and user does not own it, should throw AccessDeniedException")
            void testGetOrCreateProductInfo_CustomProduct_NotOwnedByUser() {
                UUID productId = productCustom.getId();
                BigDecimal unitPrice = new BigDecimal("5.00");

                when(productService.findById(productId)).thenReturn(productCustom);

                assertThrows(AccessDeniedException.class, () -> productInfoService.getOrCreateProductInfo(productId, user2, unitPrice));
            }
        }
    }

    // ==================== updateProductInfo Tests ====================
    @Nested
    @DisplayName("Tests for updateProductInfo method")
    class UpdateProductInfoTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            
            @Test
            @DisplayName("When product info is updated with valid data, should update successfully without creating notifications")
            void testUpdateProductInfo__ValidData_Success() {
                UUID productInfoId = productInfo.getId();
                productInfo.setProduct(productCustom);
                ProductInfoUpdate updatedInfo = new ProductInfoUpdate(21, new BigDecimal("6.99"), new BigDecimal("0.10"));
                productInfo.setUser(user1);
                when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
                when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                ProductInfo result = productInfoService.updateProductInfo(productInfoId, user1, updatedInfo);
                assertEquals(21, result.getStockQuantity());
                assertEquals(0, result.getSaleUnitPrice().compareTo(new BigDecimal("6.99")));
                assertEquals(0, result.getVatRate().compareTo(new BigDecimal("0.10")));

                verify(notificationService, Mockito.never()).createNotification(
                    ArgumentMatchers.any(NotificationType.class),
                    ArgumentMatchers.anyString(),
                    ArgumentMatchers.anyString(),
                    ArgumentMatchers.any(User.class)
                );
            }

            @Test
            @DisplayName("When product info is updated and stock is low, should create notification if stock is low")
            void testUpdateProductInfo_ValidData_SuccessAndCreateNotificationInventoryStockLow() {
                UUID productInfoId = productInfo.getId();
                productInfo.setProduct(productCustom);
                ProductInfoUpdate updatedInfo = new ProductInfoUpdate(20, new BigDecimal("6.99"), new BigDecimal("0.10"));
                productInfo.setUser(user1);
                when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
                when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                ProductInfo result = productInfoService.updateProductInfo(productInfoId, user1, updatedInfo);
                assertEquals(20, result.getStockQuantity());
                assertEquals(0, result.getSaleUnitPrice().compareTo(new BigDecimal("6.99")));
                assertEquals(0, result.getVatRate().compareTo(new BigDecimal("0.10")));

                verify(notificationService).createNotification(
                    ArgumentMatchers.eq(NotificationType.INVENTORY_STOCK_LOW),
                    ArgumentMatchers.contains("tiene pocas unidades en stock"),
                    ArgumentMatchers.anyString(),
                    ArgumentMatchers.eq(user1)
                );
            }

            @Test
            @DisplayName("When product info is updated and stock is out of stock, should create notification if stock quantity is set to 0")
            void testUpdateProductInfo_ValidData_SuccessAndCreateNotificationInventoryOutOfStock() {
                UUID productInfoId = productInfo.getId();
                productInfo.setProduct(productCustom);
                ProductInfoUpdate updatedInfo = new ProductInfoUpdate(0, new BigDecimal("6.99"), new BigDecimal("0.10"));
                productInfo.setUser(user1);
                when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
                when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                ProductInfo result = productInfoService.updateProductInfo(productInfoId, user1, updatedInfo);
                assertEquals(0, result.getStockQuantity());
                assertEquals(0, result.getSaleUnitPrice().compareTo(new BigDecimal("6.99")));
                assertEquals(0, result.getVatRate().compareTo(new BigDecimal("0.10")));

                verify(notificationService).createNotification(
                    ArgumentMatchers.eq(NotificationType.INVENTORY_OUT_OF_STOCK),
                    ArgumentMatchers.contains("sin stock"),
                    ArgumentMatchers.anyString(),
                    ArgumentMatchers.eq(user1)
                );
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("When user is not the owner of the product info, should throw AccessDeniedException")
            void testUpdateProductInfo_NotOwnedByUser_ShouldThrowAccessDeniedException() {
                UUID productInfoId = productInfo.getId();
                ProductInfoUpdate updatedInfo = new ProductInfoUpdate(20, new BigDecimal("6.99"), new BigDecimal("0.10"));
                productInfo.setUser(user1);
                when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
                assertThrows(AccessDeniedException.class, () -> productInfoService.updateProductInfo(productInfoId, user2, updatedInfo));
            }
        }
    }

    // ==================== editStockQuantity Tests ====================

    @Nested
    @DisplayName("Tests for editStockQuantity method")
    class EditStockQuantityTests {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should update stock quantity successfully without creating notifications when stock is not low or out of stock")
            void testEditStockQuantity_validData_Success() {
                UUID productInfoId = productInfo.getId();
                Integer newStockQuantity = 21;
                BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
                productInfo.setUser(user1);
                productCustom.setName("Custom Product");
                productInfo.setProduct(productCustom);

                when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
                when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                ProductInfo result = productInfoService.editStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice);
                assertEquals(31, result.getStockQuantity());

                verify(notificationService, Mockito.never()).createNotification(
                    ArgumentMatchers.any(NotificationType.class),
                    ArgumentMatchers.anyString(),
                    ArgumentMatchers.anyString(),
                    ArgumentMatchers.any(User.class)
                );
            }

            @Test
            @DisplayName("should update stock quantity successfully and create notification if stock is low")
            void testEditStockQuantity_ValidData_SuccessAndCreateNotificationInventoryStockLow() {
                UUID productInfoId = productInfo.getId();
                Integer newStockQuantity = 5;
                BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
                productInfo.setUser(user1);
                productCustom.setName("Custom Product");
                productInfo.setProduct(productCustom);

                when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
                when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                ProductInfo result = productInfoService.editStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice);
                assertEquals(15, result.getStockQuantity());

                verify(notificationService).createNotification(
                    ArgumentMatchers.eq(NotificationType.INVENTORY_STOCK_LOW),
                    ArgumentMatchers.contains("tiene pocas unidades en stock"),
                    ArgumentMatchers.anyString(),
                    ArgumentMatchers.eq(user1)
                );
            }

            @Test
            @DisplayName("should update stock quantity successfully and create notification if stock is out of stock")
            void testEditStockQuantity_ValidData_SuccessAndCreateNotificationInventoryOutOfStock() {
                UUID productInfoId = productInfo.getId();
                Integer newStockQuantity = -15;
                BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
                productInfo.setUser(user1);
                productCustom.setName("Custom Product");
                productInfo.setProduct(productCustom);

                when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
                when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                ProductInfo result = productInfoService.editStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice);
                assertEquals(0, result.getStockQuantity());

                verify(notificationService).createNotification(
                    ArgumentMatchers.eq(NotificationType.INVENTORY_OUT_OF_STOCK),
                    ArgumentMatchers.contains("se ha quedado sin stock"),
                    ArgumentMatchers.anyString(),
                    ArgumentMatchers.eq(user1)
                );
            }

            @Test
            @DisplayName("should update stock quantity successfully without creating notifications when last purchase unit price is null")
            void testEditStockQuantity_ValidData_SuccessNullLastPurchaseUnitPrice() {
                UUID productInfoId = productInfo.getId();
                Integer newStockQuantity = 5;
                BigDecimal lastPurchaseUnitPrice = null;
                productInfo.setUser(user1);
                productCustom.setName("Custom Product");
                productInfo.setProduct(productCustom);
                when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
                when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                ProductInfo result = productInfoService.editStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice);
                assertEquals(15, result.getStockQuantity());
            }

            @Test
            @DisplayName("should handle negative stock quantity by setting it to zero")
            void testEditStockQuantity_ValidData_negativeQuantity() {
                UUID productInfoId = productInfo.getId();
                Integer newStockQuantity = -5;
                BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
                productInfo.setUser(user1);
                productCustom.setName("Custom Product");
                productInfo.setProduct(productCustom);
                when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
                when(productInfoRepository.save(ArgumentMatchers.any(ProductInfo.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                ProductInfo result = productInfoService.editStockQuantity(productInfoId, user1, newStockQuantity, lastPurchaseUnitPrice);
                assertEquals(5, result.getStockQuantity());
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should throw AccessDeniedException when user is not the owner of the product info")
            void testEditStockQuantity_NotOwnedByUser_ShouldThrowAccessDeniedException() {
                UUID productInfoId = productInfo.getId();
                Integer newStockQuantity = 5;
                BigDecimal lastPurchaseUnitPrice = new BigDecimal("4.99");
                productInfo.setUser(user1);
                productCustom.setName("Custom Product");
                productInfo.setProduct(productCustom);
                when(productInfoRepository.findById(productInfoId)).thenReturn(Optional.of(productInfo));
                assertThrows(AccessDeniedException.class, () -> productInfoService.editStockQuantity(productInfoId, user2, newStockQuantity, lastPurchaseUnitPrice));
            }
        }
    }
}
