package com.expmatik.backend.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.batch.DTOs.BatchValidationResponse;
import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.file.FileStorageService;
import com.expmatik.backend.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Spy
    @InjectMocks
    private ProductService productService;

    private Product productCustom;
    private Product productNoCustom;
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
    }

    // ==================== findProductInOpenFoodFacts Tests ====================

    @Nested
    @DisplayName("Tests for findProductInOpenFoodFacts method")
    class FindProductInOpenFoodFactsTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
        
            @Test
            @DisplayName("Test finding product in Open Food Facts")
            void testFindProductInOpenFoodFacts_ValidBarcode_ReturnsProduct() throws IOException {
                String barcode = "4716982022201";
                JsonNode fixture = new ObjectMapper().readTree(readFixture("product/openfoodfacts-ok.json"));

                doReturn(fixture).when(productService).fetchOpenFoodFactsResponse(eq(barcode), any(ObjectMapper.class));

                Optional<Product> result = productService.findProductInOpenFoodFacts(barcode);

                assertTrue(result.isPresent());

                Product product = result.get();

                assertEquals(barcode, product.getBarcode());
                assertEquals("Choco Bom", product.getName());
                assertEquals("Gullón", product.getBrand());
                assertTrue(product.getDescription().isEmpty());
                assertFalse(product.getIsCustom());
                assertTrue(product.getIsPerishable());
                assertNull(product.getCreatedBy());
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Returns empty when product not found in Open Food Facts")
            void testFindProductInOpenFoodFacts_BarcodeNotFound_ReturnsEmpty() throws IOException {
                String barcode = "2000000000017";
                JsonNode fixture = new ObjectMapper().readTree(readFixture("product/openfoodfacts-not-found.json"));

                doReturn(fixture).when(productService).fetchOpenFoodFactsResponse(eq(barcode), any(ObjectMapper.class));

                Optional<Product> result = productService.findProductInOpenFoodFacts(barcode);

                assertTrue(result.isEmpty());
            }

            @Test
            @DisplayName("Returns empty when Open Food Facts API returns failure")
            void testFindProductInOpenFoodFacts_ApiFailure_ReturnsEmpty() throws IOException {
                String barcode = "78436548736534287654328";
                JsonNode fixture = new ObjectMapper().readTree(readFixture("product/openfoodfacts-failure.json"));

                doReturn(fixture).when(productService).fetchOpenFoodFactsResponse(eq(barcode), any(ObjectMapper.class));
                Optional<Product> result = productService.findProductInOpenFoodFacts(barcode);
                assertTrue(result.isEmpty());
            }

            @Test
            @DisplayName("Returns empty and logs error when Open Food Facts API connection fails")
            void testFindProductInOpenFoodFacts_ConnectionFailure_ReturnsEmpty() throws IOException {
                String barcode = "5555555555555";
                IOException ioException = new IOException("Connection timed out");

                doThrow(ioException).when(productService).fetchOpenFoodFactsResponse(eq(barcode), any(ObjectMapper.class));

                PrintStream originalErr = System.err;
                ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();

                try {
                    System.setErr(new PrintStream(capturedErr));

                    Optional<Product> result = productService.findProductInOpenFoodFacts(barcode);

                    assertTrue(result.isEmpty());
                    assertTrue(capturedErr.toString(StandardCharsets.UTF_8).contains("Final attempt failed for barcode " + barcode + ": " + ioException.getMessage()));
                } finally {
                    System.setErr(originalErr);
                }
            }
        }

        private String readFixture(String resourcePath) throws IOException {
                try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                    assertNotNull(inputStream, "Missing test fixture: " + resourcePath);
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
        }
    }

    // ==================== updateProductImage Tests ====================

    @Nested
    @DisplayName("Tests for updateProductImage method")
    class UpdateProductImageTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Updates custom product image and deletes previous local image")
            void testUpdateProductImageCustom_UpdateImageAndDeletePrevious_Success() {
                MultipartFile file = Mockito.mock(MultipartFile.class);
                when(file.isEmpty()).thenReturn(false);

                productCustom.setImageUrl("uploads/images/old-custom.png");
                when(fileStorageService.saveCustomProductImage(file)).thenReturn("uploads/images/new-custom.png");
                when(fileStorageService.isExternalUrl("uploads/images/old-custom.png")).thenReturn(false);
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = productService.updateProductImage(productCustom, file, null);

                assertEquals("uploads/images/new-custom.png", result.getImageUrl());
                verify(fileStorageService).deleteProductImage("uploads/images/old-custom.png");
                verify(productRepository).save(productCustom);
            }

            @Test
            @DisplayName("Updates non-custom image URL and deletes previous local image")
            void testUpdateProductImageNonCustom_UpdateImageUrlAndDeletePrevious_Success() {
                productNoCustom.setImageUrl("uploads/images/old-catalog.png");
                String newImageUrl = "https://cdn.example.com/new-catalog.png";

                when(fileStorageService.isExternalUrl("uploads/images/old-catalog.png")).thenReturn(false);
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = productService.updateProductImage(productNoCustom, null, newImageUrl);

                assertEquals(newImageUrl, result.getImageUrl());
                verify(fileStorageService).deleteProductImage("uploads/images/old-catalog.png");
                verify(productRepository).save(productNoCustom);
            }

            @Test
            @DisplayName("Updates custom product with external URL - does not delete")
            void testUpdateProductImageCustom_WithExternalUrl_DoesNotDeletePrevious() {
                MultipartFile file = Mockito.mock(MultipartFile.class);
                when(file.isEmpty()).thenReturn(false);

                productCustom.setImageUrl("https://external.cdn.com/old-image.png");
                when(fileStorageService.saveCustomProductImage(file)).thenReturn("uploads/images/new-custom.png");
                when(fileStorageService.isExternalUrl("https://external.cdn.com/old-image.png")).thenReturn(true);
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = productService.updateProductImage(productCustom, file, null);

                assertEquals("uploads/images/new-custom.png", result.getImageUrl());
                verify(fileStorageService, never()).deleteProductImage(any());
                verify(productRepository).save(productCustom);
            }

            @Test
            @DisplayName("Updates custom product without previous image")
            void testUpdateProductImageCustom_WithoutPreviousImage_Success() {
                MultipartFile file = Mockito.mock(MultipartFile.class);
                when(file.isEmpty()).thenReturn(false);

                productCustom.setImageUrl(null);
                when(fileStorageService.saveCustomProductImage(file)).thenReturn("uploads/images/new-custom.png");
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = productService.updateProductImage(productCustom, file, null);

                assertEquals("uploads/images/new-custom.png", result.getImageUrl());
                verify(fileStorageService, never()).deleteProductImage(any());
                verify(productRepository).save(productCustom);
            }

            @Test
            @DisplayName("Updates non-custom with external URL - does not delete previous external")
            void testUpdateProductImageNonCustom_WithExternalUrl_DoesNotDeletePrevious() {
                productNoCustom.setImageUrl("https://cdn.openfoodfacts.org/old-image.png");
                String newImageUrl = "https://cdn.example.com/new-catalog.png";

                when(fileStorageService.isExternalUrl("https://cdn.openfoodfacts.org/old-image.png")).thenReturn(true);
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = productService.updateProductImage(productNoCustom, null, newImageUrl);

                assertEquals(newImageUrl, result.getImageUrl());
                verify(fileStorageService, never()).deleteProductImage(any());
                verify(productRepository).save(productNoCustom);
            }

            @Test
            @DisplayName("Updates non-custom without previous image")
            void testUpdateProductImageNonCustom_WithoutPreviousImage_Success() {
                productNoCustom.setImageUrl(null);
                String newImageUrl = "https://cdn.example.com/new-catalog.png";

                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = productService.updateProductImage(productNoCustom, null, newImageUrl);

                assertEquals(newImageUrl, result.getImageUrl());
                verify(fileStorageService, never()).deleteProductImage(any());
                verify(productRepository).save(productNoCustom);
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Throws when non-custom product has null image URL")
            void testUpdateProductImageNonCustom_WithNullUrl_shouldThrowBadRequestException() {
                BadRequestException exception = assertThrows(BadRequestException.class, () -> productService.updateProductImage(productNoCustom, null, null));

                assertEquals("Non-custom products require an image URL", exception.getMessage());

                verify(productRepository, never()).save(any(Product.class));
            }

            @Test
            @DisplayName("Throws when custom product has empty file")
            void testUpdateProductImageCustom_WithEmptyFile_shouldThrowBadRequestException() {
                MultipartFile file = Mockito.mock(MultipartFile.class);
                when(file.isEmpty()).thenReturn(true);

                BadRequestException exception = assertThrows(BadRequestException.class, () -> productService.updateProductImage(productCustom, file, null));

                assertEquals("Custom products require an image file", exception.getMessage());

                verify(productRepository, never()).save(any(Product.class));
            }

            @Test
            @DisplayName("Throws when non-custom product has blank image URL")
            void testUpdateProductImageNonCustom_BlankImageUrl_shouldThrowBadRequestException() {

                BadRequestException exception = assertThrows(BadRequestException.class, () -> productService.updateProductImage(productNoCustom, null, "   "));

                assertEquals("Non-custom products require an image URL", exception.getMessage());

                verify(productRepository, never()).save(any(Product.class));
            }

            @Test
            @DisplayName("Throws when custom product has no file")
            void testUpdateProductImageCustom_WithoutFile_shouldThrowBadRequestException() {

                BadRequestException exception = assertThrows(BadRequestException.class, () -> productService.updateProductImage(productCustom, null, null));

                assertEquals("Custom products require an image file", exception.getMessage());
                verify(productRepository, never()).save(any(Product.class));
            }
        }
    }

    // ==================== checkUniqueBarcode Tests ====================ç

    @Nested
    @DisplayName("Tests for checkUniqueBarcode and checkUniqueBarcodeCustom methods")
    class CheckUniqueBarcodeTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("checkUniqueBarcode - succeeds when barcode does not exist")
            void testCheckUniqueBarcode_ValidBarcode_Succeess() {
                String barcode = "1111111111111";

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, user1.getId()))
                        .thenReturn(Optional.empty());

                productService.checkUniqueBarcode(barcode, user1.getId());

                verify(productRepository).findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, user1.getId());
            }

                        @Test
            @DisplayName("checkUniqueBarcodeCustom - succeeds when barcode does not exist anywhere")
            void testCheckUniqueBarcodeCustom_ValidBarcode_Succeeds() throws IOException {
                String barcode = "2222222222222";
                UUID userId = user1.getId();

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                        .thenReturn(Optional.empty());
                doReturn(Optional.empty()).when(productService).findProductInOpenFoodFacts(barcode);

                productService.checkUniqueBarcodeCustom(barcode, userId);

                verify(productRepository).findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId);
                verify(productService).findProductInOpenFoodFacts(barcode);
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("checkUniqueBarcode - throws when barcode exists in catalog")
            void testCheckUniqueBarcode_ExistingBarcode_ThrowsConflictException() {
                String barcode = "9876543210123";

                Product existingProduct = new Product();
                existingProduct.setBarcode(barcode);
                existingProduct.setIsCustom(false);

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, user1.getId()))
                        .thenReturn(Optional.of(existingProduct));

                ConflictException exception = assertThrows(ConflictException.class, () -> productService.checkUniqueBarcode(barcode, user1.getId()));
                assertEquals("A product with this barcode already exists.", exception.getMessage());
            }

            @Test
            @DisplayName("checkUniqueBarcodeCustom - throws when barcode exists in database")
            void testCheckUniqueBarcodeCustom_ExistingBarcode_ThrowsConflictException() {
                String barcode = "3333333333333";
                UUID userId = user1.getId();

                Product existingProduct = new Product();
                existingProduct.setBarcode(barcode);

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                        .thenReturn(Optional.of(existingProduct));

                ConflictException exception = assertThrows(ConflictException.class, () -> productService.checkUniqueBarcodeCustom(barcode, userId));
                assertEquals("A product with this barcode already exists.", exception.getMessage());

                // No debe llamar a la API si ya existe en la base de datos
                verify(productService, never()).findProductInOpenFoodFacts(any());
            }

            @Test
            @DisplayName("checkUniqueBarcodeCustom - throws when barcode exists in Open Food Facts")
            void testCheckUniqueBarcodeCustom_ExistingBarcodeInAPI_ThrowsConflictException() throws IOException {
                String barcode = "4444444444444";
                UUID userId = user1.getId();

                Product apiProduct = new Product();
                apiProduct.setBarcode(barcode);
                apiProduct.setIsCustom(false);

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                        .thenReturn(Optional.empty());
                doReturn(Optional.of(apiProduct)).when(productService).findProductInOpenFoodFacts(barcode);

                ConflictException exception = assertThrows(ConflictException.class, () -> productService.checkUniqueBarcodeCustom(barcode, userId));
                assertEquals("A product with this barcode already exists in the external catalog.", exception.getMessage());

                verify(productRepository).findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId);
                verify(productService).findProductInOpenFoodFacts(barcode);
            }
        }
    }

    // ==================== createProductCustom Tests ====================

    @Nested
    @DisplayName("Tests for createProductCustom method")
    class CreateProductCustomTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("createProductCustom - orchestrates checkUniqueBarcodeCustom and updateProductImage correctly")
            void testCreateProductCustom_ValidProduct_ReturnsSavedProduct() throws IOException {
                MultipartFile image = Mockito.mock(MultipartFile.class);
                when(image.isEmpty()).thenReturn(false);

                String barcode = "5555555555555";
                productCustom.setBarcode(barcode);
                UUID userId = user1.getId();

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                        .thenReturn(Optional.empty());
                doReturn(Optional.empty()).when(productService).findProductInOpenFoodFacts(barcode);

                when(fileStorageService.saveCustomProductImage(image)).thenReturn("uploads/images/custom-new.png");
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = productService.createProductCustom(productCustom, image);

                verify(productRepository).findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId);
                verify(productService).findProductInOpenFoodFacts(barcode);
                verify(fileStorageService).saveCustomProductImage(image);
                verify(productRepository).save(productCustom);

                assertEquals("uploads/images/custom-new.png", result.getImageUrl());
                assertEquals(barcode, result.getBarcode());
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("createProductCustom - stops execution when checkUniqueBarcodeCustom throws exception")
            void testCreateProductCustom_StopsWhenBarcodeExists_ShouldThrowConflictException() {
                MultipartFile image = Mockito.mock(MultipartFile.class);

                String barcode = "6666666666666";
                productCustom.setBarcode(barcode);
                UUID userId = user1.getId();

                // Simular que el barcode ya existe en BD
                Product existingProduct = new Product();
                existingProduct.setBarcode(barcode);

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                        .thenReturn(Optional.of(existingProduct));

                // Debe lanzar excepción de checkUniqueBarcodeCustom
                ConflictException exception = assertThrows(ConflictException.class, () -> productService.createProductCustom(productCustom, image));
                assertEquals("A product with this barcode already exists.", exception.getMessage());

                // Verificar que updateProductImage NO se ejecutó
                verify(fileStorageService, never()).saveCustomProductImage(any());
                verify(productRepository, never()).save(any(Product.class));
            }
        }
    }

    // ==================== createProductOpenFoodFacts Tests ====================

    @Nested
    @DisplayName("Tests for createProductOpenFoodFacts method")
    class CreateProductOpenFoodFactsTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("createProductOpenFoodFacts - orchestrates checkUniqueBarcode, findProductInOpenFoodFacts and updateProductImage correctly")
            void testCreateProductOpenFoodFacts_ValidBarcode_Success() throws IOException {
                String barcode = "7777777777777";
                UUID userId = user1.getId();

                Product apiProduct = new Product();
                apiProduct.setBarcode(barcode);
                apiProduct.setName("Producto API");
                apiProduct.setBrand("Marca API");
                apiProduct.setImageUrl("https://api.example.com/image.png");
                apiProduct.setIsCustom(false);

                // Mock checkUniqueBarcode (no lanza excepción)
                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                        .thenReturn(Optional.empty());

                // Mock findProductInOpenFoodFacts
                doReturn(Optional.of(apiProduct)).when(productService).findProductInOpenFoodFacts(barcode);

                // Mock updateProductImage
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = productService.createProductOpenFoodFacts(barcode, userId);

                // Verificar que se llamaron los métodos en orden
                verify(productRepository).findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId);
                verify(productService).findProductInOpenFoodFacts(barcode);
                verify(productRepository).save(apiProduct);

                // Verificar resultado
                assertEquals(barcode, result.getBarcode());
                assertFalse(result.getIsCustom());
                assertEquals("https://api.example.com/image.png", result.getImageUrl());
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("createProductOpenFoodFacts - stops execution when checkUniqueBarcode throws exception")
            void testCreateProductOpenFoodFacts_StopsWhenBarcodeExists_shouldThrowConflictException() {
                String barcode = "8888888888888";
                UUID userId = user1.getId();

                // Simular que el barcode ya existe en catálogo
                Product existingProduct = new Product();
                existingProduct.setBarcode(barcode);
                existingProduct.setIsCustom(false);

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                        .thenReturn(Optional.of(existingProduct));

                // Debe lanzar excepción de checkUniqueBarcode
                ConflictException exception = assertThrows(ConflictException.class, () -> productService.createProductOpenFoodFacts(barcode, userId));
                assertEquals("A product with this barcode already exists.", exception.getMessage());

                // Verificar que findProductInOpenFoodFacts y updateProductImage NO se ejecutaron
                verify(productService, never()).findProductInOpenFoodFacts(any());
                verify(productRepository, never()).save(any(Product.class));
            }

            @Test
            @DisplayName("createProductOpenFoodFacts - throws ResourceNotFoundException when product not found in API")
            void testCreateProductOpenFoodFacts_StopExecutionWhenNotFoundInAPI_shouldThrowResourceNotFoundException() throws IOException {
                String barcode = "9999999999999";
                UUID userId = user1.getId();

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                        .thenReturn(Optional.empty());

                doReturn(Optional.empty()).when(productService).findProductInOpenFoodFacts(barcode);

                ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> productService.createProductOpenFoodFacts(barcode, userId));
                assertEquals("Product with barcode " + barcode + " not found in Open Food Facts external catalog. Consider creating it as a custom product.", exception.getMessage());

                verify(productRepository, never()).save(any(Product.class));
            }
        }
    }

    // ==================== validateBarcodes Tests ====================

    @Nested
    @DisplayName("Tests for validateBarcodes method")
    class ValidateBarcodesTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("validateBarcodes - returns empty lists when input is empty")
            void testValidateBarcodes_EmptyInput_ReturnsEmptyLists() {
                List<String> barcodes = List.of();
                UUID userId = user1.getId();

                BatchValidationResponse result = productService.validateBarcodes(barcodes, userId);

                assertTrue(result.valid().isEmpty());
                assertTrue(result.notFound().isEmpty());
            }

            @Test
            @DisplayName("validateBarcodes - correctly classifies mixed scenarios")
            void testValidateBarcodes_MixedScenarios_shouldSuccess() throws IOException {
                List<String> barcodes = List.of("1111111111111", "3333333333333", "5555555555555");
                UUID userId = user1.getId();

                Product dbProduct = new Product();
                dbProduct.setBarcode("1111111111111");

                Product apiProduct = new Product();
                apiProduct.setBarcode("3333333333333");

                // Barcode 1 en BD
                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById("1111111111111", userId))
                        .thenReturn(Optional.of(dbProduct));

                // Barcode 3 no en BD pero sí en API
                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById("3333333333333", userId))
                        .thenReturn(Optional.empty());
                doReturn(Optional.of(apiProduct)).when(productService).findProductInOpenFoodFacts("3333333333333");

                // Barcode 5 no existe en ningún sitio
                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById("5555555555555", userId))
                        .thenReturn(Optional.empty());
                doReturn(Optional.empty()).when(productService).findProductInOpenFoodFacts("5555555555555");

                BatchValidationResponse result = productService.validateBarcodes(barcodes, userId);

                assertIterableEquals(
                List.of("1111111111111", "3333333333333"),
                result.valid()
                );

                assertIterableEquals(
                        List.of("5555555555555"),
                        result.notFound()
                );

                // Verificar que solo se llamó a la API para los que no estaban en BD
                verify(productService, never()).findProductInOpenFoodFacts("1111111111111");
                verify(productService).findProductInOpenFoodFacts("3333333333333");
                verify(productService).findProductInOpenFoodFacts("5555555555555");
            }
        }
    }

    // ==================== findInternalProductByBarcode Tests ====================

    @Nested
    @DisplayName("Tests for findInternalProductByBarcode method")
    class FindInternalProductByBarcodeTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("findInternalProductByBarcode - returns product when found")
            void testFindInternalProductByBarcode_ValidBarcode_ReturnsProduct() {
                String barcode = "1111111111111";
                UUID userId = user1.getId();

                Product existingProduct = new Product();
                existingProduct.setBarcode(barcode);

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                        .thenReturn(Optional.of(existingProduct));

                Product result = productService.findInternalProductByBarcode(barcode, userId);

                assertNotNull(result);
                assertEquals(barcode, result.getBarcode());
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("findInternalProductByBarcode - throws ResourceNotFoundException when not found")
            void testFindInternalProductByBarcode_InvalidBarcode_ThrowsResourceNotFoundException() {
                String barcode = "2222222222222";
                UUID userId = user1.getId();

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                        .thenReturn(Optional.empty());

                ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> productService.findInternalProductByBarcode(barcode, userId));
                assertEquals("No product found in the internal catalog with barcode: " + barcode, exception.getMessage());
            }
        }
    }

    // ==================== getOrCreateProductByBarcode Tests ====================

    @Nested
    @DisplayName("Tests for getOrCreateProductByBarcode method")
    class GetOrCreateProductByBarcodeTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("getOrCreateProductByBarcode - returns existing product if found")
            void testGetOrCreateProductByBarcode_ValidBarcode_ReturnsExisting() {
                String barcode = "1111111111111";
                UUID userId = user1.getId();

                Product existingProduct = new Product();
                existingProduct.setBarcode(barcode);

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                        .thenReturn(Optional.of(existingProduct));

                Product result = productService.getOrCreateProductByBarcode(barcode, userId);

                assertNotNull(result);
                assertEquals(barcode, result.getBarcode());

                verify(productService, never()).createProductOpenFoodFacts(any(), any());
            }

            @Test
            @DisplayName("getOrCreateProductByBarcode - creates new product when not found")
            void testGetOrCreateProductByBarcode_ValidBarcodeButNotInDatabase_CreatesNew() throws IOException {
                String barcode = "2222222222222";
                UUID userId = user1.getId();

                Product apiProduct = new Product();
                apiProduct.setBarcode(barcode);
                apiProduct.setName("Producto API");
                apiProduct.setBrand("Marca API");
                apiProduct.setImageUrl("https://api.example.com/image.png");
                apiProduct.setIsCustom(false);

                when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                        .thenReturn(Optional.empty());

                doReturn(Optional.of(apiProduct)).when(productService).findProductInOpenFoodFacts(barcode);
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = productService.getOrCreateProductByBarcode(barcode, userId);

                assertNotNull(result);
                assertEquals(barcode, result.getBarcode());
                assertEquals("Producto API", result.getName());
                assertEquals("Marca API", result.getBrand());
                assertEquals("https://api.example.com/image.png", result.getImageUrl());

                verify(productService).createProductOpenFoodFacts(barcode, userId);
            }
        }
    }
}
