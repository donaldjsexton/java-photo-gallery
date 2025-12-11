package com.example.photogallery.service;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Category;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.AlbumRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final CategoryService categoryService;
    private final TenantService tenantService;

    public AlbumService(
        AlbumRepository albumRepository,
        CategoryService categoryService,
        TenantService tenantService
    ) {
        this.albumRepository = albumRepository;
        this.categoryService = categoryService;
        this.tenantService = tenantService;
    }

    public List<Album> listForCurrentTenant() {
        return albumRepository.findByTenant(currentTenant());
    }

    public Album getById(Long id) {
        return albumRepository
            .findByIdAndTenant(id, currentTenant())
            .orElseThrow(() -> new NoSuchElementException("Album not found"));
    }

    public Album getDefaultAlbumForTenant() {
        Tenant tenant = currentTenant();
        Category defaultCategory = categoryService.getOrCreateDefaultCategory();
        return albumRepository
            .findByTenantAndName(tenant, "Default Album")
            .orElseGet(() ->
                albumRepository.save(
                    new Album(
                        tenant,
                        defaultCategory,
                        "Default Album",
                        "Auto-created album"
                    )
                )
            );
    }

    public Album create(Category category, String name, String description) {
        Album album = new Album(currentTenant(), category, name, description);
        return albumRepository.save(album);
    }

    private Tenant currentTenant() {
        return tenantService.getDefaultTenant();
    }
}
