package com.example.photogallery.service;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Photo;
import com.example.photogallery.model.enums.WorkflowStatus;
import com.example.photogallery.repository.PhotoRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
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

    private String calculateFileHash(byte[] fileBytes) throws IOException {
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
            throw new IOException("SHA-256 algorithm not available", e);
        }
    }

    @PostConstruct
    public void initializeExistingImages() {
        try {
            Path uploadPath = Paths.get("uploads");
            if (!Files.exists(uploadPath)) {
                return;
            }

            List<String> existingFiles = Files.list(uploadPath)
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());

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
        } catch (IOException e) {
            System.err.println(
                "Error initializing existing images: " + e.getMessage()
            );
        }
    }

    public Photo savePhoto(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (
            filename == null ||
            !filename.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")
        ) {
            throw new IOException("Invalid file type");
        }

        if (!contentType.startsWith("image/")) {
            throw new IOException("Not an image file");
        }
        byte[] fileBytes = file.getBytes();
        String fileHash = calculateFileHash(fileBytes);

        Optional<Photo> existingPhoto = photoRepository.findByFileHash(
            fileHash
        );
        if (existingPhoto.isPresent()) {
            throw new IOException("Duplicate file");
        }

        String fileName = photoStorageService.storeFile(file);

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

    public Photo updatePhoto(Long id, MultipartFile file) throws IOException {
        Photo existingPhoto = photoRepository.findById(id).orElse(null);
        if (existingPhoto == null) {
            throw new IOException("Photo not found");
        }

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (
            filename == null ||
            !filename.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")
        ) {
            throw new IOException("Invalid file type");
        }

        if (!contentType.startsWith("image/")) {
            throw new IOException("Not an image file");
        }

        byte[] fileBytes = file.getBytes();
        String newFileHash = calculateFileHash(fileBytes);

        // Check if the new file is different from current
        if (newFileHash.equals(existingPhoto.getFileHash())) {
            throw new IOException("File is identical to current photo");
        }

        // Check if new file hash already exists in other photos
        Optional<Photo> duplicatePhoto = photoRepository.findByFileHash(
            newFileHash
        );
        if (
            duplicatePhoto.isPresent() &&
            !duplicatePhoto.get().getId().equals(id)
        ) {
            throw new IOException("File already exists (duplicate detected)");
        }

        // Delete old file and store new one
        photoStorageService.deleteFile(existingPhoto.getFileName());
        String newFileName = photoStorageService.storeFile(file);

        // Update photo record
        existingPhoto.setOriginalName(file.getOriginalFilename());
        existingPhoto.setFileName(newFileName);
        existingPhoto.setContentType(file.getContentType());
        existingPhoto.setSize(file.getSize());
        existingPhoto.setFileHash(newFileHash);

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

    public void deletePhoto(Long id) {
        photoRepository.deleteById(id);
    }

    // Album-related methods
    public List<Photo> getPhotosInAlbum(Album album) {
        return photoRepository.findByAlbumOrderBySortOrderInAlbumAsc(album);
    }

    public List<Photo> getPhotosInAlbum(Album album, WorkflowStatus status) {
        if (status != null) {
            return photoRepository.findByAlbumAndWorkflowStatusOrderBySortOrderInAlbumAsc(
                album,
                status
            );
        }
        return getPhotosInAlbum(album);
    }

    public List<Photo> getUnassignedPhotos() {
        return photoRepository.findByAlbumIsNullOrderByUploadDateDesc();
    }

    public List<Photo> getPhotosByWorkflowStatus(WorkflowStatus status) {
        return photoRepository.findByWorkflowStatusOrderByUploadDateDesc(
            status
        );
    }

    public List<Photo> getFeaturedPhotos() {
        return photoRepository.findByIsFeaturedTrueOrderBySortOrderInAlbumAsc();
    }

    public List<Photo> getPortfolioPhotos() {
        return photoRepository.findByIsPortfolioImageTrueOrderBySortOrderInAlbumAsc();
    }

    public List<Photo> getClientApprovedPhotos() {
        return photoRepository.findByClientApprovedTrueOrderBySortOrderInAlbumAsc();
    }

    public Optional<Photo> findById(Long id) {
        return photoRepository.findById(id);
    }

    public void updateWorkflowStatus(Photo photo, WorkflowStatus status) {
        photo.setWorkflowStatus(status);
        photoRepository.save(photo);
    }

    public void toggleFeatured(Photo photo) {
        photo.setIsFeatured(!photo.getIsFeatured());
        photoRepository.save(photo);
    }

    public void togglePortfolioImage(Photo photo) {
        photo.setIsPortfolioImage(!photo.getIsPortfolioImage());
        photoRepository.save(photo);
    }

    public void setClientApproval(Photo photo, boolean approved) {
        photo.setClientApproved(approved);
        photoRepository.save(photo);
    }
}
