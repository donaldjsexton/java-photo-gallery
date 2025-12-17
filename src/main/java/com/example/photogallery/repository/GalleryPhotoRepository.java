package com.example.photogallery.repository;

import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.GalleryPhoto;
import com.example.photogallery.model.Photo;
import com.example.photogallery.model.Tenant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryPhotoRepository
    extends JpaRepository<GalleryPhoto, Long> {
    List<GalleryPhoto> findByGalleryOrderBySortOrderAscAddedAtAsc(
        Gallery gallery
    );

    List<GalleryPhoto> findByGalleryIdAndTenantOrderBySortOrderAscAddedAtAsc(
        Long galleryId,
        Tenant tenant
    );

    Optional<GalleryPhoto> findFirstByGalleryIdAndTenantOrderBySortOrderAscAddedAtAsc(
        Long galleryId,
        Tenant tenant
    );

    Optional<GalleryPhoto> findByGalleryAndPhoto(Gallery gallery, Photo photo);

    boolean existsByGalleryIdAndPhotoIdAndTenant(
        Long galleryId,
        Long photoId,
        Tenant tenant
    );

    void deleteByGalleryAndPhoto(Gallery gallery, Photo photo);

    void deleteByPhotoIdAndTenant(Long photoId, Tenant tenant);

    void deleteByGalleryIdAndTenant(Long galleryId, Tenant tenant);
}
