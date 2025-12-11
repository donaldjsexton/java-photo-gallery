package com.example.photogallery.service;

import com.example.photogallery.model.Category;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.CategoryRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TenantService tenantService;

    public CategoryService(
        CategoryRepository categoryRepository,
        TenantService tenantService
    ) {
        this.categoryRepository = categoryRepository;
        this.tenantService = tenantService;
    }

    public List<Category> listForCurrentTenant() {
        return categoryRepository.findByTenant(currentTenant());
    }

    public Category getById(Long id) {
        return categoryRepository
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Category not found"));
    }

    public Category getOrCreateDefaultCategory() {
        Tenant tenant = currentTenant();
        return categoryRepository
            .findByTenantAndName(tenant, "General")
            .orElseGet(() ->
                categoryRepository.save(
                    new Category(tenant, "General", "Default category")
                )
            );
    }

    public Category create(String name, String description) {
        Category c = new Category(currentTenant(), name, description);
        return categoryRepository.save(c);
    }

    private Tenant currentTenant() {
        return tenantService.getDefaultTenant();
    }
}
