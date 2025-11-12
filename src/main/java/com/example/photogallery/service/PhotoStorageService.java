package com.example.photogallery.service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PhotoStorageService {

    private final Path uploadPath;

    public PhotoStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(uploadPath);
    }

    public String storeFile(byte[] bytes, String storedFileName)
        throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IOException("Empty file bytes");
        }
        if (storedFileName == null || storedFileName.isBlank()) {
            throw new IOException("Target file name required");
        }
        if (
            storedFileName.contains("..") ||
            storedFileName.contains("/") ||
            storedFileName.contains("\\")
        ) {
            throw new IOException("Invalid file name");
        }
        Path target = uploadPath.resolve(storedFileName).normalize();
        if (!target.startsWith(uploadPath)) {
            throw new IOException("Resolved path escapes upload root");
        }

        Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        return storedFileName;
    }

    public Path getFilePath(String storedFileName) throws IOException {
        if (
            storedFileName == null ||
            storedFileName.isBlank() ||
            storedFileName.contains("..") ||
            storedFileName.contains("/") ||
            storedFileName.contains("\\")
        ) {
            throw new IOException("Invalid file name");
        }

        Path p = uploadPath.resolve(storedFileName).normalize();
        if (!p.startsWith(uploadPath)) {
            throw new IOException("Resolved path escapes upload root");
        }

        return p;
    }

    public boolean deleteFile(String storedFileName) throws IOException {
        return Files.deleteIfExists(getFilePath(storedFileName));
    }
}
