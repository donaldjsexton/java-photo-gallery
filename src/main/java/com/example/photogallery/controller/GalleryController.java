// src/main/java/com/example/photogallery/controller/GalleryController.java
package com.example.photogallery.controller;

import com.example.photogallery.model.Photo;
import com.example.photogallery.service.PhotoSearchService;
import com.example.photogallery.service.PhotoService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private PhotoSearchService searchService;

    @GetMapping("/")
    public String showGallery(
        Model model,
        @RequestParam(value = "sort", defaultValue = "uploadDate") String sort,
        @RequestParam(value = "search", required = false) String search // Add this
    ) {
        List<Photo> photos;

        if (search != null && !search.trim().isEmpty()) {
            // Search functionality
            photos = searchService.searchPhotos(
                search,
                PageRequest.of(0, 100, Sort.by(sort).descending())
            );
            model.addAttribute("searchQuery", search);
            model.addAttribute("isSearchResult", true);
        } else {
            // Regular gallery view
            photos = photoService.getAllPhotosSorted(sort);
            model.addAttribute("isSearchResult", false);
        }

        model.addAttribute("photos", photos);
        model.addAttribute("currentSort", sort);
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
            try {
                photoService.deletePhoto(id); // service handles disk + DB
                redirectAttributes.addFlashAttribute(
                    "message",
                    "Photo deleted successfully!"
                );
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute(
                    "error",
                    "Delete failed: " + e.getMessage()
                );
            }
        } else {
            redirectAttributes.addFlashAttribute("message", "Photo not found.");
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

    @GetMapping("/photo/{id}/exif")
    @ResponseBody
    public Map<String, Object> getPhotoExif(@PathVariable Long id) {
        Photo photo = photoService.getPhotoById(id);
        Map<String, Object> exifData = new HashMap<>();

        if (photo != null) {
            exifData.put("camera", photo.getCamera());
            exifData.put("dateTaken", photo.getDateTaken());
            exifData.put("gpsLatitude", photo.getGpsLatitude());
            exifData.put("gpsLongitude", photo.getGpsLongitude());
            exifData.put("orientation", photo.getOrientation());
            exifData.put("focalLength", photo.getFocalLength());
            exifData.put("aperture", photo.getAperture());
            exifData.put("shutterSpeed", photo.getShutterSpeed());
            exifData.put("iso", photo.getIso());
            exifData.put("imageWidth", photo.getImageWidth());
            exifData.put("imageHeight", photo.getImageHeight());
            exifData.put("allExifData", photo.getAllExifData());
        }

        return exifData;
    }
}
