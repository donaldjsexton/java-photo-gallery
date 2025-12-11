package com.example.photogallery.service;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.Tenant;
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

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AlbumService albumService;

    // ---- Create ----

    public Gallery createRootGallery(String title, String description) {
        return createRootGalleryInAlbum(
            albumService.getDefaultAlbumForTenant(),
            title,
            description
        );
    }

    public Gallery createRootGalleryInAlbum(
        Album album,
        String title,
        String description
    ) {
        Tenant tenant = resolveTenant();
        Gallery g = new Gallery();
        g.setTenant(tenant);
        g.setAlbum(album);
        g.setTitle(title);
        g.setDescription(description);
        g.setVisibility("private"); // default
        return galleryRepository.save(g);
    }

    public Gallery createRootGalleryInAlbum(
        Long albumId,
        String title,
        String description
    ) {
        Album album = albumService.getById(albumId);
        return createRootGalleryInAlbum(album, title, description);
    }

    private Gallery createChildGalleryWithAlbum(
        Long parentId,
        String title,
        String description,
        Album albumOverride
    ) {
        Tenant tenant = resolveTenant();
        Gallery parent = galleryRepository
            .findByIdAndTenant(parentId, tenant)
            .orElseThrow(() ->
                new NoSuchElementException("Parent gallery not found")
            );

        Album albumToUse =
            albumOverride != null ? albumOverride : parent.getAlbum();

        Gallery child = new Gallery();
        child.setTenant(tenant);
        child.setAlbum(albumToUse);
        child.setParent(parent);
        child.setTitle(title);
        child.setDescription(description);
        child.setVisibility("private");

        return galleryRepository.save(child);
    }

    public Gallery createChildGallery(
        Long parentId,
        String title,
        String description,
        Long albumIdOverride
    ) {
        Album albumOverride =
            albumIdOverride != null ? albumService.getById(albumIdOverride) : null;
        return createChildGalleryWithAlbum(
            parentId,
            title,
            description,
            albumOverride
        );
    }

    public Gallery createChildGallery(
        Long parentId,
        String title,
        String description
    ) {
        return createChildGalleryWithAlbum(
            parentId,
            title,
            description,
            null
        );
    }

    // ---- Read ----

    public Gallery getGallery(Long id) {
        Tenant tenant = resolveTenant();
        return galleryRepository
            .findByIdAndTenant(id, tenant)
            .orElseThrow(() -> new NoSuchElementException("Gallery not found"));
    }

    public List<Gallery> getRootGalleries() {
        return galleryRepository.findByTenantAndParentIsNull(resolveTenant());
    }

    public List<Gallery> getChildren(Long id) {
        return galleryRepository.findByTenantAndParentId(resolveTenant(), id);
    }

    // ---- Update ----

    @Transactional
    public Gallery updateGallery(
        Long id,
        String newTitle,
        String newDescription,
        String visibility
    ) {
        Tenant tenant = resolveTenant();
        Gallery g = galleryRepository
            .findByIdAndTenant(id, tenant)
            .orElseThrow(() -> new NoSuchElementException("Gallery not found"));

        if (newTitle != null) g.setTitle(newTitle);
        if (newDescription != null) g.setDescription(newDescription);
        if (visibility != null) g.setVisibility(visibility);

        return g; // JPA auto-flushes
    }

    // ---- Delete ----

    public void deleteGallery(Long id) {
        Tenant tenant = resolveTenant();
        if (!galleryRepository.findByIdAndTenant(id, tenant).isPresent()) {
            throw new NoSuchElementException("Gallery not found");
        }
        galleryRepository.deleteById(id);
    }

    private Tenant resolveTenant() {
        return tenantService.getDefaultTenant();
    }
}
