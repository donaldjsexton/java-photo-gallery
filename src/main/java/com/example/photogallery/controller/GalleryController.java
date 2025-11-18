package com.example.photogallery.controller;

import com.example.photogallery.model.Photo;
import com.example.photogallery.service.PhotoSearchService;
import com.example.photogallery.service.PhotoService;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GalleryController {

    private static final Set<String> ALLOWED_SORTS = Set.of(
        "uploadDate",
        "dateTaken",
        "dateTakenAsc",
        "camera",
        "withCamera",
        "withDateTaken"
    );

    private final PhotoService photoService;
    private final PhotoSearchService photoSearchService;

    public GalleryController(
        PhotoService photoService,
        PhotoSearchService photoSearchService
    ) {
        this.photoService = photoService;
        this.photoSearchService = photoSearchService;
    }

    @GetMapping("/")
    public String listPhotos(
        @RequestParam(name = "sort", defaultValue = "uploadDate") String sort,
        @RequestParam(name = "search", required = false) String search,
        Model model
    ) {
        String effectiveSort = ALLOWED_SORTS.contains(sort)
            ? sort
            : "uploadDate";
        boolean isSearch = StringUtils.hasText(search);
        List<Photo> photos;

        if (isSearch) {
            // Use the same search pipeline as the REST API; cap page size for UI
            Pageable pageable = PageRequest.of(0, 200);
            Page<Photo> page = photoSearchService.searchByText(
                search,
                pageable
            );
            photos = page.getContent();
        } else {
            photos = photoService.getAllPhotosSorted(effectiveSort);
        }

        model.addAttribute("photos", photos);
        model.addAttribute("currentSort", effectiveSort);
        model.addAttribute("searchQuery", search);
        model.addAttribute("isSearchResult", isSearch);

        return "gallery";
    }

    @PostMapping("/upload")
    public String handleFileUpload(
        @RequestParam("files") MultipartFile[] files,
        RedirectAttributes redirectAttributes
    ) {
        int successCount = 0;
        int duplicateCount = 0;
        int errorCount = 0;

        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                try {
                    photoService.savePhoto(file);
                    successCount++;
                } catch (IllegalArgumentException ex) {
                    // Treat IllegalArgumentException from service as "duplicate" / bad input
                    duplicateCount++;
                } catch (RuntimeException ex) {
                    // Catch-all to avoid blowing up the upload page
                    errorCount++;
                }
            }
        }

        StringBuilder msg = new StringBuilder();
        if (successCount > 0) {
            msg.append(successCount).append(" photo(s) uploaded. ");
        }
        if (duplicateCount > 0) {
            msg
                .append(duplicateCount)
                .append(" duplicate or invalid file(s) skipped. ");
        }
        if (errorCount > 0) {
            msg.append(errorCount).append(" file(s) failed to upload.");
        }
        if (msg.length() == 0) {
            msg.append("No files were uploaded.");
        }

        redirectAttributes.addFlashAttribute("message", msg.toString().trim());
        return "redirect:/";
    }

    @PostMapping("/photo/{id}/delete")
    public String deletePhoto(
        @PathVariable Long id,
        RedirectAttributes redirectAttributes
    ) {
        try {
            photoService.deletePhoto(id);
            redirectAttributes.addFlashAttribute("message", "Photo deleted.");
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Failed to delete photo."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Unexpected error deleting photo."
            );
        }
        return "redirect:/";
    }
}
