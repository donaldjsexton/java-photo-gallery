// src/main/java/com/example/photogallery/controller/GalleryController.java
package com.example.photogallery.controller;

import com.example.photogallery.model.Photo;
import com.example.photogallery.service.PhotoService;
import com.example.photogallery.service.PhotoStorageService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/api/upload")
    @ResponseBody
    public Map<String, Object> uploadSingle(
        @RequestParam("file") MultipartFile file
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            Photo photo = photoService.savePhoto(file);
            result.put("success", true);
            result.put("id", photo.getId());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/photo/{id}/delete")
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

    @PostMapping("/photo/{id}/update")
    public String updatePhoto(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        RedirectAttributes redirectAttributes
    ) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Please select a file"
            );
            return "redirect:/";
        }

        try {
            photoService.updatePhoto(id, file);
            redirectAttributes.addFlashAttribute(
                "message",
                "Photo updated successfully!"
            );
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Update failed: " + e.getMessage()
            );
        }

        return "redirect:/";
    }
}
