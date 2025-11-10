package com.example.photogallery.repository;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.enums.AlbumType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    // Find root albums (no parent)
    List<Album> findByParentIsNullOrderBySortOrder();

    // Find albums by parent
    List<Album> findByParentOrderBySortOrder(Album parent);

    // Find albums by type
    List<Album> findByAlbumTypeOrderBySortOrder(AlbumType albumType);

    // Find root albums by type
    List<Album> findByParentIsNullAndAlbumTypeOrderBySortOrder(
        AlbumType albumType
    );

    // Find by slug
    Optional<Album> findBySlug(String slug);

    // Find featured albums
    List<Album> findByIsFeaturedTrueOrderBySortOrder();

    // Find public albums
    List<Album> findByIsPublicTrueOrderBySortOrder();

    // Find client visible albums
    List<Album> findByIsClientVisibleTrueOrderBySortOrder();

    // Find albums by client name
    List<Album> findByClientNameContainingIgnoreCaseOrderByShootDateDesc(
        String clientName
    );

    // Search albums by name
    List<Album> findByNameContainingIgnoreCaseOrderBySortOrder(String name);

    // Find albums with photos
    @Query(
        "SELECT a FROM Album a WHERE SIZE(a.photos) > 0 ORDER BY a.sortOrder"
    )
    List<Album> findAlbumsWithPhotos();

    // Find empty albums
    @Query(
        "SELECT a FROM Album a WHERE SIZE(a.photos) = 0 ORDER BY a.sortOrder"
    )
    List<Album> findEmptyAlbums();

    // Get album hierarchy path
    @Query(
        "SELECT a FROM Album a WHERE a.id = :albumId OR a.id IN " +
            "(SELECT p.id FROM Album p WHERE p.id IN " +
            "(SELECT DISTINCT parent.id FROM Album child WHERE child.id = :albumId))"
    )
    List<Album> findAlbumHierarchy(@Param("albumId") Long albumId);

    // Find direct children only (recursive queries can be complex for different databases)
    List<Album> findByParentIdOrderBySortOrder(Long parentId);

    // Find albums by date range
    @Query(
        "SELECT a FROM Album a WHERE a.shootDate BETWEEN :startDate AND :endDate ORDER BY a.shootDate DESC"
    )
    List<Album> findByShootDateBetween(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate
    );

    // Find recent albums
    @Query("SELECT a FROM Album a ORDER BY a.createdDate DESC")
    List<Album> findRecentAlbums();

    // Get album statistics
    @Query("SELECT a.albumType, COUNT(a) FROM Album a GROUP BY a.albumType")
    List<Object[]> getAlbumCountByType();

    // Find albums for portfolio (featured or portfolio ready)
    @Query(
        "SELECT DISTINCT a FROM Album a " +
            "LEFT JOIN a.photos p " +
            "WHERE a.isFeatured = true OR p.isPortfolioImage = true " +
            "ORDER BY a.sortOrder"
    )
    List<Album> findPortfolioAlbums();

    // Advanced search
    @Query(
        "SELECT DISTINCT a FROM Album a " +
            "LEFT JOIN a.photos p " +
            "WHERE (:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:clientName IS NULL OR LOWER(a.clientName) LIKE LOWER(CONCAT('%', :clientName, '%'))) " +
            "AND (:albumType IS NULL OR a.albumType = :albumType) " +
            "AND (:isPublic IS NULL OR a.isPublic = :isPublic) " +
            "AND (:isFeatured IS NULL OR a.isFeatured = :isFeatured) " +
            "ORDER BY a.sortOrder"
    )
    List<Album> searchAlbums(
        @Param("name") String name,
        @Param("clientName") String clientName,
        @Param("albumType") AlbumType albumType,
        @Param("isPublic") Boolean isPublic,
        @Param("isFeatured") Boolean isFeatured
    );
}
