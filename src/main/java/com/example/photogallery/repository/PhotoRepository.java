package com.example.photogallery.repository;

import com.example.photogallery.model.Photo;
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
}
