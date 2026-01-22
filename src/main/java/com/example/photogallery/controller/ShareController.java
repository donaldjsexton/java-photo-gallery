package com.example.photogallery.controller;

import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.Album;
import com.example.photogallery.model.Photo;
import com.example.photogallery.model.ShareToken;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.GalleryPhotoRepository;
import com.example.photogallery.repository.GalleryRepository;
import com.example.photogallery.repository.PhotoRepository;
import com.example.photogallery.service.DownloadService;
import com.example.photogallery.service.PhotoVariant;
import com.example.photogallery.service.ShareTokenService;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
public class ShareController {

    private final ShareTokenService shareTokenService;
    private final GalleryRepository galleryRepository;
    private final GalleryPhotoRepository galleryPhotoRepository;
    private final PhotoRepository photoRepository;
    private final DownloadService downloadService;

    public ShareController(
        ShareTokenService shareTokenService,
        GalleryRepository galleryRepository,
        GalleryPhotoRepository galleryPhotoRepository,
        PhotoRepository photoRepository,
        DownloadService downloadService
    ) {
        this.shareTokenService = shareTokenService;
        this.galleryRepository = galleryRepository;
        this.galleryPhotoRepository = galleryPhotoRepository;
        this.photoRepository = photoRepository;
        this.downloadService = downloadService;
    }

    @GetMapping("/share/{tokenId}")
    public String viewSharedAlbum(
        @PathVariable("tokenId") UUID tokenId,
        Model model
    ) {
        ShareToken token = shareTokenService.resolveValid(tokenId);
        Tenant tenant = token.getTenant();
        var album = token.getAlbum();

        List<Gallery> galleries =
            galleryRepository.findByTenantAndAlbumAndParentIsNullOrderByCreatedAtDesc(
                tenant,
                album
            );

        Map<Long, Long> galleryThumbnails = new HashMap<>();
        for (Gallery g : galleries) {
            galleryPhotoRepository
                .findFirstByGalleryIdAndTenantOrderBySortOrderAscAddedAtAsc(
                    g.getId(),
                    tenant
                )
                .map(gp -> gp.getPhoto().getId())
                .ifPresent(photoId -> galleryThumbnails.put(g.getId(), photoId));
        }

        model.addAttribute("shareTokenId", token.getId());
        model.addAttribute("currentAlbum", album);
        model.addAttribute("galleries", galleries);
        model.addAttribute("galleryThumbnails", galleryThumbnails);
        return "share-album";
    }

    @GetMapping("/share/{tokenId}/g/{identifier}")
    public String viewSharedGallery(
        @PathVariable("tokenId") UUID tokenId,
        @PathVariable("identifier") String identifier,
        Model model
    ) {
        ShareToken token = shareTokenService.resolveValid(tokenId);
        Tenant tenant = token.getTenant();
        var album = token.getAlbum();

        Gallery gallery = resolveGalleryInAlbum(tenant, album, identifier);
        List<Photo> photos = galleryPhotoRepository
            .findByGalleryIdAndTenantOrderBySortOrderAscAddedAtAsc(
                gallery.getId(),
                tenant
            )
            .stream()
            .map(gp -> gp.getPhoto())
            .toList();

        model.addAttribute("shareTokenId", token.getId());
        model.addAttribute("currentAlbum", album);
        model.addAttribute("currentGallery", gallery);
        model.addAttribute("photos", photos);
        return "share-gallery";
    }

    @GetMapping("/share/{tokenId}/photo/{photoId}")
    public ResponseEntity<StreamingResponseBody> viewSharedPhoto(
        @PathVariable("tokenId") UUID tokenId,
        @PathVariable("photoId") Long photoId
    ) {
        try {
            ShareToken token = shareTokenService.resolveValid(tokenId);
            Tenant tenant = token.getTenant();
            var album = token.getAlbum();

            boolean allowed = galleryPhotoRepository.existsByTenantAndAlbumAndPhotoId(
                tenant,
                album,
                photoId
            );
            if (!allowed) {
                return ResponseEntity.notFound().build();
            }

            Photo photo = photoRepository
                .findByIdAndTenant(photoId, tenant)
                .orElse(null);
            if (photo == null) {
                return ResponseEntity.notFound().build();
            }

            DownloadService.ResolvedDownload resolved;
            try {
                resolved = downloadService.openForDownload(tenant, photo, null);
            } catch (FileNotFoundException e) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = resolved.mediaType();
            StreamingResponseBody body = out -> {
                try (var in = resolved.stream()) {
                    in.transferTo(out);
                }
            };

            return ResponseEntity
                .ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(body);
        } catch (NoSuchElementException | IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/share/{tokenId}/photo/{photoId}/download")
    public ResponseEntity<StreamingResponseBody> downloadSharedPhoto(
        @PathVariable("tokenId") UUID tokenId,
        @PathVariable("photoId") Long photoId,
        @RequestParam(value = "variant", required = false) String variant
    ) throws IOException {
        ShareToken token = shareTokenService.resolveValid(tokenId);
        Tenant tenant = token.getTenant();
        Album album = token.getAlbum();

        boolean allowed = galleryPhotoRepository.existsByTenantAndAlbumAndPhotoId(
            tenant,
            album,
            photoId
        );
        if (!allowed) {
            return ResponseEntity.notFound().build();
        }

        Photo photo = photoRepository
            .findByIdAndTenant(photoId, tenant)
            .orElseThrow(() -> new NoSuchElementException("Photo not found"));

        PhotoVariant v = PhotoVariant.fromString(variant);
        DownloadService.ResolvedDownload resolved;
        try {
            resolved = downloadService.openForDownload(tenant, photo, v);
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
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

    @GetMapping("/share/{tokenId}/download.zip")
    public ResponseEntity<StreamingResponseBody> downloadSharedAlbumZip(
        @PathVariable("tokenId") UUID tokenId,
        @RequestParam(value = "variant", required = false) String variant
    ) {
        ShareToken token = shareTokenService.resolveValid(tokenId);
        Tenant tenant = token.getTenant();
        Album album = token.getAlbum();
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

    private Gallery resolveGalleryInAlbum(
        Tenant tenant,
        Album album,
        String identifier
    ) {
        if (identifier == null || identifier.isBlank()) {
            throw new NoSuchElementException("Gallery not found");
        }

        String trimmed = identifier.trim();
        try {
            UUID publicId = UUID.fromString(trimmed);
            return galleryRepository
                .findByTenantAndAlbumAndPublicId(tenant, album, publicId)
                .orElseThrow(() -> new NoSuchElementException("Gallery not found"));
        } catch (IllegalArgumentException ignored) {}

        return galleryRepository
            .findByTenantAndAlbumAndSlug(tenant, album, trimmed)
            .orElseThrow(() -> new NoSuchElementException("Gallery not found"));
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "share-error";
    }
}
