package com.example.photogallery.service;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Gallery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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

        List<Long> albumIds = albums
            .stream()
            .map(Album::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // One query for the first root gallery of every album...
        Map<Long, Gallery> firstGalleryByAlbum =
            galleryService.getFirstRootGalleryByAlbumId(albumIds);
        if (firstGalleryByAlbum.isEmpty()) {
            return result;
        }

        // ...and one query for those galleries' thumbnail photos.
        Map<Long, Long> thumbnailByGallery =
            galleryPhotoService.getThumbnailPhotoIdsForGalleries(
                new ArrayList<>(firstGalleryByAlbum.values())
            );

        for (Map.Entry<Long, Gallery> entry : firstGalleryByAlbum.entrySet()) {
            Long photoId = thumbnailByGallery.get(entry.getValue().getId());
            if (photoId != null) {
                result.put(entry.getKey(), photoId);
            }
        }
        return result;
    }
}
