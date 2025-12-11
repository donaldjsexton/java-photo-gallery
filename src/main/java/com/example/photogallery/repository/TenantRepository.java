package com.example.photogallery.repository;

import com.example.photogallery.model.Tenant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findBySlug(String slug);
}
