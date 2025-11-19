package com.example.photogallery.service;

import com.example.photogallery.model.Gallery;
import com.example.photogallery.repository.GalleryRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GalleryService {

    @Autowired
    private GalleryRepository galleryRepository;

    // ---- Create ----

    public Gallery createRootGallery(String title, String description) {
        Gallery g = new Gallery();
        g.setTitle(title);
        g.setDescription(description);
        g.setVisibility("private"); // default
        return galleryRepository.save(g);
    }

    public Gallery createChildGallery(
        Long parentId,
        String title,
        String description
    ) {
        Gallery parent = galleryRepository
            .findById(parentId)
            .orElseThrow(() ->
                new NoSuchElementException("Parent gallery not found")
            );

        Gallery child = new Gallery();
        child.setParent(parent);
        child.setTitle(title);
        child.setDescription(description);
        child.setVisibility("private");

        return galleryRepository.save(child);
    }

    // ---- Read ----

    public Gallery getGallery(Long id) {
        return galleryRepository
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Gallery not found"));
    }

    public List<Gallery> getRootGalleries() {
        return galleryRepository.findByParentIsNull();
    }

    public List<Gallery> getChildren(Long id) {
        return galleryRepository.findByParentId(id);
    }

    // ---- Update ----

    @Transactional
    public Gallery updateGallery(
        Long id,
        String newTitle,
        String newDescription,
        String visibility
    ) {
        Gallery g = galleryRepository
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Gallery not found"));

        if (newTitle != null) g.setTitle(newTitle);
        if (newDescription != null) g.setDescription(newDescription);
        if (visibility != null) g.setVisibility(visibility);

        return g; // JPA auto-flushes
    }

    // ---- Delete ----

    public void deleteGallery(Long id) {
        if (!galleryRepository.existsById(id)) {
            throw new NoSuchElementException("Gallery not found");
        }
        galleryRepository.deleteById(id);
    }
}
