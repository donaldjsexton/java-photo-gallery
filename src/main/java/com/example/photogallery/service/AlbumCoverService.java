package com.example.photogallery.service;

import com.example.photogallery.model.Album;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AlbumCoverService {

    private final GalleryService galleryService;
    private final GalleryPhotoService galleryPhotoService;

    public AlbumCoverService(
        GalleryService galleryService,
        GalleryPhotoService galleryPhotoService
    ) {
        this.galleryService = galleryService;
        this.galleryPhotoService = galleryPhotoService;
    }

    public Long deriveCoverPhotoId(Album album) {
        if (album == null) {
            return null;
        }

        var galleries = galleryService.getRootGalleriesForAlbum(album);
        if (galleries == null || galleries.isEmpty()) {
            return null;
        }

        var firstGallery = galleries.get(0);
        var photo = galleryPhotoService.getThumbnailPhotoForGallery(firstGallery.getId());
        return photo != null ? photo.getId() : null;
    }

    public Map<Long, Long> deriveCoverPhotoIds(List<Album> albums) {
        Map<Long, Long> result = new HashMap<>();
        if (albums == null || albums.isEmpty()) {
            return result;
        }

        for (Album album : albums) {
            Long photoId = deriveCoverPhotoId(album);
            if (photoId != null) {
                result.put(album.getId(), photoId);
            }
        }
        return result;
    }
}
