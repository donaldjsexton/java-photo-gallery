package com.example.photogallery.controller;

import com.example.photogallery.model.Album;
import com.example.photogallery.service.AlbumCoverService;
import com.example.photogallery.service.AlbumService;
import com.example.photogallery.service.CategoryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private final CategoryService categoryService;
    private final AlbumService albumService;
    private final AlbumCoverService albumCoverService;

    public DashboardController(
        CategoryService categoryService,
        AlbumService albumService,
        AlbumCoverService albumCoverService
    ) {
        this.categoryService = categoryService;
        this.albumService = albumService;
        this.albumCoverService = albumCoverService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
        @RequestParam(name = "categoryId", required = false) Long categoryId,
        @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "12") int size,
        Model model
    ) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.min(Math.max(size, 1), 60);

        Page<Album> albumPage = albumService.searchForDashboardPage(
            categoryId,
            sort,
            query,
            PageRequest.of(pageIndex, pageSize)
        );
        List<Album> albums = albumPage.getContent();

        Map<Long, Long> albumThumbnails = new HashMap<>();
        try {
            albumThumbnails = albumCoverService.deriveCoverPhotoIds(albums);
        } catch (RuntimeException ignored) {}
        model.addAttribute("categories", categoryService.listForCurrentTenant());
        model.addAttribute("albums", albums);
        model.addAttribute("albumPage", albumPage);
        model.addAttribute("albumThumbnails", albumThumbnails);
        model.addAttribute("currentCategoryId", categoryId);
        model.addAttribute("currentSort", sort);
        model.addAttribute("searchQuery", query);
        model.addAttribute("photos", List.of());
        model.addAttribute("currentGallery", null);
        model.addAttribute("currentAlbum", null);
        model.addAttribute("flowMode", false);
        return "dashboard";
    }
}
