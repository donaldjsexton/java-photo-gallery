package com.example.photogallery.controller;

import com.example.photogallery.service.AlbumService;
import com.example.photogallery.service.CategoryService;
import com.example.photogallery.service.PhotoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LibraryMaintenanceRestController {

    private final AlbumService albumService;
    private final CategoryService categoryService;
    private final PhotoService photoService;

    public LibraryMaintenanceRestController(
        AlbumService albumService,
        CategoryService categoryService,
        PhotoService photoService
    ) {
        this.albumService = albumService;
        this.categoryService = categoryService;
        this.photoService = photoService;
    }

    @DeleteMapping("/albums/{id}")
    public ResponseEntity<Void> deleteAlbum(@PathVariable("id") Long id) {
        albumService.deleteAlbum(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/maintenance/purge-orphaned-photos")
    public ResponseEntity<PurgeResult> purgeOrphanedPhotos() {
        int deleted = photoService.purgeOrphanedPhotosForCurrentTenant();
        return ResponseEntity.ok(new PurgeResult(deleted));
    }

    public record PurgeResult(int deletedPhotos) {}
}

