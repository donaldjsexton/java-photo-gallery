package com.example.photogallery.controller;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Category;
import com.example.photogallery.model.Gallery;
import com.example.photogallery.service.AlbumService;
import com.example.photogallery.service.CategoryService;
import com.example.photogallery.service.GalleryService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AlbumFlowController {

    private final CategoryService categoryService;
    private final AlbumService albumService;
    private final GalleryService galleryService;

    public AlbumFlowController(
        CategoryService categoryService,
        AlbumService albumService,
        GalleryService galleryService
    ) {
        this.categoryService = categoryService;
        this.albumService = albumService;
        this.galleryService = galleryService;
    }

    @GetMapping("/flow/album")
    public String albumFlow(
        @RequestParam(name = "step", defaultValue = "1") int step,
        @RequestParam(name = "categoryId", required = false) Long categoryId,
        @RequestParam(name = "albumId", required = false) Long albumId,
        @RequestParam(name = "galleryId", required = false) Long galleryId,
        Model model
    ) {
        List<Category> categories = categoryService.listForCurrentTenant();
        List<Album> albums = albumService.listForCurrentTenant();
        List<Gallery> galleries = galleryService.getRootGalleries();

        model.addAttribute("categories", categories);
        model.addAttribute("albums", albums);
        model.addAttribute("galleries", galleries);
        model.addAttribute("photos", List.of());
        model.addAttribute("currentSort", "uploadDate");
        model.addAttribute("searchQuery", null);
        model.addAttribute("isSearchResult", false);

        model.addAttribute("flowMode", true);
        model.addAttribute("flowStep", Math.min(Math.max(step, 1), 4));

        if (categoryId != null) {
            try {
                model.addAttribute(
                    "currentCategory",
                    categoryService.getById(categoryId)
                );
            } catch (RuntimeException ignored) {}
        }
        if (albumId != null) {
            try {
                model.addAttribute(
                    "currentAlbum",
                    albumService.getById(albumId)
                );
            } catch (RuntimeException ignored) {}
        }
        if (galleryId != null) {
            try {
                model.addAttribute(
                    "currentGallery",
                    galleryService.getGallery(galleryId)
                );
            } catch (RuntimeException ignored) {}
        }

        return "gallery";
    }
}

