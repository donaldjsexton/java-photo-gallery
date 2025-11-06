// src/main/java/com/example/photogallery/controller/GalleryController.java
package com.example.photogallery.controller;

import com.example.photogallery.model.Photo;
import com.example.photogallery.service.PhotoService;
import com.example.photogallery.service.PhotoStorageService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GalleryController {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private PhotoStorageService photoStorageService;

    @GetMapping("/")
    public String showGallery(Model model) {
        List<Photo> photos = photoService.getAllPhotos();
        model.addAttribute("photos", photos);
        return "gallery";
    }

    @PostMapping("/upload")
    public String uploadPhotos(
        @RequestParam("files") MultipartFile[] files,
        RedirectAttributes redirectAttributes
    ) {
        if (files == null || files.length == 0) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Please select at least one file to upload"
            );
            return "redirect:/";
        }

        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    photoService.savePhoto(file);
                }
            }
            redirectAttributes.addFlashAttribute(
                "message",
                "Files uploaded successfully!"
            );
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Failed to upload files: " + e.getMessage()
            );
        }

        return "redirect:/";
    }

    @GetMapping("/image/{fileName}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        try {
            File file = photoStorageService.getFile(fileName);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                .contentType(
                    MediaType.parseMediaType(
                        Files.probeContentType(file.toPath())
                    )
                )
                .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/photo/{id}")
    public String deletePhoto(
        @PathVariable Long id,
        RedirectAttributes redirectAttributes
    ) {
        Photo photo = photoService.getPhotoById(id);
        if (photo != null) {
            photoStorageService.deleteFile(photo.getFileName());
            photoService.deletePhoto(id);
            redirectAttributes.addFlashAttribute(
                "message",
                "Photo deleted successfully!"
            );
        } else {
            redirectAttributes.addFlashAttribute("message", "Photo not found!");
        }
        return "redirect:/";
    }
}
