package com.example.photogallery.repository;

import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.GalleryPhoto;
import com.example.photogallery.model.Photo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryPhotoRepository
    extends JpaRepository<GalleryPhoto, Long> {
    List<GalleryPhoto> findByGalleryOrderBySortOrderAscAddedAtAsc(
        Gallery gallery
    );

    List<GalleryPhoto> findByGalleryIdOrderBySortOrderAscAddedAtAsc(
        Long galleryId
    );

    Optional<GalleryPhoto> findByGalleryAndPhoto(Gallery gallery, Photo photo);

    void deleteByGalleryAndPhoto(Gallery gallery, Photo photo);
}
