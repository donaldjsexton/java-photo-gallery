package com.example.photogallery.controller;

import com.example.photogallery.model.Photo;
import com.example.photogallery.service.PhotoSearchService;
import com.example.photogallery.service.PhotoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/photos")
public class PhotoRestController {

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

    public PhotoRestController(
        PhotoService photoService,
        PhotoSearchService photoSearchService
    ) {
        this.photoService = photoService;
        this.photoSearchService = photoSearchService;
    }

    // onDuplicate = cancel | skip | overwrite
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Photo> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(name = "galleryId", required = false) Long galleryId,
        @RequestParam(
            name = "onDuplicate",
            defaultValue = "cancel"
        ) String onDuplicate
    ) {
        PhotoService.DuplicateHandling handling =
            PhotoService.DuplicateHandling.fromString(onDuplicate);

        Photo saved = galleryId != null
            ? photoService.savePhotoForGallery(file, galleryId, handling)
            : photoService.savePhoto(file, handling);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // PUT /api/photos/{id} — replace file
    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Photo> replace(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file
    ) {
        Photo updated = photoService.updatePhoto(id, file);
        return ResponseEntity.ok(updated);
    }

    // GET /api/photos — list; sortBy in {uploadDate,dateTaken,dateTakenAsc,camera,withCamera,withDateTaken}
    @GetMapping
    public ResponseEntity<List<Photo>> list(
        @RequestParam(
            name = "sortBy",
            defaultValue = "uploadDate"
        ) String sortBy
    ) {
        String effectiveSort = ALLOWED_SORTS.contains(sortBy)
            ? sortBy
            : "uploadDate";
        List<Photo> photos = photoService.getAllPhotosSorted(effectiveSort);
        return ResponseEntity.ok(photos);
    }

    // GET /api/photos/{id} — details
    @GetMapping("/{id}")
    public ResponseEntity<Photo> getOne(@PathVariable Long id) {
        Photo p = photoService.getPhotoById(id);
        if (p == null) {
            // Let GlobalExceptionHandler turn this into a 404 JSON
            throw new NoSuchElementException("Photo not found with id " + id);
        }
        return ResponseEntity.ok(p);
    }

    // DELETE /api/photos/{id} — remove
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        photoService.deletePhoto(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/photos/search?query=&camera=&start=&end=&page=&size=
    @GetMapping("/search")
    public ResponseEntity<Page<Photo>> search(
        @RequestParam(name = "query", required = false) String query,
        @RequestParam(name = "camera", required = false) String camera,
        @RequestParam(name = "start", required = false) String start,
        @RequestParam(name = "end", required = false) String end,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
            Math.max(0, page),
            Math.min(Math.max(1, size), 200)
        );
        LocalDateTime startDt = parseDateTimeStart(start);
        LocalDateTime endDt = parseDateTimeEnd(end);

        Page<Photo> results = photoSearchService.advancedSearch(
            normalize(query),
            normalize(camera),
            startDt,
            endDt,
            pageable
        );
        return ResponseEntity.ok(results);
    }

    // --- helpers ---

    private static String normalize(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static LocalDateTime parseDateTimeStart(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            // Try full ISO date-time first
            return LocalDateTime.parse(raw.trim());
        } catch (DateTimeParseException ignored) {
            // Fallback to date-only at start-of-day
            try {
                LocalDate d = LocalDate.parse(raw.trim());
                return d.atStartOfDay();
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }

    private static LocalDateTime parseDateTimeEnd(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (DateTimeParseException ignored) {
            try {
                LocalDate d = LocalDate.parse(raw.trim());
                // end-of-day (inclusive)
                return d.atTime(23, 59, 59);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }
}
