// src/main/java/com/example/photogallery/controller/GalleryController.java
package com.example.photogallery.controller;

import com.example.photogallery.model.Photo;
import com.example.photogallery.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
public class GalleryController {

    @Autowired
    private PhotoService photoService;

    @GetMapping("/")
    public String showGallery(Model model) {
        List<Photo> photos = photoService.getAllPhotos();
        model.addAttribute("photos", photos);
        return "gallery";
    }

    @PostMapping("/upload")
    public String uploadPhotos(@RequestParam("files") MultipartFile[] files,
                               RedirectAttributes redirectAttributes) {
        if (files == null || files.length == 0) {
            redirectAttributes.addFlashAttribute("message", "Please select at least one file to upload");
            return "redirect:/";
        }

        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    photoService.savePhoto(file);
                }
            }
            redirectAttributes.addFlashAttribute("message", "Files uploaded successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "Failed to upload files: " + e.getMessage());
        }

        return "redirect:/";
    }
}
