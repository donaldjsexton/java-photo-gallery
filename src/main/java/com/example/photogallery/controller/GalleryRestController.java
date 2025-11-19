package com.example.photogallery.controller;

import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.Photo;
import com.example.photogallery.service.GalleryPhotoService;
import com.example.photogallery.service.GalleryService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/galleries")
public class GalleryRestController {

    private final GalleryService galleryService;
    private final GalleryPhotoService galleryPhotoService;

    public GalleryRestController(
        GalleryService galleryService,
        GalleryPhotoService galleryPhotoService
    ) {
        this.galleryService = galleryService;
        this.galleryPhotoService = galleryPhotoService;
    }

    // POST /api/galleries?title=...&description=...&parentId=...
    @PostMapping
    public ResponseEntity<Gallery> createGallery(
        @RequestParam("title") String title,
        @RequestParam(
            value = "description",
            required = false
        ) String description,
        @RequestParam(value = "parentId", required = false) Long parentId
    ) {
        Gallery gallery;
        if (parentId == null) {
            gallery = galleryService.createRootGallery(title, description);
        } else {
            gallery = galleryService.createChildGallery(
                parentId,
                title,
                description
            );
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(gallery);
    }

    // GET /api/galleries/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Gallery> getGallery(@PathVariable Long id) {
        Gallery g = galleryService.getGallery(id);
        return ResponseEntity.ok(g);
    }

    // GET /api/galleries          -> root galleries
    // GET /api/galleries?parentId -> children of that parent
    @GetMapping
    public ResponseEntity<List<Gallery>> listGalleries(
        @RequestParam(value = "parentId", required = false) Long parentId
    ) {
        List<Gallery> results;
        if (parentId == null) {
            results = galleryService.getRootGalleries();
        } else {
            results = galleryService.getChildren(parentId);
        }
        return ResponseEntity.ok(results);
    }

    // PUT /api/galleries/{id}?title=...&description=...&visibility=...
    @PutMapping("/{id}")
    public ResponseEntity<Gallery> updateGallery(
        @PathVariable Long id,
        @RequestParam(value = "title", required = false) String title,
        @RequestParam(
            value = "description",
            required = false
        ) String description,
        @RequestParam(value = "visibility", required = false) String visibility
    ) {
        Gallery updated = galleryService.updateGallery(
            id,
            title,
            description,
            visibility
        );
        return ResponseEntity.ok(updated);
    }

    // DELETE /api/galleries/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGallery(@PathVariable Long id) {
        galleryService.deleteGallery(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Photos in a gallery ----

    // GET /api/galleries/{id}/photos
    @GetMapping("/{id}/photos")
    public ResponseEntity<List<Photo>> getPhotosInGallery(
        @PathVariable Long id
    ) {
        List<Photo> photos = galleryPhotoService.getPhotosInGallery(id);
        return ResponseEntity.ok(photos);
    }

    // POST /api/galleries/{id}/photos/{photoId}
    @PostMapping("/{id}/photos/{photoId}")
    public ResponseEntity<Void> addPhotoToGallery(
        @PathVariable Long id,
        @PathVariable Long photoId,
        @RequestParam(value = "sortOrder", required = false) Integer sortOrder
    ) {
        if (sortOrder == null) {
            galleryPhotoService.addPhotoToGallery(id, photoId);
        } else {
            galleryPhotoService.addPhotoToGallery(id, photoId, sortOrder);
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // DELETE /api/galleries/{id}/photos/{photoId}
    @DeleteMapping("/{id}/photos/{photoId}")
    public ResponseEntity<Void> removePhotoFromGallery(
        @PathVariable Long id,
        @PathVariable Long photoId
    ) {
        galleryPhotoService.removePhotoFromGallery(id, photoId);
        return ResponseEntity.noContent().build();
    }

    // POST /api/galleries/{id}/photos/reorder
    // Body: JSON array of photo IDs in desired order, e.g. [5,3,10]
    @PostMapping("/{id}/photos/reorder")
    public ResponseEntity<Void> reorderPhotos(
        @PathVariable Long id,
        @RequestBody List<Long> orderedPhotoIds
    ) {
        galleryPhotoService.reorderPhotos(id, orderedPhotoIds);
        return ResponseEntity.noContent().build();
    }
}
