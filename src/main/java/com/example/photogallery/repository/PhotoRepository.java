package com.example.photogallery.repository;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Photo;
import com.example.photogallery.model.enums.WorkflowStatus;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    Optional<Photo> findByFileHash(String fileHash);

    List<Photo> findAllByOrderByDateTakenDesc();

    List<Photo> findAllByOrderByDateTakenAsc();

    List<Photo> findAllByOrderByCameraAsc();

    List<Photo> findAllByOrderByUploadDateDesc();

    // Find photos with EXIF data
    @Query(
        "SELECT p FROM Photo p WHERE p.camera IS NOT NULL ORDER BY p.camera ASC"
    )
    List<Photo> findPhotosWithCamera();

    @Query(
        "SELECT p FROM Photo p WHERE p.dateTaken IS NOT NULL ORDER BY p.dateTaken DESC"
    )
    List<Photo> findPhotosWithDateTaken();

    @Query(
        "SELECT p FROM Photo p WHERE " +
            "LOWER(p.originalName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.camera) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.allExifData) LIKE LOWER(CONCAT('%', :query, '%'))"
    )
    Page<Photo> findByTextSearch(
        @Param("query") String query,
        Pageable pageable
    );

    @Query(
        "SELECT p FROM Photo p WHERE " +
            "p.dateTaken BETWEEN :start AND :end " +
            "ORDER BY p.dateTaken DESC"
    )
    Page<Photo> findByDateRange(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    @Query(
        "SELECT p FROM Photo p WHERE LOWER(p.cameraInfo) LIKE LOWER(CONCAT('%', :camera, '%'))"
    )
    Page<Photo> findByCamera(@Param("camera") String camera, Pageable pageable);

    // Combination search
    @Query(
        "SELECT p FROM Photo p WHERE " +
            "(:query IS NULL OR LOWER(p.searchableText) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:camera IS NULL OR LOWER(p.cameraInfo) LIKE LOWER(CONCAT('%', :camera, '%'))) " +
            "AND (:startDate IS NULL OR p.dateTakenParsed >= :startDate) " +
            "AND (:endDate IS NULL OR p.dateTakenParsed <= :endDate)"
    )
    Page<Photo> advancedSearch(
        @Param("query") String query,
        @Param("camera") String camera,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        Pageable pageable
    );

    // Album-related queries
    List<Photo> findByAlbumOrderBySortOrderInAlbumAsc(Album album);

    List<Photo> findByAlbumIsNullOrderByUploadDateDesc();

    List<Photo> findByAlbumOrderBySortOrderInAlbumAsc(
        Album album,
        Pageable pageable
    );

    // Workflow status queries
    List<Photo> findByWorkflowStatusOrderByUploadDateDesc(
        WorkflowStatus status
    );

    List<Photo> findByAlbumAndWorkflowStatusOrderBySortOrderInAlbumAsc(
        Album album,
        WorkflowStatus status
    );

    // Featured photos
    List<Photo> findByIsFeaturedTrueOrderBySortOrderInAlbumAsc();

    List<Photo> findByIsPortfolioImageTrueOrderBySortOrderInAlbumAsc();

    // Client-related queries
    List<Photo> findByClientApprovedTrueOrderBySortOrderInAlbumAsc();

    @Query(
        "SELECT p FROM Photo p WHERE p.album.isClientVisible = true AND p.workflowStatus IN :statuses ORDER BY p.sortOrderInAlbum ASC"
    )
    List<Photo> findClientVisiblePhotos(
        @Param("statuses") List<WorkflowStatus> statuses
    );

    // Album statistics
    @Query("SELECT COUNT(p) FROM Photo p WHERE p.album = :album")
    Long countByAlbum(@Param("album") Album album);

    @Query(
        "SELECT p.workflowStatus, COUNT(p) FROM Photo p WHERE p.album = :album GROUP BY p.workflowStatus"
    )
    List<Object[]> countByWorkflowStatusInAlbum(@Param("album") Album album);

    // Advanced album searches
    @Query(
        "SELECT p FROM Photo p WHERE p.album = :album " +
            "AND (:status IS NULL OR p.workflowStatus = :status) " +
            "AND (:isFeatured IS NULL OR p.isFeatured = :isFeatured) " +
            "AND (:clientApproved IS NULL OR p.clientApproved = :clientApproved) " +
            "ORDER BY p.sortOrderInAlbum ASC"
    )
    List<Photo> findPhotosInAlbumWithFilters(
        @Param("album") Album album,
        @Param("status") WorkflowStatus status,
        @Param("isFeatured") Boolean isFeatured,
        @Param("clientApproved") Boolean clientApproved
    );
}
