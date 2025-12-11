package com.example.photogallery.service;

import com.example.photogallery.model.Photo;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.PhotoRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoService {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private PhotoStorageService photoStorageService;

    @Autowired
    private ExifService exifService;

    @Autowired
    private TenantService tenantService;

    @Value("${photo.gallery.upload.dir}")
    private String uploadDir;

    //Duplicate Enum
    public enum DuplicateHandling {
        CANCEL,
        SKIP,
        OVERWRITE;

        public static DuplicateHandling fromString(String raw) {
            if (raw == null || raw.isBlank()) {
                return CANCEL;
            }
            switch (raw.toLowerCase()) {
                case "skip":
                    return SKIP;
                case "overwrite":
                    return OVERWRITE;
                default:
                    return CANCEL;
            }
        }
    }

    // ---------------------------------------------------------
    // File Hash
    // ---------------------------------------------------------
    private String calculateFileHash(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    // ---------------------------------------------------------
    // Initialize existing orphaned images
    // ---------------------------------------------------------
    @PostConstruct
    public void initializeExistingImages() {
        try {
            Tenant tenant = resolveTenant();
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                return;
            }

            List<String> existingFiles;
            try (var stream = Files.list(uploadPath)) {
                existingFiles = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
            }

            List<String> dbFiles = getAllPhotos()
                .stream()
                .map(Photo::getFileName)
                .collect(Collectors.toList());

            for (String fileName : existingFiles) {
                if (!dbFiles.contains(fileName)) {
                    Path filePath = uploadPath.resolve(fileName);
                    byte[] fileBytes = Files.readAllBytes(filePath);
                    String fileHash = calculateFileHash(fileBytes);

                    // Skip if hash already known to DB
                    if (
                        photoRepository
                            .findByTenantAndFileHash(tenant, fileHash)
                            .isPresent()
                    ) {
                        continue;
                    }

                    Photo photo = new Photo(
                        tenant,
                        fileName,
                        fileName,
                        "application/octet-stream",
                        Files.size(filePath),
                        fileHash
                    );

                    photoRepository.save(photo);
                }
            }
        } catch (Exception e) {
            System.err.println(
                "Error initializing existing images: " + e.getMessage()
            );
        }
    }

    // ---------------------------------------------------------
    // Upload new photo
    // ---------------------------------------------------------
    public Photo savePhoto(MultipartFile file) {
        return savePhoto(file, DuplicateHandling.CANCEL);
    }

    @Transactional
    public Photo savePhoto(MultipartFile file, DuplicateHandling handling) {
        Tenant tenant = resolveTenant();
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        if (
            filename == null ||
            !filename.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")
        ) {
            throw new IllegalArgumentException("Invalid file type");
        }

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Not an image file");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read uploaded file bytes", e);
        }

        String fileHash = calculateFileHash(fileBytes);

        Optional<Photo> existingPhotoOpt = photoRepository.findByTenantAndFileHash(
            tenant,
            fileHash
        );
        if (existingPhotoOpt.isPresent()) {
            Photo existing = existingPhotoOpt.get();

            switch (handling) {
                case CANCEL:
                    // Current behavior
                    throw new IllegalArgumentException("Duplicate file");
                case SKIP:
                    // Don’t write anything, just reuse the existing row
                    return existing;
                case OVERWRITE:
                    try {
                        // Replace file on disk for that existing photo
                        photoStorageService.deleteFile(existing.getFileName());

                        String newStoredName =
                            UUID.randomUUID().toString() +
                            getCanonicalExtension(filename);

                        photoStorageService.storeFile(fileBytes, newStoredName);

                        existing.setOriginalName(filename);
                        existing.setFileName(newStoredName);
                        existing.setContentType(contentType);
                        existing.setSize(file.getSize());
                        existing.setFileHash(fileHash);

                        exifService.extractAndSetExifData(existing, fileBytes);
                        return photoRepository.save(existing);
                    } catch (Exception e) {
                        throw new RuntimeException(
                            "Failed to overwrite existing file",
                            e
                        );
                    }
            }
        }

        String fileName =
            UUID.randomUUID().toString() + getCanonicalExtension(filename);

        try {
            photoStorageService.storeFile(fileBytes, fileName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store uploaded file", e);
        }

        Photo photo = new Photo(
            tenant,
            filename,
            fileName,
            contentType,
            file.getSize(),
            fileHash
        );

        try {
            exifService.extractAndSetExifData(photo, fileBytes);
        } catch (Exception e) {
            System.err.println(
                "EXIF extraction failed for " + filename + ": " + e.getMessage()
            );
        }

        return photoRepository.save(photo);
    }

    // ---------------------------------------------------------
    // Update / Replace photo file
    // ---------------------------------------------------------
    @Transactional
    public Photo updatePhoto(Long id, MultipartFile file) {
        Tenant tenant = resolveTenant();
        Photo existingPhoto = photoRepository
            .findByIdAndTenant(id, tenant)
            .orElseThrow(() ->
                new NoSuchElementException("Photo not found with id " + id)
            );

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        if (
            filename == null ||
            !filename.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")
        ) {
            throw new IllegalArgumentException("Invalid file type");
        }

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Not an image file");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read uploaded file bytes", e);
        }

        String newFileHash = calculateFileHash(fileBytes);

        // Same file as current → 400
        if (newFileHash.equals(existingPhoto.getFileHash())) {
            return existingPhoto;
        }

        // New file already used by another photo → 400
        Optional<Photo> duplicatePhoto =
            photoRepository.findByTenantAndFileHash(tenant, newFileHash);
        if (
            duplicatePhoto.isPresent() &&
            !duplicatePhoto.get().getId().equals(id)
        ) {
            throw new IllegalArgumentException(
                "File already exists (duplicate detected)"
            );
        }

        // Delete old file and store new one
        try {
            photoStorageService.deleteFile(existingPhoto.getFileName());
            String newFileName =
                UUID.randomUUID().toString() + getCanonicalExtension(filename);
            photoStorageService.storeFile(fileBytes, newFileName);

            existingPhoto.setOriginalName(filename);
            existingPhoto.setFileName(newFileName);
            existingPhoto.setContentType(contentType);
            existingPhoto.setSize(file.getSize());
            existingPhoto.setFileHash(newFileHash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to replace stored file", e);
        }

        // EXIF extraction for the new image (log-only if it fails)
        try {
            exifService.extractAndSetExifData(existingPhoto, fileBytes);
        } catch (Exception e) {
            System.err.println(
                "EXIF extraction failed for updated photo " +
                    filename +
                    ": " +
                    e.getMessage()
            );
        }

        return photoRepository.save(existingPhoto);
    }

    // ---------------------------------------------------------
    // Queries
    // ---------------------------------------------------------
    public List<Photo> getAllPhotos() {
        return photoRepository.findAllByTenant(resolveTenant());
    }

    public List<Photo> getAllPhotosSorted(String sortBy) {
        Tenant tenant = resolveTenant();
        switch (sortBy) {
            case "dateTaken":
                return photoRepository.findAllByTenantOrderByDateTakenDesc(
                    tenant
                );
            case "dateTakenAsc":
                return photoRepository.findAllByTenantOrderByDateTakenAsc(
                    tenant
                );
            case "camera":
                return photoRepository.findAllByTenantOrderByCameraAsc(tenant);
            case "uploadDate":
                return photoRepository.findAllByTenantOrderByUploadDateDesc(
                    tenant
                );
            case "withCamera":
                return photoRepository.findPhotosWithCamera(tenant);
            case "withDateTaken":
                return photoRepository.findPhotosWithDateTaken(tenant);
            default:
                return photoRepository.findAllByTenant(tenant);
        }
    }

    public Photo getPhotoById(Long id) {
        Tenant tenant = resolveTenant();
        return photoRepository.findByIdAndTenant(id, tenant).orElse(null);
    }

    // ---------------------------------------------------------
    // Delete photo
    // ---------------------------------------------------------
    @Transactional
    public void deletePhoto(Long id) {
        Tenant tenant = resolveTenant();
        Photo p = photoRepository
            .findByIdAndTenant(id, tenant)
            .orElseThrow(() ->
                new NoSuchElementException("Photo not found with id " + id)
            );

        try {
            photoStorageService.deleteFile(p.getFileName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete stored file", e);
        }

        photoRepository.deleteById(id);
    }

    // ---------------------------------------------------------
    // Utility
    // ---------------------------------------------------------
    private static String getCanonicalExtension(String name) {
        if (name == null) return "";
        String base = Paths.get(name).getFileName().toString();
        int dot = base.lastIndexOf('.');
        if (dot < 0) return "";
        String ext = base.substring(dot).toLowerCase();
        if (".jpeg".equals(ext)) return ".jpg";
        if (".tif".equals(ext)) return ".tiff";
        return ext;
    }

    private Tenant resolveTenant() {
        return tenantService.getDefaultTenant();
    }
}
