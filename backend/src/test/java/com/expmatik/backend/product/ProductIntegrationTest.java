package com.expmatik.backend.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProductIntegrationTest {

    private static final Path UPLOAD_IMAGES_DIR = Path.of("uploads", "images");
    private Set<String> filesBeforeTest = new HashSet<>();

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void snapshotUploadsImages() throws IOException {
        filesBeforeTest = listImageFileNames();
    }

    @AfterEach
    void cleanupNewUploadedImages() throws IOException {
        Set<String> filesAfterTest = listImageFileNames();
        filesAfterTest.removeAll(filesBeforeTest);

        for (String newFileName : filesAfterTest) {
            Files.deleteIfExists(UPLOAD_IMAGES_DIR.resolve(newFileName));
        }
    }

    private Set<String> listImageFileNames() throws IOException {
        if (!Files.exists(UPLOAD_IMAGES_DIR)) {
            return new HashSet<>();
        }

        try (Stream<Path> files = Files.list(UPLOAD_IMAGES_DIR)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        }
    }
    
    // == Pruebas para GET /api/products/{id} ==

    @Nested
    @DisplayName("Tests for GET /api/products/{id} endpoint")
    class GetProductByIdTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should return product details when product exists and user is authorized")
            @WithUserDetails("admin@expmatik.com")
            void testGetNonCustomProductById_ValidId_ReturnsProduct() throws Exception {
                UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                
                mockMvc.perform(get("/api/products/" + productId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(productId.toString()))
                        .andExpect(jsonPath("$.name").value("Leche Entera"))
                        .andExpect(jsonPath("$.brand").value("Puleva"))
                        .andExpect(jsonPath("$.description").value("Leche entera de vaca"));
            }

            @Test
            @DisplayName("Should return non custom product details for custom product when user is Maintainer")
            @WithUserDetails("repo@expmatik.com")
            void testGetNonCustomProductById_Maintainer_ReturnsProduct() throws Exception {
                UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                
                mockMvc.perform(get("/api/products/" + productId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(productId.toString()))
                        .andExpect(jsonPath("$.name").value("Leche Entera"))
                        .andExpect(jsonPath("$.brand").value("Puleva"))
                        .andExpect(jsonPath("$.description").value("Leche entera de vaca"));
            }

            @Test
            @DisplayName("Should return product details for custom product when user is Maintainer")
            @WithUserDetails("repo@expmatik.com")
            void testGetCustomProductById_Maintainer_ReturnsProduct() throws Exception {
                UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

                mockMvc.perform(get("/api/products/" + productId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(productId.toString()))
                        .andExpect(jsonPath("$.name").value("ProductoPersonalizado"))
                        .andExpect(jsonPath("$.brand").value("ProductoPersonalizado"))
                        .andExpect(jsonPath("$.description").value("Producto personalizado de prueba"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return product details for custom product when user is Admin")
            void testGetCustomProductById_ValidId_ReturnsProduct() throws Exception {
                UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

                mockMvc.perform(get("/api/products/" + productId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(productId.toString()))
                        .andExpect(jsonPath("$.name").value("ProductoPersonalizado"))
                        .andExpect(jsonPath("$.brand").value("ProductoPersonalizado"))
                        .andExpect(jsonPath("$.description").value("Producto personalizado de prueba"));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return 404 when product does not exist")
            void testGetCustomProductById_InvalidId_ReturnsNotFound() throws Exception {
                UUID productId = UUID.fromString("99900000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/products/" + productId))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.message").value("Product not found with id: " + productId));
            }


            @Test
            @WithUserDetails("admin2@expmatik.com")
            @DisplayName("Should return 403 when product exists but user is not the owner")
            void testGetCustomProductById_NotOwnedByUser_ReturnsForbidden() throws Exception {
                UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

                mockMvc.perform(get("/api/products/" + productId))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$").value("You are not authorized to view this product."));
            }
        }
    }

    // == Pruebas para GET /api/products/custom ==

    @Nested
    @DisplayName("Tests for GET /api/products/custom endpoint")
    class GetCustomProductsByUserIdTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return custom products for valid user ID")
            void testGetCustomProductsByUserId_ValidRequest_ReturnsProducts() throws Exception {
                mockMvc.perform(get("/api/products/custom"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(1))
                        .andExpect(jsonPath("$[0].name").value("ProductoPersonalizado"))
                        .andExpect(jsonPath("$[0].brand").value("ProductoPersonalizado"))
                        .andExpect(jsonPath("$[0].description").value("Producto personalizado de prueba"));
            }

            @Test
            @WithUserDetails("admin2@expmatik.com")
            @DisplayName("Should return empty list when user has no custom products")
            void testGetAllCustomProductsByUserId_ValidRequest_ReturnsEmptyList() throws Exception {
                mockMvc.perform(get("/api/products/custom"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(0));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("Should return 403 when user is not authorized to view custom products")
            void testGetCustomProductsByUserId_Maintainer_ReturnsForbidden() throws Exception {
                mockMvc.perform(get("/api/products/custom"))
                        .andExpect(status().isForbidden());
            }
        }
    }

    // == Pruebas para GET /api/products/non-custom ==

    @Nested
    @DisplayName("Tests for GET /api/products/non-custom endpoint")
    class GetNonCustomProductsTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return non-custom products for valid user ID")
            void testGetAllNonCustomProducts() throws Exception {
                mockMvc.perform(get("/api/products/non-custom"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(4))
                        .andExpect(jsonPath("$[0].name").value("Leche Entera"))
                        .andExpect(jsonPath("$[1].name").value("Pan de Molde"))
                        .andExpect(jsonPath("$[2].name").value("Yogur Natural"))
                        .andExpect(jsonPath("$[3].name").value("Requiere actualización"));  
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("Should return 403 when user is not authorized to view non-custom products")
            void testGetAllNonCustomProducts_AsMaintainer_ReturnsForbidden() throws Exception {
                mockMvc.perform(get("/api/products/non-custom"))
                        .andExpect(status().isForbidden());
            }
        }
    }

    // == Pruebas para Get /api/products ==

    @Nested
    @DisplayName("Tests for GET /api/products endpoint")
    class SearchProductsTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return matching products when searching by name")
            void testSearchProducts_ByName_ShouldReturnMatchingProducts() throws Exception {
                String searchTerm = "Leche";
                mockMvc.perform(get("/api/products").param("name", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].name").value("Leche Entera"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return matching products when searching by brand")
            void testSearchProducts_ByBrand_ShouldReturnMatchingProducts() throws Exception {
                String searchTerm = "Bimbo";
                mockMvc.perform(get("/api/products").param("brand", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].brand").value("Bimbo"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return matching products when searching by name and brand")
            void testSearchProducts_ByNameAndBrand_ShouldReturnMatchingProducts() throws Exception {
                String nameSearchTerm = "Yogur";
                String brandSearchTerm = "Danone";
                mockMvc.perform(get("/api/products")
                        .param("name", nameSearchTerm)
                        .param("brand", brandSearchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].name").value("Yogur Natural"))
                        .andExpect(jsonPath("$.content[0].brand").value("Danone"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return empty list when no products match the search criteria")
            void testSearchProducts_NoMatches_ShouldReturnEmptyList() throws Exception {
                String searchTerm = "NonExistentProduct";
                mockMvc.perform(get("/api/products").param("name", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(0))
                        .andExpect(jsonPath("$.totalElements").value(0));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return all products when search term is empty")
            void testSearchProducts_EmptySearchTerm_ShouldReturnAllProducts() throws Exception {
                mockMvc.perform(get("/api/products").param("name", ""))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(5));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return empty list when search term contains special characters")
            void testSearchProducts_SpecialCharacters_ShouldReturnEmptyList() throws Exception {
                String searchTerm = "Leche Entera!";
                mockMvc.perform(get("/api/products").param("name", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(0));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return matching products when searching with different case")
            void testSearchProducts_CaseInsensitive_ShouldReturnMatchingProducts() throws Exception {
                String searchTerm = "leche entera";
                mockMvc.perform(get("/api/products").param("name", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].name").value("Leche Entera"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return matching products when searching for a partial match")
            void testSearchProducts_PartialMatch_ShouldReturnMatchingProducts() throws Exception {
                String searchTerm = "Leche";
                mockMvc.perform(get("/api/products").param("name", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].name").value("Leche Entera"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return matching products when searching by barcode")
            void testSearchProducts_ByBarcode_ShouldReturnMatchingProduct() throws Exception {
                String searchTerm = "20000001";
                mockMvc.perform(get("/api/products").param("barcode", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].name").value("Leche Entera"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Test pagination parameters")
            void testSearchProducts_Pagination_ShouldReturnPagedResults() throws Exception {
                mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "2"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2))
                        .andExpect(jsonPath("$.size").value(2))
                        .andExpect(jsonPath("$.number").value(0));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("Should return 403 when user is not authorized to search products")
            void testSearchProducts_AsMaintainer_ReturnsForbidden() throws Exception {
                mockMvc.perform(get("/api/products").param("name", "Leche"))
                        .andExpect(status().isForbidden());
            }
        }
    }

    // == Pruebas para POST /api/products/custom ==

    @Nested
    @DisplayName("Tests for POST /api/products/custom endpoint")
    class CreateCustomProductTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should create a custom product with valid inputs")
            void testCreateCustomProduct_ValidInputs_ShouldCreateProduct() throws Exception {
                String name = "Producto Personalizado de Test";
                String brand = "Marca de Test";
                String description = "Descripción del producto personalizado de test";

                String barcode = String.format("%08d", Math.abs((int) (System.nanoTime() % 100_000_000L)));
                Boolean isPerishable = false;

                BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
                ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
                ImageIO.write(image, "png", imageOut);
                byte[] pngBytes = imageOut.toByteArray();
                MockMultipartFile file = new MockMultipartFile("file", "test_image.png", "image/png", pngBytes);

                mockMvc.perform(multipart("/api/products/custom")
                    .file(file)
                        .param("name", name)
                        .param("brand", brand)
                        .param("description", description)
                        .param("isPerishable", isPerishable.toString())
                        .param("barcode", barcode))
                    .andExpect(status().isOk())
                        .andExpect(jsonPath("$.name").value(name))
                        .andExpect(jsonPath("$.brand").value(brand))
                        .andExpect(jsonPath("$.description").value(description))
                        .andExpect(jsonPath("$.isPerishable").value(isPerishable))
                        .andExpect(jsonPath("$.barcode").value(barcode));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return 400 when file type is not allowed")
            void testCreateCustomProduct_InvalidFileType_ShouldReturnBadRequest() throws Exception {
                String name = "Producto Personalizado de Test";
                String brand = "Marca de Test";
                String description = "Descripción del producto personalizado de test";
                String barcode = String.format("%08d", Math.abs((int) (System.nanoTime() % 100_000_000L)));
                Boolean isPerishable = false;

                MockMultipartFile file = new MockMultipartFile("file", "test_image.txt", "text/plain", "This is not an image".getBytes());

                mockMvc.perform(multipart("/api/products/custom")
                    .file(file)
                        .param("name", name)
                        .param("brand", brand)
                        .param("description", description)
                        .param("isPerishable", isPerishable.toString())
                        .param("barcode", barcode))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("File type is not allowed. Allowed types: jpg, jpeg, png"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return 400 when the image file is invalid")
            void testCreateCustomProduct_WithInvalidImage_ShouldReturnBadRequest() throws Exception {
                String name = "Producto Personalizado de Test";
                String brand = "Marca de Test";
                String description = "Descripción del producto personalizado de test";
                String barcode = String.format("%08d", Math.abs((int) (System.nanoTime() % 100_000_000L)));
                Boolean isPerishable = false;

                MockMultipartFile file = new MockMultipartFile("file", "test_image.jpg", "image/jpeg", new byte[0]);

                mockMvc.perform(multipart("/api/products/custom")
                    .file(file)
                        .param("name", name)
                        .param("brand", brand)
                        .param("description", description)
                        .param("isPerishable", isPerishable.toString())
                        .param("barcode", barcode))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Custom products require an image file"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return 409 when a product with the same barcode already exists in the catalog")
            void testCreateCustomProduct_WithBarcodeAlreadyExistsInCatalog_ShouldReturnConflict() throws Exception {
                String name = "Producto Personalizado de Test";
                String brand = "Marca de Test";
                String description = "Descripción del producto personalizado de test";
                String barcode = "20000000";
                Boolean isPerishable = false;

                BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
                ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
                ImageIO.write(image, "png", imageOut);
                byte[] pngBytes = imageOut.toByteArray();
                MockMultipartFile file = new MockMultipartFile("file", "test_image.png", "image/png", pngBytes);

                mockMvc.perform(multipart("/api/products/custom")
                    .file(file)
                        .param("name", name)
                        .param("brand", brand)
                        .param("description", description)
                        .param("isPerishable", isPerishable.toString())
                        .param("barcode", barcode))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("A product with this barcode already exists."));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return 409 when a product with the same barcode already exists in the external catalog")
            void testCreateCustomProduct_WithBarcodeAlreadyExistsInExternalCatalog_ShouldReturnConflict() throws Exception {
                String name = "Producto Personalizado de Test";
                String brand = "Marca de Test";
                String description = "Descripción del producto personalizado de test";
                String barcode = "20000001";
                Boolean isPerishable = false;

                BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
                ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
                ImageIO.write(image, "png", imageOut);
                byte[] pngBytes = imageOut.toByteArray();
                MockMultipartFile file = new MockMultipartFile("file", "test_image.png", "image/png", pngBytes);

                mockMvc.perform(multipart("/api/products/custom")
                    .file(file)
                        .param("name", name)
                        .param("brand", brand)
                        .param("description", description)
                        .param("isPerishable", isPerishable.toString())
                        .param("barcode", barcode))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("A product with this barcode already exists."));
            }

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("Should return 403 when user is not authorized to create custom products")
            void testCreateCustomProduct_AsMaintainer_ShouldReturnForbidden() throws Exception {
                String name = "Producto Personalizado de Test";
                String brand = "Marca de Test";
                String description = "Descripción del producto personalizado de test";
                String barcode = String.format("%08d", Math.abs((int) (System.nanoTime() % 100_000_000L)));
                Boolean isPerishable = false;

                BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
                ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
                ImageIO.write(image, "png", imageOut);
                byte[] pngBytes = imageOut.toByteArray();
                MockMultipartFile file = new MockMultipartFile("file", "test_image.png", "image/png", pngBytes);

                mockMvc.perform(multipart("/api/products/custom")
                    .file(file)
                        .param("name", name)
                        .param("brand", brand)
                        .param("description", description)
                        .param("isPerishable", isPerishable.toString())
                        .param("barcode", barcode))
                    .andExpect(status().isForbidden());
            }
        }
    }

    // == Pruebas para POST /api/products/non-custom ==

    @Nested
    @DisplayName("Tests for POST /api/products/non-custom endpoint")
    class CreateNonCustomProductTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should create a non-custom product with valid barcode that exists in external catalog")
            void testCreateNonCustomProduct_ValidBarcode_ShouldReturnOk() throws Exception {
                String barcode = "4716982022201";
                Thread.sleep(2000);

                mockMvc.perform(post("/api/products/non-custom")
                        .param("barcode", barcode))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.barcode").value(barcode))
                        .andExpect(jsonPath("$.name").value("Choco Bom"))
                        .andExpect(jsonPath("$.brand").value("Gullón"))
                        .andExpect(jsonPath("$.isCustom").value(false))
                        .andExpect(jsonPath("$.imageUrl").exists());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should create a non-custom product with valid barcode that exists in external catalog but has no brand info, should set brand to 'Unknown'")
            void testCreateNonCustomProduct_UnknownBrand_ShouldReturnOk() throws Exception {
                String barcode = "5000112556780";
                Thread.sleep(2000);

                mockMvc.perform(post("/api/products/non-custom")
                        .param("barcode", barcode))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.barcode").value(barcode))
                        .andExpect(jsonPath("$.brand").value("Unknown"))
                        .andExpect(jsonPath("$.isCustom").value(false))
                        .andExpect(jsonPath("$.imageUrl").exists());
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return 409 when a product with the same barcode already exists")
            void testCreateNonCustomProduct_DuplicateBarcode_ShouldReturnConflict() throws Exception {
                String barcode = "20000001";

                mockMvc.perform(post("/api/products/non-custom")
                        .param("barcode", barcode))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("A product with this barcode already exists."));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return 404 when barcode does not exist in external catalog")
            void testCreateNonCustomProduct_BarcodeDoesNotExistInExternalCatalog_ShouldReturnNotFound() throws Exception {
                String barcode = "20000010";

                mockMvc.perform(post("/api/products/non-custom")
                        .param("barcode", barcode))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.message").value("Product with barcode " + barcode + " not found in Open Food Facts external catalog. Consider creating it as a custom product."));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return 400 when barcode format is invalid")
            void testCreateNonCustomProduct_InvalidBarcode_ShouldReturnBadRequest() throws Exception {
                String[] invalidBarcodes = {
                    "",
                    "abc12345",
                    "1234567",
                    "123456789012",
                    "12345678901234"
                };

                for (String invalidBarcode : invalidBarcodes) {
                    mockMvc.perform(post("/api/products/non-custom")
                            .param("barcode", invalidBarcode))
                            .andExpect(status().isBadRequest())
                            .andExpect(jsonPath("$.message").value("Barcode must be 8 or 13 digits"));
                }
            }

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("Should return 403 when user is not authorized to create custom products")
            void testCreateNonCustomProduct_AsMaintainer_ShouldReturnForbidden() throws Exception {
                String barcode = "4716982022201";
                Thread.sleep(2000);

                mockMvc.perform(post("/api/products/non-custom")
                        .param("barcode", barcode))
                        .andExpect(status().isForbidden());
            }
        }
    }

    // == Pruebas para GET /api/products/openfoodfacts/{barcode} ==

    @Nested
    @DisplayName("Tests for GET /api/products/openfoodfacts/{barcode} endpoint")
    class GetProductFromOpenFoodFactsTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return product details from Open Food Facts for valid barcode")
            void testGetProductFromOpenFoodFacts_ValidBarcode_ShouldReturnProduct() throws Exception {
                String barcode = "4716982022201";
                Thread.sleep(5000);

                mockMvc.perform(get("/api/products/openfoodfacts/" + barcode))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.barcode").value(barcode))
                        .andExpect(jsonPath("$.name").value("Choco Bom"))
                        .andExpect(jsonPath("$.brand").value("Gullón"))
                        .andExpect(jsonPath("$.isCustom").value(false))
                        .andExpect(jsonPath("$.imageUrl").exists());
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return 404 when barcode does not exist in Open Food Facts")
            void testGetProductFromOpenFoodFacts_InvalidBarcode_ShouldReturnNotFound() throws Exception {
                String barcode = "20000010";

                mockMvc.perform(get("/api/products/openfoodfacts/" + barcode))
                        .andExpect(status().isNotFound());
            }

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("Should return 403 when user is not authorized to access Open Food Facts product details")
            void testGetProductFromOpenFoodFacts_AsMaintainer_ShouldReturnForbidden() throws Exception {
                String barcode = "20000010";

                mockMvc.perform(get("/api/products/openfoodfacts/" + barcode))
                        .andExpect(status().isForbidden());
            }
        }
    }

    // == Pruebas para GET /api/products/validate-barcode/{barcode} ==

    @Nested
    @DisplayName("Tests for GET /api/products/validate-barcode/{barcode} endpoint")
    class ValidateProductBarcodeTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
    
            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return true when barcode exists in the catalog")
            void testValidateProduct_ExistingInCatalog_ShouldReturnTrue() throws Exception {
                String barcode = "20000001";

                mockMvc.perform(get("/api/products/validate-barcode/" + barcode))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").value(true));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return true when barcode exists in external catalog")
            void testValidateProduct_ExistingInExternalCatalog_ShouldReturnTrue() throws Exception {
                String barcode = "7622300281182";
                Thread.sleep(2000);

                mockMvc.perform(get("/api/products/validate-barcode/" + barcode))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").value(true));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return false when barcode does not exist in any catalog")
            void testValidateProduct_NotExistingInAnyCatalog_ShouldReturnFalse() throws Exception {
                String barcode = "20000010";

                mockMvc.perform(get("/api/products/validate-barcode/" + barcode))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").value(false));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("Should return 403 when user is not authorized to validate barcodes")
            void testValidateProduct_AsMaintainer_ShouldReturnForbidden() throws Exception {
                String barcode = "20000001";

                mockMvc.perform(get("/api/products/validate-barcode/" + barcode))
                        .andExpect(status().isForbidden());
            }
        }
    }

    // == Test de /api/products/without-info == //

    @Nested
    @DisplayName("Tests for GET /api/products/without-info endpoint")
    class SearchProductsWithoutInfoTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return products without info when searching by name")  
            void testSearchProductsWithoutInfo_ByName_ShouldReturnProducts() throws Exception {
                String searchTerm = "Leche";
                mockMvc.perform(get("/api/products/without-info")
                        .param("name", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(0));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return products without info when searching by brand")
            void testSearchProductsWithoutInfo_ByBrand_ShouldReturnProducts() throws Exception {
                String searchTerm = "Bimbo";
                mockMvc.perform(get("/api/products/without-info")
                        .param("brand", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].brand").value("Bimbo"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return products without info when searching by name and brand")
            void testSearchProductsWithoutInfo_ByNameAndBrand_ShouldReturnProducts() throws Exception {
                String nameSearchTerm = "Yogur";
                String brandSearchTerm = "Danone";

                mockMvc.perform(get("/api/products/without-info")
                        .param("name", nameSearchTerm)
                        .param("brand", brandSearchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].name").value("Yogur Natural"))
                        .andExpect(jsonPath("$.content[0].brand").value("Danone"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return no products when searching with no matches")
            void testSearchProductsWithoutInfo_NoMatches_ShouldReturnNoProducts() throws Exception {
                String searchTerm = "NonExistentProduct";

                mockMvc.perform(get("/api/products/without-info")
                        .param("name", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(0))
                        .andExpect(jsonPath("$.totalElements").value(0));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return all products when search term is empty")
            void testSearchProductsWithoutInfo_EmptySearchTerm_ShouldReturnAllProductsWithoutInfo() throws Exception {
                mockMvc.perform(get("/api/products/without-info")
                        .param("name", ""))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should handle special characters in search term")
            void testSearchProductsWithoutInfo_SpecialCharacters_ShouldReturnNoProducts() throws Exception {
                String searchTerm = "Leche Entera!";

                mockMvc.perform(get("/api/products/without-info")
                        .param("name", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(0));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should perform case-insensitive search")
            void testSearchProductsWithoutInfo_CaseInsensitive_ShouldReturnProducts() throws Exception {
                String searchTerm = "pan de molde";

                mockMvc.perform(get("/api/products/without-info")
                        .param("name", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].name").value("Pan de Molde"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return products matching the search term")
            void testSearchProductsWithoutInfo_PartialMatch_ShouldReturnProducts() throws Exception {
                String searchTerm = "Molde";

                mockMvc.perform(get("/api/products/without-info")
                        .param("name", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].name").value("Pan de Molde"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Should return product by barcode")
            void testSearchProductsWithoutInfo_ByBarcode_ShouldReturnProduct() throws Exception {
                String searchTerm = "20000002";

                mockMvc.perform(get("/api/products/without-info")
                        .param("barcode", searchTerm))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].name").value("Pan de Molde"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("Test pagination parameters for products without info")
            void testSearchProductsWithoutInfo_Pagination_ShouldReturnProducts() throws Exception {
                mockMvc.perform(get("/api/products/without-info")
                        .param("page", "0")
                        .param("size", "2"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2))
                        .andExpect(jsonPath("$.size").value(2))
                        .andExpect(jsonPath("$.number").value(0));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("Should return 403 when user is not authorized to search products without info")
            void testSearchProductsWithoutInfo_AsMaintainer_ShouldReturnForbidden() throws Exception {
                mockMvc.perform(get("/api/products/without-info")
                        .param("name", "Leche"))
                        .andExpect(status().isForbidden());
            }
        }
    }
}
