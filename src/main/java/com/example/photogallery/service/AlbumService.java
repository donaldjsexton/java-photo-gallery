package com.example.photogallery.service;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.AlbumVisibility;
import com.example.photogallery.model.Category;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.AlbumRepository;
import com.example.photogallery.repository.GalleryPhotoRepository;
import com.example.photogallery.repository.GalleryRepository;
import com.example.photogallery.repository.ShareTokenRepository;
import java.util.List;
import java.util.NoSuchElementException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final CategoryService categoryService;
    private final TenantService tenantService;
    private final GalleryRepository galleryRepository;
    private final GalleryPhotoRepository galleryPhotoRepository;
    private final ShareTokenRepository shareTokenRepository;
    private final PhotoService photoService;

    public AlbumService(
        AlbumRepository albumRepository,
        CategoryService categoryService,
        TenantService tenantService,
        GalleryRepository galleryRepository,
        GalleryPhotoRepository galleryPhotoRepository,
        ShareTokenRepository shareTokenRepository,
        PhotoService photoService
    ) {
        this.albumRepository = albumRepository;
        this.categoryService = categoryService;
        this.tenantService = tenantService;
        this.galleryRepository = galleryRepository;
        this.galleryPhotoRepository = galleryPhotoRepository;
        this.shareTokenRepository = shareTokenRepository;
        this.photoService = photoService;
    }

    public List<Album> listForCurrentTenant() {
        return albumRepository.findByTenant(currentTenant());
    }

    public List<Album> searchForDashboard(
        Long categoryId,
        String sortKey,
        String query
    ) {
        Tenant tenant = currentTenant();
        Category category = categoryId != null
            ? categoryService.getById(categoryId)
            : null;

        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;

        Sort sort = resolveDashboardSort(sortKey);
        return albumRepository.searchForTenant(tenant, category, normalizedQuery, sort);
    }

    public Album getById(Long id) {
        return albumRepository
            .findByIdAndTenant(id, currentTenant())
            .orElseThrow(() -> new NoSuchElementException("Album not found"));
    }

    @Transactional
    public Album updateAlbum(
        Long id,
        Long categoryId,
        String name,
        String description,
        String visibility
    ) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Album name is required.");
        }

        Album album = getById(id);
        AlbumVisibility previousVisibility = album.getVisibility();
        Category category = categoryId != null
            ? categoryService.getById(categoryId)
            : null;

        AlbumVisibility parsedVisibility = parseVisibility(visibility);
        album.setCategory(category);
        album.setName(name.trim());
        album.setDescription(description != null ? description.trim() : null);
        album.setVisibility(parsedVisibility);
        Album saved = albumRepository.save(album);

        if (previousVisibility != parsedVisibility) {
            galleryRepository.updateVisibilityForAlbum(
                currentTenant(),
                saved,
                galleryVisibilityForAlbum(parsedVisibility)
            );
        }

        return saved;
    }

    public Album updateAlbum(
        Long id,
        Long categoryId,
        String name,
        String description
    ) {
        return updateAlbum(id, categoryId, name, description, null);
    }

    public Album getDefaultAlbumForTenant() {
        Tenant tenant = currentTenant();
        Category defaultCategory = categoryService.getOrCreateDefaultCategory();
        return albumRepository
            .findFirstByTenantAndNameOrderByIdAsc(tenant, "Default Album")
            .orElseGet(() -> {
                try {
                    return albumRepository.save(
                        new Album(
                            tenant,
                            defaultCategory,
                            "Default Album",
                            "Auto-created album"
                        )
                    );
                } catch (DataIntegrityViolationException e) {
                    return albumRepository
                        .findFirstByTenantAndNameOrderByIdAsc(
                            tenant,
                            "Default Album"
                        )
                        .orElseThrow(() -> e);
                }
            });
    }

    public Album create(Category category, String name, String description) {
        String trimmedName = name != null ? name.trim() : null;
        if (!StringUtils.hasText(trimmedName)) {
            throw new IllegalArgumentException("Album name is required.");
        }

        Tenant tenant = currentTenant();
        String trimmedDescription = description != null ? description.trim() : null;
        Album album = new Album(tenant, category, trimmedName, trimmedDescription);
        album.setVisibility(AlbumVisibility.PRIVATE);
        return albumRepository.save(album);
    }

    @jakarta.transaction.Transactional
    public void deleteAlbum(Long id) {
        Tenant tenant = currentTenant();
        Album album = albumRepository
            .findByIdAndTenant(id, tenant)
            .orElseThrow(() -> new NoSuchElementException("Album not found"));

        if ("Default Album".equalsIgnoreCase(album.getName())) {
            throw new IllegalArgumentException("Default album cannot be deleted.");
        }

        var galleries = galleryRepository.findByTenantAndAlbum(tenant, album);

        // Break parent links first to avoid FK issues when deleting a hierarchy.
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
                galleryPhotoRepository.deleteByGalleryIdAndTenant(g.getId(), tenant);
            }
            galleryRepository.deleteAll(galleries);
        }

        shareTokenRepository.deleteByAlbum(album);
        albumRepository.delete(album);

        // Album deletion may orphan photos; purge them from DB + disk so they can be reuploaded.
        photoService.purgeOrphanedPhotosForCurrentTenant();
    }

    private Tenant currentTenant() {
        return tenantService.getCurrentTenant();
    }

    private static Sort resolveDashboardSort(String sortKey) {
        if (!StringUtils.hasText(sortKey)) {
            return Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            );
        }

        return switch (sortKey.trim()) {
            case "title" -> Sort.by(
                Sort.Order.asc("name").ignoreCase(),
                Sort.Order.desc("id")
            );
            case "createdAt" -> Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            );
            default -> Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            );
        };
    }

    private static AlbumVisibility parseVisibility(String visibility) {
        if (!StringUtils.hasText(visibility)) {
            return AlbumVisibility.PRIVATE;
        }
        String normalized = visibility.trim().toLowerCase();
        return switch (normalized) {
            case "public" -> AlbumVisibility.PUBLIC;
            case "private" -> AlbumVisibility.PRIVATE;
            default -> AlbumVisibility.PRIVATE;
        };
    }

    private static String galleryVisibilityForAlbum(AlbumVisibility visibility) {
        return visibility == AlbumVisibility.PUBLIC ? "public" : "private";
    }
}
