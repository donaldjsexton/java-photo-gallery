package com.example.photogallery.controller;

import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.Photo;
import com.example.photogallery.service.GalleryPhotoService;
import com.example.photogallery.service.GalleryService;
import com.example.photogallery.service.SignedUrlService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/g")
@ConditionalOnBean(SignedUrlService.class)
public class GallerySignedUrlController {

    private final GalleryService galleryService;
    private final GalleryPhotoService galleryPhotoService;
    private final SignedUrlService signedUrlService;

    public GallerySignedUrlController(
        GalleryService galleryService,
        GalleryPhotoService galleryPhotoService,
        SignedUrlService signedUrlService
    ) {
        this.galleryService = galleryService;
        this.galleryPhotoService = galleryPhotoService;
        this.signedUrlService = signedUrlService;
    }

    @GetMapping("/{gallerySlug}")
    public ResponseEntity<GalleryResponse> getGallery(
        @PathVariable("gallerySlug") String gallerySlug
    ) {
        Gallery gallery = galleryService.getGalleryBySlugOrPublicId(
            gallerySlug
        );
        List<Photo> photos = galleryPhotoService.getPhotosInGallery(
            gallery.getId()
        );
        List<GalleryAsset> assets = new ArrayList<>();

        for (Photo photo : photos) {
            String objectKey = resolveObjectKey(photo);
            if (!StringUtils.hasText(objectKey)) {
                continue;
            }
            for (AssetVariant variant : AssetVariant.values()) {
                Duration ttl = ttlForVariant(variant);
                boolean useCdn = variant != AssetVariant.DOWNLOAD;
                String signedUrl = signedUrlService.signGetObjectUrl(
                    objectKey,
                    ttl,
                    useCdn
                );
                assets.add(
                    new GalleryAsset(
                        photo.getId(),
                        variant,
                        objectKey,
                        signedUrl
                    )
                );
            }
        }

        return ResponseEntity.ok(new GalleryResponse(toDto(gallery), assets));
    }

    private static Duration ttlForVariant(AssetVariant variant) {
        return variant == AssetVariant.DOWNLOAD
            ? Duration.ofMinutes(5)
            : Duration.ofMinutes(15);
    }

    private static GalleryDto toDto(Gallery gallery) {
        return new GalleryDto(
            gallery.getId(),
            gallery.getPublicId(),
            gallery.getSlug(),
            gallery.getTitle(),
            gallery.getDescription(),
            gallery.getVisibility()
        );
    }

    private static String resolveObjectKey(Photo photo) {
        if (photo == null) {
            return null;
        }
        if (StringUtils.hasText(photo.getFileName())) {
            return photo.getFileName();
        }
        return photo.getOriginalName();
    }

    public enum AssetVariant {
        THUMB,
        WEB,
        DOWNLOAD
    }

    public record GalleryResponse(
        GalleryDto gallery,
        List<GalleryAsset> assets
    ) {}

    public record GalleryDto(
        Long id,
        UUID publicId,
        String slug,
        String title,
        String description,
        String visibility
    ) {}

    public record GalleryAsset(
        Long photoId,
        AssetVariant variant,
        String objectKey,
        String signedUrl
    ) {}
}
