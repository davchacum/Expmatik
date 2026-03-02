package com.expmatik.backend.config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(MvcConfig.class);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            File uploadFolder = uploadPath.toFile();

            if (!uploadFolder.exists()) {
                if (uploadFolder.mkdirs()) {
                    logger.info("Upload directory created: {}", uploadPath);
                } else {
                    logger.warn("Failed to create upload directory: {}", uploadPath);
                }
            }

            String uploadPathStr = uploadPath.toString();
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations("file:" + uploadPathStr + File.separator);

            logger.info("Resource handler configured for /uploads/** -> {}", uploadPathStr);
        } catch (Exception e) {
            logger.error("Error configuring resource handlers", e);
        }
    }
}