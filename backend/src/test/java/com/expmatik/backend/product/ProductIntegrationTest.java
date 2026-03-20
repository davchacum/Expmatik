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

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetNonCustomProductById() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("Leche Entera"))
                .andExpect(jsonPath("$.brand").value("Puleva"))
                .andExpect(jsonPath("$.description").value("Leche entera de vaca"));
    }

    @Test
    @WithUserDetails("repo@expmatik.com")
    void testGetNonCustomProductById_Maintainer() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("Leche Entera"))
                .andExpect(jsonPath("$.brand").value("Puleva"))
                .andExpect(jsonPath("$.description").value("Leche entera de vaca"));
    }

    @Test
    @WithUserDetails("repo@expmatik.com")
    void testGetCustomProductById_Maintainer() throws Exception {
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
    void testGetCustomProductById() throws Exception {
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
    void testGetCustomProductById_NotFound() throws Exception {
        UUID productId = UUID.fromString("99900000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: " + productId));
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testGetCustomProductById_Unauthorized() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("You are not authorized to view this product."));
    }

    // == Pruebas para GET /api/products/custom ==

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetCustomProductsByUserId() throws Exception {
        mockMvc.perform(get("/api/products/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("ProductoPersonalizado"))
                .andExpect(jsonPath("$[0].brand").value("ProductoPersonalizado"))
                .andExpect(jsonPath("$[0].description").value("Producto personalizado de prueba"));
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testGetAllCustomProductsByUserId_NoProducts() throws Exception {
        mockMvc.perform(get("/api/products/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // == Pruebas para GET /api/products/non-custom ==

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetAllNonCustomProducts() throws Exception {
        mockMvc.perform(get("/api/products/non-custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Leche Entera"))
                .andExpect(jsonPath("$[1].name").value("Pan de Molde"))
                .andExpect(jsonPath("$[2].name").value("Yogur Natural"));  
    }

    // == Pruebas para Get /api/products ==

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProductsByName() throws Exception {
        String searchTerm = "Leche";
        mockMvc.perform(get("/api/products").param("name", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Leche Entera"));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProductsByBrand() throws Exception {
        String searchTerm = "Bimbo";
        mockMvc.perform(get("/api/products").param("brand", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].brand").value("Bimbo"));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProductsByNameAndBrand() throws Exception {
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
    void testSearchProducts_NoMatches() throws Exception {
        String searchTerm = "NonExistentProduct";
        mockMvc.perform(get("/api/products").param("name", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProducts_EmptySearchTerm() throws Exception {
        mockMvc.perform(get("/api/products").param("name", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(4));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProducts_SpecialCharacters() throws Exception {
        String searchTerm = "Leche Entera!";
        mockMvc.perform(get("/api/products").param("name", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProducts_CaseInsensitive() throws Exception {
        String searchTerm = "leche entera";
        mockMvc.perform(get("/api/products").param("name", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Leche Entera"));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProducts_PartialMatch() throws Exception {
        String searchTerm = "Leche";
        mockMvc.perform(get("/api/products").param("name", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Leche Entera"));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProducts_ByBarcode() throws Exception {
        String searchTerm = "20000001";
        mockMvc.perform(get("/api/products").param("barcode", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Leche Entera"));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    @DisplayName("Test pagination parameters")
    void testSearchProducts_Pagination() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("page", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0));
    }

    // == Pruebas para POST /api/products/custom ==

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateCustomProduct() throws Exception {
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


    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateCustomProduct_InvalidFileType() throws Exception {
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
    void testCreateCustomProduct_WithInvalidImage() throws Exception {
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
    void testCreateCustomProduct_WithBarcodeAlreadyExistsInCatalog() throws Exception {
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
    void testCreateCustomProduct_WithBarcodeAlreadyExistsInExternalCatalog() throws Exception {
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

    // == Pruebas para POST /api/products/non-custom ==

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateNonCustomProduct() throws Exception {
        String barcode = "4716982022201";

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
    void testCreateNonCustomProduct_UnknownBrand() throws Exception {
        String barcode = "5000112556780";

        mockMvc.perform(post("/api/products/non-custom")
                .param("barcode", barcode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.barcode").value(barcode))
                .andExpect(jsonPath("$.brand").value("Unknown"))
                .andExpect(jsonPath("$.isCustom").value(false))
                .andExpect(jsonPath("$.imageUrl").exists());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateNonCustomProduct_DuplicateBarcode() throws Exception {
        String barcode = "20000001";

        mockMvc.perform(post("/api/products/non-custom")
                .param("barcode", barcode))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A product with this barcode already exists."));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateNonCustomProduct_BarcodeDoesNotExistInExternalCatalog() throws Exception {
        String barcode = "20000010";

        mockMvc.perform(post("/api/products/non-custom")
                .param("barcode", barcode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product with barcode " + barcode + " not found in Open Food Facts external catalog. Consider creating it as a custom product."));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateNonCustomProduct_InvalidBarcode() throws Exception {
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

    // == Pruebas para GET /api/products/openfoodfacts/{barcode} ==

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetProductFromOpenFoodFacts() throws Exception {
        String barcode = "4716982022201";

        mockMvc.perform(get("/api/products/openfoodfacts/" + barcode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value(barcode))
                .andExpect(jsonPath("$.name").value("Choco Bom"))
                .andExpect(jsonPath("$.brand").value("Gullón"))
                .andExpect(jsonPath("$.isCustom").value(false))
                .andExpect(jsonPath("$.imageUrl").exists());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetProductFromOpenFoodFacts_NotFound() throws Exception {
        String barcode = "20000010";

        mockMvc.perform(get("/api/products/openfoodfacts/" + barcode))
                .andExpect(status().isNotFound());
    }

    // == Pruebas para GET /api/products/validate-barcode/{barcode} ==
    
    @Test
    @WithUserDetails("admin@expmatik.com")
    void testValidateProduct_ExistingInCatalog() throws Exception {
        String barcode = "20000001";

        mockMvc.perform(get("/api/products/validate-barcode/" + barcode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testValidateProduct_ExistingInExternalCatalog() throws Exception {
        String barcode = "4716982022201";

        mockMvc.perform(get("/api/products/validate-barcode/" + barcode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testValidateProduct_NotExistingInAnyCatalog() throws Exception {
        String barcode = "20000010";

        mockMvc.perform(get("/api/products/validate-barcode/" + barcode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    // == Test de /api/products/without-info == //

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProductsWithoutInfoByName() throws Exception {
        String searchTerm = "Leche";
        mockMvc.perform(get("/api/products/without-info")
                .param("name", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProductsWithoutInfoByBrand() throws Exception {
        String searchTerm = "Bimbo";
        mockMvc.perform(get("/api/products/without-info")
                .param("brand", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].brand").value("Bimbo"));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProductsWithoutInfoByNameAndBrand() throws Exception {
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
    void testSearchProductsWithoutInfo_NoMatches() throws Exception {
        String searchTerm = "NonExistentProduct";

        mockMvc.perform(get("/api/products/without-info")
                .param("name", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProductsWithoutInfo_EmptySearchTerm() throws Exception {
        mockMvc.perform(get("/api/products/without-info")
                .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProductsWithoutInfo_SpecialCharacters() throws Exception {
        String searchTerm = "Leche Entera!";

        mockMvc.perform(get("/api/products/without-info")
                .param("name", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProductsWithoutInfo_CaseInsensitive() throws Exception {
        String searchTerm = "pan de molde";

        mockMvc.perform(get("/api/products/without-info")
                .param("name", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Pan de Molde"));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProductsWithoutInfo_PartialMatch() throws Exception {
        String searchTerm = "Molde";

        mockMvc.perform(get("/api/products/without-info")
                .param("name", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Pan de Molde"));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchProductsWithoutInfo_ByBarcode() throws Exception {
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
    void testSearchProductsWithoutInfo_Pagination() throws Exception {
        mockMvc.perform(get("/api/products/without-info")
                .param("page", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0));
    }


}
