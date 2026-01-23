package com.example.photogallery.controller;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.Photo;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.PhotoRepository;
import com.example.photogallery.service.AlbumService;
import com.example.photogallery.service.DownloadService;
import com.example.photogallery.service.GalleryService;
import com.example.photogallery.service.PhotoVariant;
import com.example.photogallery.service.TenantService;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.NoSuchElementException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
public class DownloadController {

    private final TenantService tenantService;
    private final AlbumService albumService;
    private final GalleryService galleryService;
    private final PhotoRepository photoRepository;
    private final DownloadService downloadService;

    public DownloadController(
        TenantService tenantService,
        AlbumService albumService,
        GalleryService galleryService,
        PhotoRepository photoRepository,
        DownloadService downloadService
    ) {
        this.tenantService = tenantService;
        this.albumService = albumService;
        this.galleryService = galleryService;
        this.photoRepository = photoRepository;
        this.downloadService = downloadService;
    }

    @GetMapping("/photos/{photoId}/image")
    public ResponseEntity<StreamingResponseBody> viewPhoto(
        @PathVariable("photoId") Long photoId,
        @RequestParam(value = "variant", required = false) String variant
    ) throws IOException {
        Tenant tenant = tenantService.getCurrentTenant();
        Photo photo = photoRepository
            .findByIdAndTenant(photoId, tenant)
            .orElseThrow(() -> new NoSuchElementException("Photo not found"));

        PhotoVariant v = PhotoVariant.fromString(variant);
        DownloadService.ResolvedDownload resolved;
        try {
            resolved = downloadService.openForDownload(tenant, photo, v);
        } catch (FileNotFoundException e) {
            throw new NoSuchElementException("Photo file not found");
        }

        MediaType mediaType = resolved.mediaType();
        String dispositionType = downloadService.isInlineSafe(mediaType)
            ? "inline"
            : "attachment";
        StreamingResponseBody body = out -> {
            try (var in = resolved.stream()) {
                in.transferTo(out);
            }
        };

        return ResponseEntity
            .ok()
            .contentType(mediaType)
            .cacheControl(CacheControl.noCache())
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                dispositionType + "; filename=\"" + resolved.fileName() + "\""
            )
            .body(body);
    }

    @GetMapping("/photos/{photoId}/download")
    public ResponseEntity<StreamingResponseBody> downloadPhoto(
        @PathVariable("photoId") Long photoId,
        @RequestParam(value = "variant", required = false) String variant
    ) throws IOException {
        Tenant tenant = tenantService.getCurrentTenant();
        Photo photo = photoRepository
            .findByIdAndTenant(photoId, tenant)
            .orElseThrow(() -> new NoSuchElementException("Photo not found"));

        PhotoVariant v = PhotoVariant.fromString(variant);
        DownloadService.ResolvedDownload resolved;
        try {
            resolved = downloadService.openForDownload(tenant, photo, v);
        } catch (FileNotFoundException e) {
            throw new NoSuchElementException("Photo file not found");
        }
        StreamingResponseBody body = out -> {
            try (var in = resolved.stream()) {
                in.transferTo(out);
            }
        };

        return ResponseEntity
            .ok()
            .contentType(resolved.mediaType())
            .cacheControl(CacheControl.noCache())
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + resolved.fileName() + "\""
            )
            .body(body);
    }

    @GetMapping("/albums/{albumId}/download.zip")
    public ResponseEntity<StreamingResponseBody> downloadAlbumZip(
        @PathVariable("albumId") Long albumId,
        @RequestParam(value = "variant", required = false) String variant
    ) {
        Album album = albumService.getById(albumId);
        Tenant tenant = tenantService.getCurrentTenant();
        PhotoVariant v = PhotoVariant.fromString(variant);

        String fileName = downloadService.buildAlbumZipFileName(album, v);
        StreamingResponseBody body = out -> {
            try {
                downloadService.writeAlbumZip(out, tenant, album, v);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\""
            )
            .cacheControl(CacheControl.noCache())
            .body(body);
    }

    @GetMapping("/galleries/{galleryId}/download.zip")
    public ResponseEntity<StreamingResponseBody> downloadGalleryZip(
        @PathVariable("galleryId") Long galleryId,
        @RequestParam(value = "variant", required = false) String variant
    ) {
        Gallery gallery = galleryService.getGallery(galleryId);
        Tenant tenant = tenantService.getCurrentTenant();
        PhotoVariant v = PhotoVariant.fromString(variant);

        String fileName = downloadService.buildGalleryZipFileName(gallery, v);
        StreamingResponseBody body = out -> {
            try {
                downloadService.writeGalleryZip(out, tenant, gallery, v);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\""
            )
            .cacheControl(CacheControl.noCache())
            .body(body);
    }
}
