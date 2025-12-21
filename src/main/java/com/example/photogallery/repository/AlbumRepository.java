package com.example.photogallery.repository;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Category;
import com.example.photogallery.model.Tenant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Sort;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findByTenant(Tenant tenant);
    List<Album> findByTenantAndCategory(Tenant tenant, Category category);
    Optional<Album> findFirstByTenantAndNameOrderByIdAsc(Tenant tenant, String name);
    Optional<Album> findByIdAndTenant(Long id, Tenant tenant);

    @Query(
        """
        SELECT a FROM Album a
        WHERE a.tenant = :tenant
          AND (:category IS NULL OR a.category = :category)
          AND (
            :q IS NULL OR
            LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        """
    )
    List<Album> searchForTenant(
        @Param("tenant") Tenant tenant,
        @Param("category") Category category,
        @Param("q") String q,
        Sort sort
    );

    @Query(
        """
        SELECT a FROM Album a
        WHERE a.tenant = :tenant
          AND (:category IS NULL OR a.category = :category)
          AND (
            :q IS NULL OR
            LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        """
    )
    Page<Album> searchForTenant(
        @Param("tenant") Tenant tenant,
        @Param("category") Category category,
        @Param("q") String q,
        Pageable pageable
    );
}
