package com.example.photogallery.repository;

import com.example.photogallery.model.Photo;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    // --- Dedupe ---
    Optional<Photo> findByFileHash(String fileHash);

    // --- Sorters (PRD §10) ---
    List<Photo> findAllByOrderByUploadDateDesc();
    List<Photo> findAllByOrderByDateTakenDesc();
    List<Photo> findAllByOrderByDateTakenAsc();
    List<Photo> findAllByOrderByCameraAsc();

    // --- Filters (PRD §10) ---
    @Query(
        "SELECT p FROM Photo p WHERE p.camera IS NOT NULL AND p.camera <> '' ORDER BY p.uploadDate DESC"
    )
    List<Photo> findPhotosWithCamera();

    @Query(
        "SELECT p FROM Photo p WHERE p.dateTaken IS NOT NULL ORDER BY p.uploadDate DESC"
    )
    List<Photo> findPhotosWithDateTaken();

    // --- Text search (case-insensitive across specified fields) ---
    @Query(
        """
        SELECT p FROM Photo p
        WHERE LOWER(p.originalName)   LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(p.camera)         LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(p.allExifData)    LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(p.searchableText) LIKE LOWER(CONCAT('%', :query, '%'))
        """
    )
    Page<Photo> findByTextSearch(
        @Param("query") String query,
        Pageable pageable
    );

    // Camera contains (case-insensitive)
    @Query(
        """
        SELECT p FROM Photo p
        WHERE LOWER(p.camera) LIKE LOWER(CONCAT('%', :camera, '%'))
        """
    )
    Page<Photo> findByCameraIgnoreCaseContaining(
        @Param("camera") String camera,
        Pageable pageable
    );

    // Date range against DATE column: dateTakenParsed
    @Query(
        """
        SELECT p FROM Photo p
        WHERE p.dateTakenParsed BETWEEN :start AND :end
        """
    )
    Page<Photo> findByDateRange(
        @Param("start") LocalDate start,
        @Param("end") LocalDate end,
        Pageable pageable
    );

    // --- Advanced search (all optional; service normalizes inputs) ---
    @Query(
        """
        SELECT p FROM Photo p
        WHERE
          ( :query IS NULL OR
            LOWER(p.originalName)   LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(p.camera)         LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(p.allExifData)    LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(p.searchableText) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        AND ( :camera IS NULL OR LOWER(p.camera) LIKE LOWER(CONCAT('%', :camera, '%')) )
        AND ( :startDate IS NULL OR :endDate IS NULL OR p.dateTakenParsed BETWEEN :startDate AND :endDate )
        """
    )
    Page<Photo> advancedSearch(
        @Param("query") String query,
        @Param("camera") String camera,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );
}
