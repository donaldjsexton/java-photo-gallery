package com.example.photogallery.repository;

import com.example.photogallery.model.Photo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
