package com.example.photogallery.repository;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.Tenant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {
    List<Gallery> findByTenantAndParentIsNull(Tenant tenant);
    List<Gallery> findByTenantAndParentId(Tenant tenant, Long parentId);
    List<Gallery> findByTenantAndParentIdIn(Tenant tenant, List<Long> parentIds);
    List<Gallery> findByTenant(Tenant tenant);
    java.util.Optional<Gallery> findByIdAndTenant(Long id, Tenant tenant);
    List<Gallery> findByTenantAndAlbum(Tenant tenant, Album album);

    Optional<Gallery> findByTenantAndPublicId(Tenant tenant, UUID publicId);
    Optional<Gallery> findByTenantAndSlug(Tenant tenant, String slug);
    boolean existsByTenantAndSlug(Tenant tenant, String slug);

    @Modifying
    @Query(
        "UPDATE Gallery g SET g.coverPhoto = null WHERE g.tenant = :tenant AND g.coverPhoto.id = :photoId"
    )
    int clearCoverPhotoReferences(
        @Param("tenant") Tenant tenant,
        @Param("photoId") Long photoId
    );
}
