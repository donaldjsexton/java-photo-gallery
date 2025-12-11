package com.example.photogallery.service;

import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.TenantRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    @Value("${photo.gallery.default-tenant-slug:default}")
    private String defaultTenantSlug;

    @Value("${photo.gallery.default-tenant-name:Default Tenant}")
    private String defaultTenantName;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @PostConstruct
    public void ensureDefaultTenant() {
        tenantRepository
            .findBySlug(defaultTenantSlug)
            .orElseGet(() ->
                tenantRepository.save(
                    new Tenant(defaultTenantSlug, defaultTenantName)
                )
            );
    }

    public Tenant getDefaultTenant() {
        return tenantRepository
            .findBySlug(defaultTenantSlug)
            .orElseGet(() ->
                tenantRepository.save(
                    new Tenant(defaultTenantSlug, defaultTenantName)
                )
            );
    }

    public Tenant save(Tenant tenant) {
        return tenantRepository.save(tenant);
    }
}
