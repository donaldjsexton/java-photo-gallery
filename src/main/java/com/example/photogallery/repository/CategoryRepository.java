package com.example.photogallery.repository;

import com.example.photogallery.model.Category;
import com.example.photogallery.model.Tenant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByTenant(Tenant tenant);
    Optional<Category> findByTenantAndName(Tenant tenant, String name);
}
