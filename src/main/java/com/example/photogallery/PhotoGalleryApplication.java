package com.example.photogallery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class PhotoGalleryApplication {
    public static void main(String[] args) {
        loadDotEnvIfPresent();
        SpringApplication.run(PhotoGalleryApplication.class, args);
    }

    private static void loadDotEnvIfPresent() {
        Path envPath = Path.of(".env");
        if (!Files.isRegularFile(envPath)) {
            return;
        }
        try {
            Files.lines(envPath)
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .forEach(line -> {
                    int idx = line.indexOf('=');
                    if (idx <= 0) {
                        return;
                    }
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (System.getenv(key) == null && System.getProperty(key) == null) {
                        System.setProperty(key, value);
                    }
                });
        } catch (IOException e) {
            System.err.println("Failed to load .env: " + e.getMessage());
        }
    }
}
