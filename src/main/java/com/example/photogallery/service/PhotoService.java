package com.example.photogallery.service;

import com.example.photogallery.model.Photo;
import com.example.photogallery.repository.PhotoRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.io.IOException;
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

    @Value("${photo.gallery.upload.dir}")
    private String uploadDir;

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

    @PostConstruct
    public void initializeExistingImages() {
        try {
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
                    // Create photo record for orphaned file
                    Path filePath = uploadPath.resolve(fileName);
                    byte[] fileBytes = Files.readAllBytes(filePath);
                    String fileHash = calculateFileHash(fileBytes);

                    if (photoRepository.findByFileHash(fileHash).isPresent()) {
                        // File already exists in the database
                        continue;
                    }

                    Photo photo = new Photo(
                        fileName, // Use filename as original name since we don't know the original
                        fileName,
                        "application/octet-stream", // Generic content type
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

    @Transactional
    public Photo savePhoto(MultipartFile file) {
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

        Optional<Photo> existingPhoto = photoRepository.findByFileHash(
            fileHash
        );
        if (existingPhoto.isPresent()) {
            throw new IllegalArgumentException("Duplicate file");
        }

        String fileName =
            UUID.randomUUID().toString() + getCanonicalExtension(filename);

        try {
            photoStorageService.storeFile(fileBytes, fileName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store uploaded file", e);
        }

        Photo photo = new Photo(
            file.getOriginalFilename(),
            fileName,
            file.getContentType(),
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

    @Transactional
    public Photo updatePhoto(Long id, MultipartFile file) {
        Photo existingPhoto = photoRepository
            .findById(id)
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
            throw new IllegalArgumentException(
                "File is identical to current photo"
            );
        }

        // New file already used by another photo → 400
        Optional<Photo> duplicatePhoto = photoRepository.findByFileHash(
            newFileHash
        );
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

            exifService.extractAndSetExifData(existingPhoto, fileBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to replace stored file", e);
        }

        // Re-extract EXIF for the new image
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

    public List<Photo> getAllPhotos() {
        return photoRepository.findAll();
    }

    public List<Photo> getAllPhotosSorted(String sortBy) {
        switch (sortBy) {
            case "dateTaken":
                return photoRepository.findAllByOrderByDateTakenDesc();
            case "dateTakenAsc":
                return photoRepository.findAllByOrderByDateTakenAsc();
            case "camera":
                return photoRepository.findAllByOrderByCameraAsc();
            case "uploadDate":
                return photoRepository.findAllByOrderByUploadDateDesc();
            case "withCamera":
                return photoRepository.findPhotosWithCamera();
            case "withDateTaken":
                return photoRepository.findPhotosWithDateTaken();
            default:
                return photoRepository.findAll();
        }
    }

    public Photo getPhotoById(Long id) {
        return photoRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deletePhoto(Long id) throws IOException {
        Photo p = photoRepository
            .findById(id)
            .orElseThrow(() ->
                new NoSuchElementException("Photo not found with id " + id)
            );
        photoStorageService.deleteFile(p.getFileName());
        photoRepository.deleteById(id);
    }

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
}
