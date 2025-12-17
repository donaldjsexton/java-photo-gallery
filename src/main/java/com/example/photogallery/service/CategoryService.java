package com.example.photogallery.service;

import com.example.photogallery.model.Category;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.AlbumRepository;
import com.example.photogallery.repository.CategoryRepository;
import com.example.photogallery.repository.GalleryPhotoRepository;
import com.example.photogallery.repository.GalleryRepository;
import com.example.photogallery.repository.ShareTokenRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AlbumRepository albumRepository;
    private final GalleryRepository galleryRepository;
    private final GalleryPhotoRepository galleryPhotoRepository;
    private final ShareTokenRepository shareTokenRepository;
    private final TenantService tenantService;
    private final PhotoService photoService;

    public CategoryService(
        CategoryRepository categoryRepository,
        AlbumRepository albumRepository,
        GalleryRepository galleryRepository,
        GalleryPhotoRepository galleryPhotoRepository,
        ShareTokenRepository shareTokenRepository,
        TenantService tenantService,
        PhotoService photoService
    ) {
        this.categoryRepository = categoryRepository;
        this.albumRepository = albumRepository;
        this.galleryRepository = galleryRepository;
        this.galleryPhotoRepository = galleryPhotoRepository;
        this.shareTokenRepository = shareTokenRepository;
        this.tenantService = tenantService;
        this.photoService = photoService;
    }

    public List<Category> listForCurrentTenant() {
        return categoryRepository.findByTenant(currentTenant());
    }

    public Category getById(Long id) {
        Tenant tenant = currentTenant();
        return categoryRepository
            .findByIdAndTenant(id, tenant)
            .orElseThrow(() -> new NoSuchElementException("Category not found"));
    }

    public Category getOrCreateDefaultCategory() {
        Tenant tenant = currentTenant();
        return categoryRepository
            .findByTenantAndName(tenant, "General")
            .orElseGet(() -> {
                try {
                    return categoryRepository.save(
                        new Category(tenant, "General", "Default category")
                    );
                } catch (DataIntegrityViolationException e) {
                    return categoryRepository
                        .findByTenantAndName(tenant, "General")
                        .orElseThrow(() -> e);
                }
            });
    }

    public Category create(String name, String description) {
        Category c = new Category(currentTenant(), name, description);
        return categoryRepository.save(c);
    }

    @jakarta.transaction.Transactional
    public void deleteCategory(Long id) {
        Tenant tenant = currentTenant();
        Category category = categoryRepository
            .findByIdAndTenant(id, tenant)
            .orElseThrow(() -> new NoSuchElementException("Category not found"));

        if ("General".equalsIgnoreCase(category.getName())) {
            throw new IllegalArgumentException(
                "Default category cannot be deleted."
            );
        }

        var albums = albumRepository.findByTenantAndCategory(tenant, category);
        if (!albums.isEmpty()) {
            for (var album : albums) {
                // Reuse the same hard-delete behavior as album deletion, but inline to avoid service cycles.
                if ("Default Album".equalsIgnoreCase(album.getName())) {
                    continue;
                }

                var galleries = galleryRepository.findByTenantAndAlbum(
                    tenant,
                    album
                );
                if (!galleries.isEmpty()) {
                    var galleryIds = galleries
                        .stream()
                        .map(com.example.photogallery.model.Gallery::getId)
                        .toList();

                    // Detach any children (even if they live in a different album) from galleries being deleted.
                    var allChildren = galleryRepository.findByTenantAndParentIdIn(
                        tenant,
                        galleryIds
                    );
                    if (!allChildren.isEmpty()) {
                        for (var child : allChildren) {
                            child.setParent(null);
                        }
                        galleryRepository.saveAll(allChildren);
                    }

                    for (var g : galleries) {
                        g.setParent(null);
                    }
                    galleryRepository.saveAll(galleries);

                    for (var g : galleries) {
                        galleryPhotoRepository.deleteByGalleryIdAndTenant(
                            g.getId(),
                            tenant
                        );
                    }
                    galleryRepository.deleteAll(galleries);
                }

                shareTokenRepository.deleteByAlbum(album);
                albumRepository.delete(album);
            }
        }

        categoryRepository.delete(category);

        // Category deletion may orphan photos; purge them from DB + disk so they can be reuploaded.
        photoService.purgeOrphanedPhotosForCurrentTenant();
    }

    private Tenant currentTenant() {
        return tenantService.getCurrentTenant();
    }
}
