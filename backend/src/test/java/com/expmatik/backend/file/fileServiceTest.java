package com.expmatik.backend.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.FileSizeExceededException;

@ExtendWith(MockitoExtension.class)
public class fileServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    private byte[] validImageBytes;
    private byte[] smallImageBytes;
    private byte[] largeImageBytes;

    @BeforeEach
    public void setUp() throws IOException {
        fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());

        try (InputStream smallImageStream = getClass().getResourceAsStream("/product/-2MB.png");
             InputStream largeImageStream = getClass().getResourceAsStream("/product/+2MB.jpg")) {
            
            if (smallImageStream == null || largeImageStream == null) {
                throw new IOException("Test image files not found in resources/product/");
            }
            
            smallImageBytes = smallImageStream.readAllBytes();
            largeImageBytes = largeImageStream.readAllBytes();
        }
        
        validImageBytes = smallImageBytes;
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                    }
                });
        }
    }

    // ==================== saveCustomProductImage Tests ====================

    @Test
    @DisplayName("Should save custom product image successfully")
    void testSaveCustomProductImage_Success() throws IOException {
        MultipartFile validImageFile = createMockMultipartFile("test.jpg", "image/jpeg", validImageBytes);
        String result = fileStorageService.saveCustomProductImage(validImageFile);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("/uploads/images/");
        assertThat(result).endsWith(".jpg");

        String fileName = result.substring(result.lastIndexOf("/") + 1);
        Path savedFile = tempDir.resolve("images").resolve(fileName);
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("Should throw BadRequestException when custom product image is null")
    void testSaveCustomProductImage_NullFile() {
        assertThatThrownBy(() -> fileStorageService.saveCustomProductImage(null))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("File is empty");
    }

    @Test
    @DisplayName("Should throw BadRequestException when custom product image is empty")
    void testSaveCustomProductImage_EmptyFile() {
        MultipartFile emptyFile = createMockMultipartFile("test.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> fileStorageService.saveCustomProductImage(emptyFile))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("File is empty");
    }

    @Test
    @DisplayName("Should throw FileSizeExceededException when custom product image exceeds 2MB")
    void testSaveCustomProductImage_ExceedsSizeLimit() {
        MultipartFile largeImage = createMockMultipartFile("large.jpg", "image/jpeg", largeImageBytes);

        assertThatThrownBy(() -> fileStorageService.saveCustomProductImage(largeImage))
            .isInstanceOf(FileSizeExceededException.class)
            .hasMessage("File size exceeds 2MB limit");
    }

    @Test
    @DisplayName("Should throw BadRequestException when custom product image has invalid extension")
    void testSaveCustomProductImage_InvalidExtension() {
        MultipartFile invalidFile = createMockMultipartFile("test.gif", "image/gif", validImageBytes);

        assertThatThrownBy(() -> fileStorageService.saveCustomProductImage(invalidFile))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("File type is not allowed");
    }

    @Test
    @DisplayName("Should throw BadRequestException when custom product image content is not valid")
    void testSaveCustomProductImage_InvalidImageContent() {
        byte[] notAnImage = "this is not an image".getBytes();
        MultipartFile invalidFile = createMockMultipartFile("test.jpg", "image/jpeg", notAnImage);

        assertThatThrownBy(() -> fileStorageService.saveCustomProductImage(invalidFile))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("not a valid image");
    }

    // ==================== deleteProductImage Tests ====================

    @Test
    @DisplayName("Should delete product image from products folder")
    void testDeleteProductImage_FromProductsFolder() throws IOException {

        Path productsDir = tempDir.resolve("products");
        Files.createDirectories(productsDir);
        Path testFile = productsDir.resolve("test-image.jpg");
        Files.write(testFile, "test content".getBytes());

        fileStorageService.deleteProductImage("/uploads/products/test-image.jpg");

        assertThat(Files.exists(testFile)).isFalse();
    }

    @Test
    @DisplayName("Should delete product image from images folder")
    void testDeleteProductImage_FromImagesFolder() throws IOException {

        Path imagesDir = tempDir.resolve("images");
        Files.createDirectories(imagesDir);
        Path testFile = imagesDir.resolve("test-image.jpg");
        Files.write(testFile, "test content".getBytes());

        fileStorageService.deleteProductImage("/uploads/images/test-image.jpg");

        assertThat(Files.exists(testFile)).isFalse();
    }

    @Test
    @DisplayName("Should not throw exception when deleting non-existent file")
    void testDeleteProductImage_NonExistentFile() {

        fileStorageService.deleteProductImage("/uploads/images/non-existent.jpg");
    }

    @Test
    @DisplayName("Should not delete external URL")
    void testDeleteProductImage_ExternalUrl() throws IOException {

        Path productsDir = tempDir.resolve("products");
        Files.createDirectories(productsDir);
        Path testFile = productsDir.resolve("external.jpg");
        Files.write(testFile, "test content".getBytes());

        fileStorageService.deleteProductImage("http://example.com/external.jpg");


        assertThat(Files.exists(testFile)).isTrue();
    }

    @Test
    @DisplayName("Should not delete when URL is null")
    void testDeleteProductImage_NullUrl() {

        fileStorageService.deleteProductImage(null);
    }

    @Test
    @DisplayName("Should not delete when URL is blank")
    void testDeleteProductImage_BlankUrl() {

         fileStorageService.deleteProductImage("");
        fileStorageService.deleteProductImage("   ");
    }

    // ==================== isExternalUrl Tests ====================

    @Test
    @DisplayName("Should identify HTTP URLs as external")
    void testIsExternalUrl_Http() {
        assertThat(fileStorageService.isExternalUrl("http://example.com/image.jpg")).isTrue();
    }

    @Test
    @DisplayName("Should identify HTTPS URLs as external")
    void testIsExternalUrl_Https() {
        assertThat(fileStorageService.isExternalUrl("https://example.com/image.jpg")).isTrue();
    }

    @Test
    @DisplayName("Should identify relative URLs as not external")
    void testIsExternalUrl_Relative() {
        assertThat(fileStorageService.isExternalUrl("/uploads/images/image.jpg")).isFalse();
    }

    @Test
    @DisplayName("Should identify null as not external")
    void testIsExternalUrl_Null() {
        assertThat(fileStorageService.isExternalUrl(null)).isFalse();
    }

    @Test
    @DisplayName("Should identify empty string as not external")
    void testIsExternalUrl_Empty() {
        assertThat(fileStorageService.isExternalUrl("")).isFalse();
    }

    // ==================== Save Custom Product Image Tests ====================

    @Test
    @DisplayName("Should accept JPEG extension (uppercase)")
    void testSaveCustomProductImage_JPEGExtension() throws IOException {
        MultipartFile jpegFile = createMockMultipartFile("test.JPEG", "image/jpeg", validImageBytes);

        String result = fileStorageService.saveCustomProductImage(jpegFile);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("/uploads/images/");
    }

    @Test
    @DisplayName("Should create upload directory if it doesn't exist")
    void testSaveCustomProductImage_CreatesDirectory() throws IOException {
        Path imagesDir = tempDir.resolve("images");
        assertThat(Files.exists(imagesDir)).isFalse();

        MultipartFile validImageFile = createMockMultipartFile("test.jpg", "image/jpeg", validImageBytes);
        fileStorageService.saveCustomProductImage(validImageFile);

        assertThat(Files.exists(imagesDir)).isTrue();
    }

    @Test
    @DisplayName("Should handle IOException when reading image")
    void testSaveCustomProductImage_IOExceptionOnRead() throws IOException {
        MultipartFile problematicFile = mock(MultipartFile.class);
        when(problematicFile.isEmpty()).thenReturn(false);
        when(problematicFile.getSize()).thenReturn(1024L);
        when(problematicFile.getOriginalFilename()).thenReturn("test.jpg");
        when(problematicFile.getInputStream())
            .thenReturn(new ByteArrayInputStream(validImageBytes))
            .thenThrow(new IOException("Stream error"));

        assertThatThrownBy(() -> fileStorageService.saveCustomProductImage(problematicFile))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Could not store image");
    }

    // ==================== Helper Methods ====================

    private MultipartFile createMockMultipartFile(String filename, String contentType, byte[] content) {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getOriginalFilename()).thenReturn(filename);
        lenient().when(file.getContentType()).thenReturn(contentType);
        lenient().when(file.getSize()).thenReturn((long) content.length);
        lenient().when(file.isEmpty()).thenReturn(content.length == 0);
        
        try {
            lenient().when(file.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(content));
            lenient().when(file.getBytes()).thenReturn(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return file;
    }
}
