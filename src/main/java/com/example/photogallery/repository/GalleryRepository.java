package com.example.photogallery.repository;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.Tenant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {
    List<Gallery> findByTenantAndParentIsNull(Tenant tenant);
    List<Gallery> findByTenantAndParentId(Tenant tenant, Long parentId);
    List<Gallery> findByTenant(Tenant tenant);
    java.util.Optional<Gallery> findByIdAndTenant(Long id, Tenant tenant);
    List<Gallery> findByTenantAndAlbum(Tenant tenant, Album album);
}
