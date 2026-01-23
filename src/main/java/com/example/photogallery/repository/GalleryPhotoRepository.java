package com.example.photogallery.repository;

import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.GalleryPhoto;
import com.example.photogallery.model.Photo;
import com.example.photogallery.model.Album;
import com.example.photogallery.model.Tenant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GalleryPhotoRepository
    extends JpaRepository<GalleryPhoto, Long> {
    List<GalleryPhoto> findByGalleryOrderBySortOrderAscAddedAtAsc(
        Gallery gallery
    );

    List<GalleryPhoto> findByGalleryIdAndTenantOrderBySortOrderAscAddedAtAsc(
        Long galleryId,
        Tenant tenant
    );

    @Query(
        """
        SELECT gp FROM GalleryPhoto gp
        JOIN FETCH gp.photo p
        WHERE gp.gallery.id = :galleryId
          AND gp.tenant = :tenant
        ORDER BY
            CASE WHEN gp.sortOrder IS NULL THEN 1 ELSE 0 END,
            gp.sortOrder ASC,
            gp.addedAt ASC
        """
    )
    List<GalleryPhoto> findByGalleryIdAndTenantWithPhotoOrderBySortOrderAscAddedAtAsc(
        @Param("galleryId") Long galleryId,
        @Param("tenant") Tenant tenant
    );

    @EntityGraph(attributePaths = "photo")
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

    @Query(
        """
        SELECT (COUNT(gp) > 0) FROM GalleryPhoto gp
        WHERE gp.tenant = :tenant
          AND gp.gallery.album = :album
          AND gp.photo.id = :photoId
        """
    )
    boolean existsByTenantAndAlbumAndPhotoId(
        @Param("tenant") Tenant tenant,
        @Param("album") Album album,
        @Param("photoId") Long photoId
    );

    @Query(
        """
        SELECT gp.photo FROM GalleryPhoto gp
        WHERE gp.tenant = :tenant
          AND gp.gallery.album = :album
        ORDER BY
            CASE WHEN gp.gallery.createdAt IS NULL THEN 1 ELSE 0 END,
            gp.gallery.createdAt ASC,
            gp.gallery.id ASC,
            CASE WHEN gp.sortOrder IS NULL THEN 1 ELSE 0 END,
            gp.sortOrder ASC,
            gp.addedAt ASC
        """
    )
    List<Photo> findPhotosByTenantAndAlbumOrdered(
        @Param("tenant") Tenant tenant,
        @Param("album") Album album
    );

    @Query(
        """
        SELECT gp.photo FROM GalleryPhoto gp
        WHERE gp.tenant = :tenant
          AND gp.gallery = :gallery
        ORDER BY
            CASE WHEN gp.sortOrder IS NULL THEN 1 ELSE 0 END,
            gp.sortOrder ASC,
            gp.addedAt ASC
        """
    )
    List<Photo> findPhotosByTenantAndGalleryOrdered(
        @Param("tenant") Tenant tenant,
        @Param("gallery") Gallery gallery
    );
}
