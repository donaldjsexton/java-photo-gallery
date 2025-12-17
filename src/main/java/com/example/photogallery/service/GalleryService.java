package com.example.photogallery.service;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.AlbumVisibility;
import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.GalleryPhotoRepository;
import com.example.photogallery.repository.GalleryRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.util.Locale;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GalleryService {

    private final TransactionTemplate transactionTemplate;

    public GalleryService(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Autowired
    private GalleryRepository galleryRepository;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private GalleryPhotoRepository galleryPhotoRepository;

    @PostConstruct
    void init() {
        transactionTemplate.executeWithoutResult(status -> backfillPublicIdsAndSlugs());
    }

    private void backfillPublicIdsAndSlugs() {
        for (Gallery g : galleryRepository.findAll()) {
            boolean changed = false;
            if (g.getPublicId() == null) {
                g.setPublicId(UUID.randomUUID());
                changed = true;
            }
            if (!StringUtils.hasText(g.getSlug())) {
                g.setSlug(generateUniqueSlug(g.getTenant(), g.getTitle()));
                changed = true;
            }
            if (g.getAlbum() != null) {
                String desired = galleryVisibilityForAlbum(g.getAlbum());
                if (
                    !StringUtils.hasText(g.getVisibility()) ||
                    !desired.equalsIgnoreCase(g.getVisibility())
                ) {
                    g.setVisibility(desired);
                    changed = true;
                }
            }
            if (changed) {
                galleryRepository.save(g);
            }
        }
    }

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
        if (album == null) {
            throw new IllegalArgumentException("Album is required.");
        }
        if (album.getId() == null) {
            throw new IllegalArgumentException("Album is required.");
        }
        Tenant tenant = resolveTenant();
        Album resolvedAlbum = albumService.getById(album.getId());
        Gallery g = new Gallery();
        g.setTenant(tenant);
        g.setAlbum(resolvedAlbum);
        g.setTitle(title);
        g.setPublicId(UUID.randomUUID());
        g.setDescription(description);
        g.setVisibility(galleryVisibilityForAlbum(resolvedAlbum));
        return saveWithUniqueSlugRetry(g, title);
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
        String description
    ) {
        Tenant tenant = resolveTenant();
        Gallery parent = galleryRepository
            .findByIdAndTenant(parentId, tenant)
            .orElseThrow(() ->
                new NoSuchElementException("Parent gallery not found")
            );

        Gallery child = new Gallery();
        child.setTenant(tenant);
        child.setAlbum(parent.getAlbum());
        child.setParent(parent);
        child.setTitle(title);
        child.setPublicId(UUID.randomUUID());
        child.setDescription(description);
        child.setVisibility(galleryVisibilityForAlbum(parent.getAlbum()));

        return saveWithUniqueSlugRetry(child, title);
    }

    public Gallery createChildGallery(
        Long parentId,
        String title,
        String description
    ) {
        return createChildGalleryWithAlbum(
            parentId,
            title,
            description
        );
    }

    // ---- Read ----

    public Gallery getGallery(Long id) {
        Tenant tenant = resolveTenant();
        return galleryRepository
            .findByIdAndTenant(id, tenant)
            .orElseThrow(() -> new NoSuchElementException("Gallery not found"));
    }

    public Gallery getGalleryBySlugOrPublicId(String identifier) {
        Tenant tenant = resolveTenant();
        if (!StringUtils.hasText(identifier)) {
            throw new NoSuchElementException("Gallery not found");
        }

        String trimmed = identifier.trim();
        try {
            UUID publicId = UUID.fromString(trimmed);
            return galleryRepository
                .findByTenantAndPublicId(tenant, publicId)
                .orElseThrow(() -> new NoSuchElementException("Gallery not found"));
        } catch (IllegalArgumentException ignored) {}

        return galleryRepository
            .findByTenantAndSlug(tenant, trimmed)
            .orElseThrow(() -> new NoSuchElementException("Gallery not found"));
    }

    public List<Gallery> getRootGalleries() {
        return galleryRepository.findByTenantAndParentIsNull(resolveTenant());
    }

    public List<Gallery> getRootGalleriesForAlbum(Long albumId) {
        Tenant tenant = resolveTenant();
        Album album = albumService.getById(albumId);
        return getRootGalleriesForAlbum(album);
    }

    public List<Gallery> getRootGalleriesForAlbum(Album album) {
        Tenant tenant = resolveTenant();
        return galleryRepository.findByTenantAndAlbumAndParentIsNullOrderByCreatedAtDesc(
            tenant,
            album
        );
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
        if (visibility != null) {
            g.setVisibility(galleryVisibilityForAlbum(g.getAlbum()));
        }

        return g; // JPA auto-flushes
    }

    // ---- Delete ----

    @Transactional
    public void deleteGallery(Long id) {
        Tenant tenant = resolveTenant();
        Gallery gallery = galleryRepository
            .findByIdAndTenant(id, tenant)
            .orElseThrow(() -> new NoSuchElementException("Gallery not found"));

        // Move children to the root (and avoid FK constraints if DB lacks ON DELETE SET NULL)
        List<Gallery> children = galleryRepository.findByTenantAndParentId(
            tenant,
            id
        );
        if (!children.isEmpty()) {
            for (Gallery child : children) {
                child.setParent(null);
            }
            galleryRepository.saveAll(children);
        }

        // Remove any dependent rows that could block deletion (DBs may not have cascading FKs)
        galleryPhotoRepository.deleteByGalleryIdAndTenant(id, tenant);

        galleryRepository.delete(gallery);

        // If the gallery deletion orphaned any photos, purge them from DB + disk so they can be reuploaded.
        photoService.purgeOrphanedPhotosForCurrentTenant();
    }

    private Tenant resolveTenant() {
        return tenantService.getCurrentTenant();
    }

    private Gallery saveWithUniqueSlugRetry(Gallery gallery, String titleForSlug) {
        Tenant tenant = gallery.getTenant();
        for (int attempt = 0; attempt < 5; attempt++) {
            gallery.setSlug(generateUniqueSlug(tenant, titleForSlug));
            try {
                return galleryRepository.save(gallery);
            } catch (DataIntegrityViolationException e) {
                if (attempt == 4) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Failed to create gallery with unique slug");
    }

    private String generateUniqueSlug(Tenant tenant, String title) {
        String base = slugify(title);
        if (!StringUtils.hasText(base)) {
            base = "gallery";
        }

        String candidate = base;
        int i = 2;
        while (galleryRepository.existsByTenantAndSlug(tenant, candidate)) {
            candidate = base + "-" + i;
            i++;
            if (i > 10_000) {
                candidate = base + "-" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }
        return candidate;
    }

    private static String slugify(String input) {
        if (!StringUtils.hasText(input)) return null;
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        String slug =
            normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (!StringUtils.hasText(slug)) return null;
        if (slug.length() > 80) return slug.substring(0, 80).replaceAll("-+$", "");
        return slug;
    }

    private static String galleryVisibilityForAlbum(Album album) {
        AlbumVisibility visibility = album != null ? album.getVisibility() : null;
        return visibility == AlbumVisibility.PUBLIC ? "public" : "private";
    }
}
