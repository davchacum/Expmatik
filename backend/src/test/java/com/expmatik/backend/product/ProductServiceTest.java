package com.expmatik.backend.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    @DisplayName("Test finding product in Open Food Facts")
    void testFindProductInOpenFoodFacts() throws IOException {
        String barcode = "4716982022201";
        JsonNode fixture = new ObjectMapper().readTree(readFixture("product/openfoodfacts-ok.json"));

        doReturn(fixture).when(productService).fetchOpenFoodFactsResponse(eq(barcode), any(ObjectMapper.class));

        Optional<Product> result = productService.findProductInOpenFoodFacts(barcode);

        assertThat(result).isPresent();
        assertThat(result.get().getBarcode()).isEqualTo(barcode);
        assertThat(result.get().getName()).isEqualTo("Choco Bom");
        assertThat(result.get().getBrand()).isEqualTo("Gullón");
        assertThat(result.get().getDescription()).isEmpty();
        assertThat(result.get().getIsCustom()).isFalse();
        assertThat(result.get().getIsPerishable()).isTrue();
        assertThat(result.get().getCreatedBy()).isNull();
    }

    @Test
    @DisplayName("Returns empty when product not found in Open Food Facts")
    void testFindProductInOpenFoodFactsNotFound() throws IOException {
        String barcode = "2000000000017";
        JsonNode fixture = new ObjectMapper().readTree(readFixture("product/openfoodfacts-not-found.json"));

        doReturn(fixture).when(productService).fetchOpenFoodFactsResponse(eq(barcode), any(ObjectMapper.class));

        Optional<Product> result = productService.findProductInOpenFoodFacts(barcode);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Returns empty when Open Food Facts API returns failure")
    void testFindProductInOpenFoodFactsFailure() throws IOException {
        String barcode = "78436548736534287654328";
        JsonNode fixture = new ObjectMapper().readTree(readFixture("product/openfoodfacts-failure.json"));

        doReturn(fixture).when(productService).fetchOpenFoodFactsResponse(eq(barcode), any(ObjectMapper.class));
        Optional<Product> result = productService.findProductInOpenFoodFacts(barcode);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Throws when Open Food Facts API returns empty response")
    void testFindProductInOpenFoodFactsEmptyResponse() throws IOException {
        String barcode = "9999999999999";
        JsonNode emptyResponse = new ObjectMapper().createObjectNode();

        doReturn(emptyResponse).when(productService).fetchOpenFoodFactsResponse(eq(barcode), any(ObjectMapper.class));

        assertThatThrownBy(() -> productService.findProductInOpenFoodFacts(barcode))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found with barcode: " + barcode);
    }

    @Test
    @DisplayName("Returns empty and logs error when Open Food Facts API connection fails")
    void testFindProductInOpenFoodFactsConnectionError() throws IOException {
        String barcode = "5555555555555";
        IOException ioException = new IOException("Connection timed out");

        doThrow(ioException).when(productService).fetchOpenFoodFactsResponse(eq(barcode), any(ObjectMapper.class));

        PrintStream originalErr = System.err;
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();

        try {
            System.setErr(new PrintStream(capturedErr));

            Optional<Product> result = productService.findProductInOpenFoodFacts(barcode);

            assertThat(result).isEmpty();
            assertThat(capturedErr.toString(StandardCharsets.UTF_8))
                    .contains("Error connecting to Open Food Facts API: " + ioException.getMessage());
        } finally {
            System.setErr(originalErr);
        }
    }

    private String readFixture(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(inputStream).as("Missing test fixture: %s", resourcePath).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ==================== updateProductImage Tests ====================

    @Test
    @DisplayName("Throws when custom product has no file")
    void testUpdateProductImageCustomWithoutFileThrows() {
        assertThatThrownBy(() -> productService.updateProductImage(productCustom, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Custom products require an image file");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Updates custom product image and deletes previous local image")
    void testUpdateProductImageCustomSuccess() {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);

        productCustom.setImageUrl("uploads/images/old-custom.png");
        when(fileStorageService.saveCustomProductImage(file)).thenReturn("uploads/images/new-custom.png");
        when(fileStorageService.isExternalUrl("uploads/images/old-custom.png")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProductImage(productCustom, file, null);

        assertThat(result.getImageUrl()).isEqualTo("uploads/images/new-custom.png");
        verify(fileStorageService).deleteProductImage("uploads/images/old-custom.png");
        verify(productRepository).save(productCustom);
    }

    

    @Test
    @DisplayName("Throws when non-custom product has blank image URL")
    void testUpdateProductImageNonCustomWithoutUrlThrows() {
        assertThatThrownBy(() -> productService.updateProductImage(productNoCustom, null, "   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Non-custom products require an image URL");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Updates non-custom image URL and deletes previous local image")
    void testUpdateProductImageNonCustomSuccess() {
        productNoCustom.setImageUrl("uploads/images/old-catalog.png");
        String newImageUrl = "https://cdn.example.com/new-catalog.png";

        when(fileStorageService.isExternalUrl("uploads/images/old-catalog.png")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProductImage(productNoCustom, null, newImageUrl);

        assertThat(result.getImageUrl()).isEqualTo(newImageUrl);
        verify(fileStorageService).deleteProductImage("uploads/images/old-catalog.png");
        verify(productRepository).save(productNoCustom);
    }

    @Test
    @DisplayName("Throws when custom product has empty file")
    void testUpdateProductImageCustomWithEmptyFileThrows() {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProductImage(productCustom, file, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Custom products require an image file");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Updates custom product with external URL - does not delete")
    void testUpdateProductImageCustomWithExternalUrlDoesNotDelete() {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);

        productCustom.setImageUrl("https://external.cdn.com/old-image.png");
        when(fileStorageService.saveCustomProductImage(file)).thenReturn("uploads/images/new-custom.png");
        when(fileStorageService.isExternalUrl("https://external.cdn.com/old-image.png")).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProductImage(productCustom, file, null);

        assertThat(result.getImageUrl()).isEqualTo("uploads/images/new-custom.png");
        verify(fileStorageService, never()).deleteProductImage(any());
        verify(productRepository).save(productCustom);
    }

    @Test
    @DisplayName("Updates custom product without previous image")
    void testUpdateProductImageCustomWithoutPreviousImage() {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);

        productCustom.setImageUrl(null);
        when(fileStorageService.saveCustomProductImage(file)).thenReturn("uploads/images/new-custom.png");
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProductImage(productCustom, file, null);

        assertThat(result.getImageUrl()).isEqualTo("uploads/images/new-custom.png");
        verify(fileStorageService, never()).deleteProductImage(any());
        verify(productRepository).save(productCustom);
    }

    @Test
    @DisplayName("Throws when non-custom product has null image URL")
    void testUpdateProductImageNonCustomWithNullUrlThrows() {
        assertThatThrownBy(() -> productService.updateProductImage(productNoCustom, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Non-custom products require an image URL");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Updates non-custom with external URL - does not delete previous external")
    void testUpdateProductImageNonCustomWithExternalUrlDoesNotDelete() {
        productNoCustom.setImageUrl("https://cdn.openfoodfacts.org/old-image.png");
        String newImageUrl = "https://cdn.example.com/new-catalog.png";

        when(fileStorageService.isExternalUrl("https://cdn.openfoodfacts.org/old-image.png")).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProductImage(productNoCustom, null, newImageUrl);

        assertThat(result.getImageUrl()).isEqualTo(newImageUrl);
        verify(fileStorageService, never()).deleteProductImage(any());
        verify(productRepository).save(productNoCustom);
    }

    @Test
    @DisplayName("Updates non-custom without previous image")
    void testUpdateProductImageNonCustomWithoutPreviousImage() {
        productNoCustom.setImageUrl(null);
        String newImageUrl = "https://cdn.example.com/new-catalog.png";

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProductImage(productNoCustom, null, newImageUrl);

        assertThat(result.getImageUrl()).isEqualTo(newImageUrl);
        verify(fileStorageService, never()).deleteProductImage(any());
        verify(productRepository).save(productNoCustom);
    }

    // ==================== searchAllProducts Tests ====================

    @Test
    @DisplayName("Search by barcode returns only catalog product")
    void testSearchAllProductsByBarcode() {
        String barcode = "1234567890123";
        Product catalogProduct = new Product();
        catalogProduct.setBarcode(barcode);
        catalogProduct.setIsCustom(false);

        when(productRepository.findByBarcodeAndIsCustomFalse(barcode))
                .thenReturn(Optional.of(catalogProduct));

        List<Product> results = productService.searchAllProducts(user1.getId(), null, null, barcode);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getBarcode()).isEqualTo(barcode);
        verify(productRepository).findByBarcodeAndIsCustomFalse(barcode);
    }

    @Test
    @DisplayName("Search by barcode returns empty when product not found")
    void testSearchAllProductsByBarcodeNotFound() {
        String barcode = "9999999999999";

        when(productRepository.findByBarcodeAndIsCustomFalse(barcode))
                .thenReturn(Optional.empty());

        List<Product> results = productService.searchAllProducts(user1.getId(), null, null, barcode);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Search by name and brand returns catalog and custom products")
    void testSearchAllProductsByNameAndBrand() {
        String name = "Coca";
        String brand = "Coca-Cola";

        Product catalogProduct = new Product();
        catalogProduct.setName("Coca Cola Original");
        catalogProduct.setBrand("Coca-Cola");
        catalogProduct.setIsCustom(false);

        Product customProduct = new Product();
        customProduct.setName("Coca Cola Custom");
        customProduct.setBrand("Coca-Cola");
        customProduct.setIsCustom(true);
        customProduct.setCreatedBy(user1);

        when(productRepository.findByNameContainingIgnoreCaseAndBrandContainingIgnoreCaseAndIsCustomFalse(name, brand))
                .thenReturn(List.of(catalogProduct));
        when(productRepository.findByIsCustomTrueAndNameContainingIgnoreCaseAndBrandContainingIgnoreCaseAndCreatedById(name, brand, user1.getId()))
                .thenReturn(List.of(customProduct));

        List<Product> results = productService.searchAllProducts(user1.getId(), name, brand, null);

        assertThat(results).hasSize(2);
        assertThat(results).contains(catalogProduct, customProduct);
    }

    @Test
    @DisplayName("Search by name only returns catalog and custom products")
    void testSearchAllProductsByNameOnly() {
        String name = "Coca";

        Product catalogProduct = new Product();
        catalogProduct.setName("Coca Cola Original");
        catalogProduct.setIsCustom(false);

        Product customProduct = new Product();
        customProduct.setName("Coca Cola Custom");
        customProduct.setIsCustom(true);
        customProduct.setCreatedBy(user1);

        when(productRepository.findByNameContainingIgnoreCaseAndIsCustomFalse(name))
                .thenReturn(List.of(catalogProduct));
        when(productRepository.findByIsCustomTrueAndNameContainingIgnoreCaseAndCreatedById(name, user1.getId()))
                .thenReturn(List.of(customProduct));

        List<Product> results = productService.searchAllProducts(user1.getId(), name, null, null);

        assertThat(results).hasSize(2);
        assertThat(results).contains(catalogProduct, customProduct);
    }

    @Test
    @DisplayName("Search by brand only returns catalog and custom products")
    void testSearchAllProductsByBrandOnly() {
        String brand = "Coca-Cola";

        Product catalogProduct = new Product();
        catalogProduct.setBrand("Coca-Cola");
        catalogProduct.setIsCustom(false);

        Product customProduct = new Product();
        customProduct.setBrand("Coca-Cola");
        customProduct.setIsCustom(true);
        customProduct.setCreatedBy(user1);

        when(productRepository.findByBrandContainingIgnoreCaseAndIsCustomFalse(brand))
                .thenReturn(List.of(catalogProduct));
        when(productRepository.findByIsCustomTrueAndBrandContainingIgnoreCaseAndCreatedById(brand, user1.getId()))
                .thenReturn(List.of(customProduct));

        List<Product> results = productService.searchAllProducts(user1.getId(), null, brand, null);

        assertThat(results).hasSize(2);
        assertThat(results).contains(catalogProduct, customProduct);
    }

    @Test
    @DisplayName("Search with no filters returns all catalog and custom products")
    void testSearchAllProductsNoFilters() {
        Product catalogProduct = new Product();
        catalogProduct.setIsCustom(false);

        Product customProduct = new Product();
        customProduct.setIsCustom(true);
        customProduct.setCreatedBy(user1);

        when(productRepository.findByIsCustomFalse())
                .thenReturn(List.of(catalogProduct));
        when(productRepository.findByIsCustomTrueAndCreatedById(user1.getId()))
                .thenReturn(List.of(customProduct));

        List<Product> results = productService.searchAllProducts(user1.getId(), null, null, null);

        assertThat(results).hasSize(2);
        assertThat(results).contains(catalogProduct, customProduct);
    }

    @Test
    @DisplayName("Search with blank barcode ignores barcode and uses other filters")
    void testSearchAllProductsWithBlankBarcode() {
        String name = "Coca";

        Product catalogProduct = new Product();
        catalogProduct.setName("Coca Cola Original");
        catalogProduct.setIsCustom(false);

        when(productRepository.findByNameContainingIgnoreCaseAndIsCustomFalse(name))
                .thenReturn(List.of(catalogProduct));
        when(productRepository.findByIsCustomTrueAndNameContainingIgnoreCaseAndCreatedById(name, user1.getId()))
                .thenReturn(List.of());

        List<Product> results = productService.searchAllProducts(user1.getId(), name, null, "   ");

        assertThat(results).hasSize(1);
        assertThat(results).contains(catalogProduct);
    }

    @Test
    @DisplayName("Search by name (valid) and brand (blank) ignores brand")
    void testSearchAllProductsByNameValidBrandBlank() {
        String name = "Coca";

        Product catalogProduct = new Product();
        catalogProduct.setName("Coca Cola Original");
        catalogProduct.setIsCustom(false);

        when(productRepository.findByNameContainingIgnoreCaseAndIsCustomFalse(name))
                .thenReturn(List.of(catalogProduct));
        when(productRepository.findByIsCustomTrueAndNameContainingIgnoreCaseAndCreatedById(name, user1.getId()))
                .thenReturn(List.of());

        List<Product> results = productService.searchAllProducts(user1.getId(), name, "   ", null);

        assertThat(results).hasSize(1);
        assertThat(results).contains(catalogProduct);
    }

    @Test
    @DisplayName("Search by name (blank) and brand (valid) ignores name")
    void testSearchAllProductsByNameBlankBrandValid() {
        String brand = "Coca-Cola";

        Product catalogProduct = new Product();
        catalogProduct.setBrand("Coca-Cola");
        catalogProduct.setIsCustom(false);

        when(productRepository.findByBrandContainingIgnoreCaseAndIsCustomFalse(brand))
                .thenReturn(List.of(catalogProduct));
        when(productRepository.findByIsCustomTrueAndBrandContainingIgnoreCaseAndCreatedById(brand, user1.getId()))
                .thenReturn(List.of());

        List<Product> results = productService.searchAllProducts(user1.getId(), "   ", brand, null);

        assertThat(results).hasSize(1);
        assertThat(results).contains(catalogProduct);
    }

    @Test
    @DisplayName("Search by name (blank) and brand (blank) returns all products")
    void testSearchAllProductsByNameBlankBrandBlank() {
        Product catalogProduct = new Product();
        catalogProduct.setIsCustom(false);

        Product customProduct = new Product();
        customProduct.setIsCustom(true);
        customProduct.setCreatedBy(user1);

        when(productRepository.findByIsCustomFalse())
                .thenReturn(List.of(catalogProduct));
        when(productRepository.findByIsCustomTrueAndCreatedById(user1.getId()))
                .thenReturn(List.of(customProduct));

        List<Product> results = productService.searchAllProducts(user1.getId(), "   ", "   ", null);

        assertThat(results).hasSize(2);
        assertThat(results).contains(catalogProduct, customProduct);
    }

    @Test
    @DisplayName("Search by name and brand (both blank) returns all products")
    void testSearchAllProductsByNameAndBrandBoth() {
        Product catalogProduct = new Product();
        catalogProduct.setIsCustom(false);

        Product customProduct = new Product();
        customProduct.setIsCustom(true);
        customProduct.setCreatedBy(user1);

        when(productRepository.findByIsCustomFalse())
                .thenReturn(List.of(catalogProduct));
        when(productRepository.findByIsCustomTrueAndCreatedById(user1.getId()))
                .thenReturn(List.of(customProduct));

        List<Product> results = productService.searchAllProducts(user1.getId(), null, "   ", null);

        assertThat(results).hasSize(2);
        assertThat(results).contains(catalogProduct, customProduct);
    }

    // ==================== checkUniqueBarcode Tests ====================

    @Test
    @DisplayName("checkUniqueBarcode - succeeds when barcode does not exist")
    void testCheckUniqueBarcodeSuccess() {
        String barcode = "1111111111111";

        when(productRepository.findByBarcodeAndIsCustomFalse(barcode))
                .thenReturn(Optional.empty());

        productService.checkUniqueBarcode(barcode);

        verify(productRepository).findByBarcodeAndIsCustomFalse(barcode);
    }

    @Test
    @DisplayName("checkUniqueBarcode - throws when barcode exists in catalog")
    void testCheckUniqueBarcodeThrowsWhenExists() {
        String barcode = "9876543210123";

        Product existingProduct = new Product();
        existingProduct.setBarcode(barcode);
        existingProduct.setIsCustom(false);

        when(productRepository.findByBarcodeAndIsCustomFalse(barcode))
                .thenReturn(Optional.of(existingProduct));

        assertThatThrownBy(() -> productService.checkUniqueBarcode(barcode))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A product with this barcode already exists in the catalog.");
    }

    @Test
    @DisplayName("checkUniqueBarcodeCustom - succeeds when barcode does not exist anywhere")
    void testCheckUniqueBarcodeCustomSuccess() throws IOException {
        String barcode = "2222222222222";
        UUID userId = user1.getId();

        when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                .thenReturn(Optional.empty());
        doReturn(Optional.empty()).when(productService).findProductInOpenFoodFacts(barcode);

        productService.checkUniqueBarcodeCustom(barcode, userId);

        verify(productRepository).findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId);
        verify(productService).findProductInOpenFoodFacts(barcode);
    }

    @Test
    @DisplayName("checkUniqueBarcodeCustom - throws when barcode exists in database")
    void testCheckUniqueBarcodeCustomThrowsWhenExistsInDb() {
        String barcode = "3333333333333";
        UUID userId = user1.getId();

        Product existingProduct = new Product();
        existingProduct.setBarcode(barcode);

        when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                .thenReturn(Optional.of(existingProduct));

        assertThatThrownBy(() -> productService.checkUniqueBarcodeCustom(barcode, userId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A product with this barcode already exists.");

        // No debe llamar a la API si ya existe en la base de datos
        verify(productService, never()).findProductInOpenFoodFacts(any());
    }

    @Test
    @DisplayName("checkUniqueBarcodeCustom - throws when barcode exists in Open Food Facts")
    void testCheckUniqueBarcodeCustomThrowsWhenExistsInAPI() throws IOException {
        String barcode = "4444444444444";
        UUID userId = user1.getId();

        Product apiProduct = new Product();
        apiProduct.setBarcode(barcode);
        apiProduct.setIsCustom(false);

        when(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId))
                .thenReturn(Optional.empty());
        doReturn(Optional.of(apiProduct)).when(productService).findProductInOpenFoodFacts(barcode);

        assertThatThrownBy(() -> productService.checkUniqueBarcodeCustom(barcode, userId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A product with this barcode already exists in the external catalog.");

        verify(productRepository).findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId);
        verify(productService).findProductInOpenFoodFacts(barcode);
    }

    // ==================== createProductCustom Tests ====================

    @Test
    @DisplayName("createProductCustom - orchestrates checkUniqueBarcodeCustom and updateProductImage correctly")
    void testCreateProductCustomSuccess() throws IOException {
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

        assertThat(result.getImageUrl()).isEqualTo("uploads/images/custom-new.png");
        assertThat(result.getBarcode()).isEqualTo(barcode);
    }

    @Test
    @DisplayName("createProductCustom - stops execution when checkUniqueBarcodeCustom throws exception")
    void testCreateProductCustomStopsWhenValidationFails() {
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
        assertThatThrownBy(() -> productService.createProductCustom(productCustom, image))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A product with this barcode already exists.");

        // Verificar que updateProductImage NO se ejecutó
        verify(fileStorageService, never()).saveCustomProductImage(any());
        verify(productRepository, never()).save(any(Product.class));
    }

    // ==================== createProductOpenFoodFacts Tests ====================

    @Test
    @DisplayName("createProductOpenFoodFacts - orchestrates checkUniqueBarcode, findProductInOpenFoodFacts and updateProductImage correctly")
    void testCreateProductOpenFoodFactsSuccess() throws IOException {
        String barcode = "7777777777777";
        UUID userId = user1.getId();

        Product apiProduct = new Product();
        apiProduct.setBarcode(barcode);
        apiProduct.setName("Producto API");
        apiProduct.setBrand("Marca API");
        apiProduct.setImageUrl("https://api.example.com/image.png");
        apiProduct.setIsCustom(false);

        // Mock checkUniqueBarcode (no lanza excepción)
        when(productRepository.findByBarcodeAndIsCustomFalse(barcode))
                .thenReturn(Optional.empty());

        // Mock findProductInOpenFoodFacts
        doReturn(Optional.of(apiProduct)).when(productService).findProductInOpenFoodFacts(barcode);

        // Mock updateProductImage
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.createProductOpenFoodFacts(barcode, userId);

        // Verificar que se llamaron los métodos en orden
        verify(productRepository).findByBarcodeAndIsCustomFalse(barcode);
        verify(productService).findProductInOpenFoodFacts(barcode);
        verify(productRepository).save(apiProduct);

        // Verificar resultado
        assertThat(result.getBarcode()).isEqualTo(barcode);
        assertThat(result.getIsCustom()).isFalse();
        assertThat(result.getImageUrl()).isEqualTo("https://api.example.com/image.png");
    }

    @Test
    @DisplayName("createProductOpenFoodFacts - stops execution when checkUniqueBarcode throws exception")
    void testCreateProductOpenFoodFactsStopsWhenBarcodeExists() {
        String barcode = "8888888888888";
        UUID userId = user1.getId();

        // Simular que el barcode ya existe en catálogo
        Product existingProduct = new Product();
        existingProduct.setBarcode(barcode);
        existingProduct.setIsCustom(false);

        when(productRepository.findByBarcodeAndIsCustomFalse(barcode))
                .thenReturn(Optional.of(existingProduct));

        // Debe lanzar excepción de checkUniqueBarcode
        assertThatThrownBy(() -> productService.createProductOpenFoodFacts(barcode, userId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A product with this barcode already exists in the catalog.");

        // Verificar que findProductInOpenFoodFacts y updateProductImage NO se ejecutaron
        verify(productService, never()).findProductInOpenFoodFacts(any());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("createProductOpenFoodFacts - throws ResourceNotFoundException when product not found in API")
    void testCreateProductOpenFoodFactsThrowsWhenNotFoundInAPI() throws IOException {
        String barcode = "9999999999999";
        UUID userId = user1.getId();

        when(productRepository.findByBarcodeAndIsCustomFalse(barcode))
                .thenReturn(Optional.empty());

        doReturn(Optional.empty()).when(productService).findProductInOpenFoodFacts(barcode);

        assertThatThrownBy(() -> productService.createProductOpenFoodFacts(barcode, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product with barcode " + barcode + " not found in Open Food Facts external catalog. Consider creating it as a custom product.");

        verify(productRepository, never()).save(any(Product.class));
    }

    // ==================== validateBarcodes Tests ====================

    @Test
    @DisplayName("validateBarcodes - returns empty lists when input is empty")
    void testValidateBarcodesEmptyList() {
        List<String> barcodes = List.of();
        UUID userId = user1.getId();

        BatchValidationResponse result = productService.validateBarcodes(barcodes, userId);

        assertThat(result.valid()).isEmpty();
        assertThat(result.notFound()).isEmpty();
    }

    @Test
    @DisplayName("validateBarcodes - correctly classifies mixed scenarios")
    void testValidateBarcodesMixedScenarios() throws IOException {
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

        assertThat(result.valid()).containsExactlyInAnyOrder("1111111111111", "3333333333333");
        assertThat(result.notFound()).containsExactly("5555555555555");

        // Verificar que solo se llamó a la API para los que no estaban en BD
        verify(productService, never()).findProductInOpenFoodFacts("1111111111111");
        verify(productService).findProductInOpenFoodFacts("3333333333333");
        verify(productService).findProductInOpenFoodFacts("5555555555555");
    }

}
