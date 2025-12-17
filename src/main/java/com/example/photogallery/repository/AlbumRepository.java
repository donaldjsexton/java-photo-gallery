package com.example.photogallery.repository;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Category;
import com.example.photogallery.model.Tenant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findByTenant(Tenant tenant);
    List<Album> findByTenantAndCategory(Tenant tenant, Category category);
    Optional<Album> findFirstByTenantAndNameOrderByIdAsc(Tenant tenant, String name);
    Optional<Album> findByIdAndTenant(Long id, Tenant tenant);
}
