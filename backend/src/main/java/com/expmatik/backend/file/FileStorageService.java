package com.expmatik.backend.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.FileSizeExceededException;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final long CUSTOM_MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String[] ALLOWED_EXTENSIONS = { "jpg","jpeg","png" };


    public String saveCustomProductImage(MultipartFile image) {
        try {
            validateCustomProductImage(image);
            String fileName = UUID.randomUUID().toString() + ".jpg";

            Path uploadPath = Paths.get(uploadDir, "images").toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Upload directory created: {}", uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Custom product image stored: {}", fileName);

            return "/uploads/images/" + fileName;
        } catch (IOException e) {
            logger.error("Error storing custom product image", e);
            throw new RuntimeException("Could not store image. Please try again!", e);
        }
    }

    public String saveProductImage(MultipartFile file) {
        validateFile(file);

        try {
            Path uploadPath = Paths.get(uploadDir, "products").toAbsolutePath();
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID().toString() + "." + extension;

            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, file.getBytes());

            logger.info("File saved: {}", filename);

            return "/uploads/products/" + filename;
        } catch (IOException e) {
            logger.error("Error saving file", e);
            throw new BadRequestException("Failed to save image: " + e.getMessage());
        }
    }

    public void deleteProductImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || isExternalUrl(imageUrl)) {
            return; 
        }

        try {
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            
            Path filePath1 = Paths.get(uploadDir, "products", filename).toAbsolutePath();
            Path filePath2 = Paths.get(uploadDir, "images", filename).toAbsolutePath();

            if (Files.exists(filePath1)) {
                Files.delete(filePath1);
                logger.info("File deleted from products: {}", filename);
            } else if (Files.exists(filePath2)) {
                Files.delete(filePath2);
                logger.info("File deleted from images: {}", filename);
            }
        } catch (IOException e) {
            logger.warn("Error deleting file: {}", imageUrl, e);
        }
    }

    public boolean isExternalUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private void validateFile(MultipartFile file) {
        
        checkIfFileIsEmpty(file);

        checkIfFileSizeExceedsLimit(file);

        checkIfFileNameIsInvalid(file);

        checkIfFileTypeIsAllowed(file);

        checkIfFileContentIsValidImage(file);
    }

    private void validateCustomProductImage(MultipartFile file) {
        checkIfFileIsEmpty(file);
        
        checkIfFileSizeExceedsLimit(file);
        
        checkIfFileTypeIsAllowed(file);
        
        checkIfFileContentIsValidImage(file);
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf(".");
        if (lastDot > 0) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    private void checkIfFileIsEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
    }

    private void checkIfFileSizeExceedsLimit(MultipartFile file) {
        if (file.getSize() > CUSTOM_MAX_FILE_SIZE) {
            throw new FileSizeExceededException("File size exceeds 2MB limit");
        }
    }

    private void checkIfFileNameIsInvalid(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BadRequestException("Filename is null");
        }
    }

    private void checkIfFileTypeIsAllowed(MultipartFile file) {
        String filename = file.getOriginalFilename();

        String extension = getFileExtension(filename).toLowerCase();
        boolean isAllowed = false;
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
             throw new BadRequestException("File type is not allowed. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private void checkIfFileContentIsValidImage(MultipartFile image) {
        try (InputStream is = image.getInputStream()) {
                if (ImageIO.read(is) == null) {
                    throw new BadRequestException("The file content is not a valid image.");
                }
        }
        catch (IOException e) {
            throw new BadRequestException("Error reading image file: " + e.getMessage());
        }
    }
    
}
