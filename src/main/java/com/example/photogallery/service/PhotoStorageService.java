package com.example.photogallery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class PhotoStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        return fileName;
    }

    public File getFile(String fileName) {
        Path filePath = Paths.get(uploadDir, fileName);
        return filePath.toFile();
    }

    public boolean deleteFile(String fileName) {
        File file = getFile(fileName);
        return file.delete();
    }
}