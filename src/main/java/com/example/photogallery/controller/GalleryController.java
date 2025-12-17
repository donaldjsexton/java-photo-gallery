package com.example.photogallery.controller;

import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.Photo;
import com.example.photogallery.service.AlbumService;
import com.example.photogallery.service.CategoryService;
import com.example.photogallery.service.GalleryPhotoService;
import com.example.photogallery.service.GalleryService;
import com.example.photogallery.service.PhotoService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GalleryController {

    private final GalleryService galleryService;
    private final GalleryPhotoService galleryPhotoService;
    private final PhotoService photoService;
    private final CategoryService categoryService;
    private final AlbumService albumService;

    public GalleryController(
        PhotoService photoService,
        GalleryService galleryService,
        GalleryPhotoService galleryPhotoService,
        CategoryService categoryService,
        AlbumService albumService
    ) {
        this.photoService = photoService;
        this.galleryService = galleryService;
        this.galleryPhotoService = galleryPhotoService;
        this.categoryService = categoryService;
        this.albumService = albumService;
    }

    @GetMapping("/")
    public String listGalleriesRoot(Model model) {
        // Root: ONLY show galleries; no global photo dump
        List<Gallery> galleries = galleryService.getRootGalleries();
        model.addAttribute("categories", categoryService.listForCurrentTenant());
        model.addAttribute("albums", albumService.listForCurrentTenant());

        model.addAttribute("photos", List.of()); // empty – no global view
        model.addAttribute("currentSort", "uploadDate");
        model.addAttribute("searchQuery", null);
        model.addAttribute("isSearchResult", false);
        model.addAttribute("galleries", galleries);
        model.addAttribute("currentGallery", null); // important for template logic
        model.addAttribute("currentAlbum", null);
        model.addAttribute("flowMode", false);

        return "gallery";
    }

    @GetMapping("/gallery/{id}")
    public String viewGallery(
        @PathVariable("id") Long galleryId,
        @RequestParam(name = "sort", defaultValue = "uploadDate") String sort,
        Model model
    ) {
        Gallery currentGallery = galleryService.getGallery(galleryId);
        if (currentGallery.getSlug() != null && !currentGallery.getSlug().isBlank()) {
            return "redirect:/" + currentGallery.getSlug() + "?sort=" + sort;
        }
        List<Photo> photos = galleryPhotoService.getPhotosInGallery(galleryId);
        List<Gallery> galleries = galleryService.getRootGalleries();
        model.addAttribute("categories", categoryService.listForCurrentTenant());
        model.addAttribute("albums", albumService.listForCurrentTenant());

        model.addAttribute("photos", photos);
        model.addAttribute("currentSort", sort);
        model.addAttribute("searchQuery", null);
        model.addAttribute("isSearchResult", false);
        model.addAttribute("galleries", galleries);
        model.addAttribute("currentGallery", currentGallery);
        model.addAttribute("currentAlbum", currentGallery.getAlbum());
        model.addAttribute("flowMode", false);

        return "gallery";
    }

    @GetMapping("/{identifier}")
    public String viewGalleryByIdentifier(
        @PathVariable("identifier") String identifier,
        @RequestParam(name = "sort", defaultValue = "uploadDate") String sort,
        Model model
    ) {
        Gallery currentGallery = galleryService.getGalleryBySlugOrPublicId(identifier);
        List<Photo> photos = galleryPhotoService.getPhotosInGallery(currentGallery.getId());
        List<Gallery> galleries = galleryService.getRootGalleries();
        model.addAttribute("categories", categoryService.listForCurrentTenant());
        model.addAttribute("albums", albumService.listForCurrentTenant());

        model.addAttribute("photos", photos);
        model.addAttribute("currentSort", sort);
        model.addAttribute("searchQuery", null);
        model.addAttribute("isSearchResult", false);
        model.addAttribute("galleries", galleries);
        model.addAttribute("currentGallery", currentGallery);
        model.addAttribute("currentAlbum", currentGallery.getAlbum());
        model.addAttribute("flowMode", false);

        return "gallery";
    }

    @PostMapping("/upload")
    public String handleFileUpload(
        @RequestParam("files") MultipartFile[] files,
        @RequestParam(name = "galleryId") Long galleryId,
        @RequestParam(
            name = "duplicateMode",
            defaultValue = "cancel"
        ) String duplicateMode,
        @RequestParam(value = "redirectTo", required = false) String redirectTo,
        RedirectAttributes redirectAttributes
    ) {
        if (galleryId == null) {
            redirectAttributes.addFlashAttribute(
                "message",
                "You must select a gallery to upload photos."
            );
            return "redirect:/";
        }

        int successCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        PhotoService.DuplicateHandling handling =
            PhotoService.DuplicateHandling.fromString(duplicateMode);

        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                try {
                    Photo saved = photoService.savePhotoForGallery(
                        file,
                        galleryId,
                        handling
                    );
                    successCount++;

                    // attach to selected gallery (mandatory now)
                    try {
                        galleryPhotoService.addPhotoToGallery(
                            galleryId,
                            saved.getId()
                        );
                    } catch (RuntimeException linkEx) {
                        errorCount++;
                    }
                } catch (IllegalArgumentException ex) {
                    skippedCount++;
                } catch (RuntimeException ex) {
                    errorCount++;
                }
            }
        }

        StringBuilder msg = new StringBuilder();
        if (successCount > 0) {
            msg.append(successCount).append(" photo(s) uploaded. ");
        }
        if (skippedCount > 0) {
            msg
                .append(skippedCount)
                .append(" unsupported/duplicate file(s) skipped. ");
        }
        if (errorCount > 0) {
            msg.append(errorCount).append(" file(s) failed to upload.");
        }
        if (msg.length() == 0) {
            msg.append("No files were uploaded.");
        }

        redirectAttributes.addFlashAttribute("message", msg.toString().trim());
        Gallery gallery = galleryService.getGallery(galleryId);
        String target = redirectTo != null
            ? safeRedirect(redirectTo).replace("{galleryId}", galleryId.toString())
            : null;
        if (target != null && target.contains("{gallerySlug}")) {
            target = target.replace("{gallerySlug}", gallery.getSlug());
        }
        if (target == null) {
            target = gallery.getSlug() != null
                ? ("/" + gallery.getSlug())
                : ("/gallery/" + galleryId);
        }
        return "redirect:" + target;
    }

    @DeleteMapping("/photo/{id}/delete")
    public String deletePhoto(
        @PathVariable Long id,
        RedirectAttributes redirectAttributes
    ) {
        try {
            photoService.deletePhoto(id);
            redirectAttributes.addFlashAttribute("message", "Photo deleted.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Failed to delete photo."
            );
        }
        return "redirect:/";
    }

    @PostMapping("/categories")
    public String createCategory(
        @RequestParam("name") String name,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(value = "redirectTo", required = false) String redirectTo,
        RedirectAttributes redirectAttributes
    ) {
        try {
            var created = categoryService.create(name, description);
            redirectAttributes.addFlashAttribute(
                "message",
                "Category created."
            );
            if (redirectTo != null) {
                String target = safeRedirect(redirectTo).replace(
                    "{categoryId}",
                    created.getId().toString()
                );
                return "redirect:" + target;
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Failed to create category: " + ex.getMessage()
            );
        }
        return "redirect:/";
    }

    @PostMapping("/albums")
    public String createAlbum(
        @RequestParam("name") String name,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(value = "categoryId", required = false) Long categoryId,
        @RequestParam(value = "redirectTo", required = false) String redirectTo,
        RedirectAttributes redirectAttributes
    ) {
        try {
            var category = categoryId != null
                ? categoryService.getById(categoryId)
                : categoryService.getOrCreateDefaultCategory();
            var created = albumService.create(category, name, description);
            redirectAttributes.addFlashAttribute("message", "Album created.");
            if (redirectTo != null) {
                String target = safeRedirect(redirectTo).replace(
                    "{albumId}",
                    created.getId().toString()
                );
                return "redirect:" + target;
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Failed to create album: " + ex.getMessage()
            );
        }
        return "redirect:/";
    }

    @PostMapping("/galleries")
    public String createGallery(
        @RequestParam("title") String title,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(value = "albumId", required = false) Long albumId,
        @RequestParam(value = "parentId", required = false) Long parentId,
        @RequestParam(value = "redirectTo", required = false) String redirectTo,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Gallery created;
            if (parentId != null) {
                created =
                    galleryService.createChildGallery(
                        parentId,
                        title,
                        description,
                        albumId
                    );
            } else if (albumId != null) {
                created =
                    galleryService.createRootGalleryInAlbum(
                        albumId,
                        title,
                        description
                    );
            } else {
                created = galleryService.createRootGallery(title, description);
            }
            redirectAttributes.addFlashAttribute(
                "message",
                "Gallery created: #" + created.getId()
            );
            if (redirectTo != null) {
                String target = safeRedirect(redirectTo).replace(
                    "{galleryId}",
                    created.getId().toString()
                );
                if (target.contains("{gallerySlug}")) {
                    target = target.replace("{gallerySlug}", created.getSlug());
                }
                return "redirect:" + target;
            }
            return created.getSlug() != null
                ? ("redirect:/" + created.getSlug())
                : ("redirect:/gallery/" + created.getId());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Failed to create gallery: " + ex.getMessage()
            );
            return "redirect:/";
        }
    }

    @PutMapping("/galleries/{id}")
    public String updateGallery(
        @PathVariable("id") Long id,
        @RequestParam("title") String title,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(value = "visibility", required = false) String visibility,
        @RequestParam(value = "redirectTo", required = false) String redirectTo,
        RedirectAttributes redirectAttributes
    ) {
        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Gallery title is required."
            );
            Gallery g = galleryService.getGallery(id);
            return g.getSlug() != null
                ? ("redirect:/" + g.getSlug())
                : ("redirect:/gallery/" + id);
        }

        try {
            galleryService.updateGallery(
                id,
                title.trim(),
                description,
                visibility
            );
            redirectAttributes.addFlashAttribute("message", "Gallery updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Failed to update gallery: " + ex.getMessage()
            );
        }

        String target = redirectTo != null
            ? safeRedirect(redirectTo).replace("{galleryId}", id.toString())
            : null;
        Gallery g = galleryService.getGallery(id);
        if (target != null && target.contains("{gallerySlug}")) {
            target = target.replace("{gallerySlug}", g.getSlug());
        }
        if (target == null) {
            target = g.getSlug() != null ? ("/" + g.getSlug()) : ("/gallery/" + id);
        }
        return "redirect:" + target;
    }

    @DeleteMapping("/galleries/{id}")
    public String deleteGallery(
        @PathVariable("id") Long id,
        @RequestParam(value = "redirectTo", required = false) String redirectTo,
        RedirectAttributes redirectAttributes
    ) {
        try {
            galleryService.deleteGallery(id);
            redirectAttributes.addFlashAttribute("message", "Gallery deleted.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Failed to delete gallery: " + ex.getMessage()
            );
            Gallery g = galleryService.getGallery(id);
            return g.getSlug() != null
                ? ("redirect:/" + g.getSlug())
                : ("redirect:/gallery/" + id);
        }

        String target = redirectTo != null ? safeRedirect(redirectTo) : "/dashboard";
        return "redirect:" + target;
    }

    private static String safeRedirect(String redirectTo) {
        String trimmed = redirectTo.trim();
        if (!trimmed.startsWith("/") || trimmed.contains("://")) {
            return "/";
        }
        return trimmed;
    }
}
