package com.example.photogallery.service;

import com.example.photogallery.model.Photo;
import com.example.photogallery.repository.PhotoRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
                    Photo photo = new Photo(
                        fileName, // Use filename as original name since we don't know the original
                        fileName,
                        "application/octet-stream", // Generic content type
                        Files.size(filePath)
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
        String fileName = photoStorageService.storeFile(file);

        Photo photo = new Photo(
            file.getOriginalFilename(),
            fileName,
            file.getContentType(),
            file.getSize()
        );

        return photoRepository.save(photo);
    }

    public List<Photo> getAllPhotos() {
        return photoRepository.findAll();
    }

    public Photo getPhotoById(Long id) {
        return photoRepository.findById(id).orElse(null);
    }

    public void deletePhoto(Long id) {
        photoRepository.deleteById(id);
    }
}
