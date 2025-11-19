package com.example.photogallery.repository;

import com.example.photogallery.model.Gallery;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {
    List<Gallery> findByParentIsNull();
    List<Gallery> findByParentId(Long parentId);
}
