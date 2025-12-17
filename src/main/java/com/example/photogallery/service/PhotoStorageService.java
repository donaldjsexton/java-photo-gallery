package com.example.photogallery.service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PhotoStorageService {

    private final Path uploadPath;

    private static final String TENANT_SLUG_PATTERN = "^[a-z0-9][a-z0-9-]{0,63}$";

    public PhotoStorageService(
        @Value("${photo.gallery.upload.dir}") String uploadDir
    ) {
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
        if (storedFileName.contains("..") || storedFileName.contains("\\")) {
            throw new IOException("Invalid file name");
        }

        Path target = resolveStoredPath(storedFileName);
        if (!target.startsWith(uploadPath)) {
            throw new IOException("Resolved path escapes upload root");
        }

        Files.createDirectories(target.getParent());
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        return storedFileName;
    }

    public Path getFilePath(String storedFileName) throws IOException {
        if (
            storedFileName == null ||
            storedFileName.isBlank() ||
            storedFileName.contains("..") ||
            storedFileName.contains("\\")
        ) {
            throw new IOException("Invalid file name");
        }

        Path p = resolveStoredPath(storedFileName);
        if (!p.startsWith(uploadPath)) {
            throw new IOException("Resolved path escapes upload root");
        }

        return p;
    }

    public boolean deleteFile(String storedFileName) throws IOException {
        return Files.deleteIfExists(getFilePath(storedFileName));
    }

    public void deleteEmptyTenantDirectory(String tenantSlug) throws IOException {
        if (tenantSlug == null || tenantSlug.isBlank()) {
            return;
        }
        String trimmed = tenantSlug.trim();
        if (!trimmed.matches(TENANT_SLUG_PATTERN)) {
            throw new IOException("Invalid tenant segment");
        }

        Path tenantDir = uploadPath.resolve(trimmed).normalize();
        if (!tenantDir.startsWith(uploadPath)) {
            throw new IOException("Resolved path escapes upload root");
        }
        if (!Files.isDirectory(tenantDir)) {
            return;
        }

        try (var stream = Files.list(tenantDir)) {
            if (stream.findAny().isPresent()) {
                return;
            }
        }
        Files.deleteIfExists(tenantDir);
    }

    private Path resolveStoredPath(String storedFileName) throws IOException {
        // Supports:
        // - legacy: "uuid.jpg" (stored at uploads/uuid.jpg)
        // - tenant-segmented: "tenant-slug/uuid.jpg" (stored at uploads/tenant-slug/uuid.jpg)
        String normalized = storedFileName.trim();

        int slashCount = 0;
        for (int i = 0; i < normalized.length(); i++) {
            if (normalized.charAt(i) == '/') slashCount++;
        }
        if (slashCount == 0) {
            if (normalized.contains("/")) {
                throw new IOException("Invalid file name");
            }
            return uploadPath.resolve(normalized).normalize();
        }
        if (slashCount != 1) {
            throw new IOException("Invalid file name");
        }

        int idx = normalized.indexOf('/');
        String tenantSlug = normalized.substring(0, idx);
        String leafName = normalized.substring(idx + 1);

        if (!tenantSlug.matches(TENANT_SLUG_PATTERN)) {
            throw new IOException("Invalid tenant segment");
        }
        if (
            leafName.isBlank() ||
            leafName.contains("..") ||
            leafName.contains("/") ||
            leafName.contains("\\")
        ) {
            throw new IOException("Invalid file name");
        }

        return uploadPath.resolve(tenantSlug).resolve(leafName).normalize();
    }
}
