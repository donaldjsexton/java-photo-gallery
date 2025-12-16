package com.example.photogallery.controller;

import com.example.photogallery.model.Gallery;
import com.example.photogallery.service.AlbumService;
import com.example.photogallery.service.CategoryService;
import com.example.photogallery.service.GalleryPhotoService;
import com.example.photogallery.service.GalleryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final GalleryService galleryService;
    private final GalleryPhotoService galleryPhotoService;
    private final CategoryService categoryService;
    private final AlbumService albumService;

    public DashboardController(
        GalleryService galleryService,
        GalleryPhotoService galleryPhotoService,
        CategoryService categoryService,
        AlbumService albumService
    ) {
        this.galleryService = galleryService;
        this.galleryPhotoService = galleryPhotoService;
        this.categoryService = categoryService;
        this.albumService = albumService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Gallery> galleries = galleryService.getRootGalleries();
        Map<Long, String> galleryThumbnails = new HashMap<>();
        for (Gallery g : galleries) {
            try {
                var photo = galleryPhotoService.getThumbnailPhotoForGallery(
                    g.getId()
                );
                if (photo != null) {
                    galleryThumbnails.put(g.getId(), photo.getFileName());
                }
            } catch (RuntimeException ignored) {}
        }
        model.addAttribute("categories", categoryService.listForCurrentTenant());
        model.addAttribute("albums", albumService.listForCurrentTenant());
        model.addAttribute("galleries", galleries);
        model.addAttribute("galleryThumbnails", galleryThumbnails);
        model.addAttribute("photos", List.of());
        model.addAttribute("currentGallery", null);
        model.addAttribute("currentAlbum", null);
        model.addAttribute("flowMode", false);
        return "dashboard";
    }
}
