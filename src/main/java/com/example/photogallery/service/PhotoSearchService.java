package com.example.photogallery.service;

import com.example.photogallery.model.Photo;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.PhotoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PhotoSearchService {

    private final PhotoRepository photoRepository;
    private final TenantService tenantService;

    public PhotoSearchService(
        PhotoRepository photoRepository,
        TenantService tenantService
    ) {
        this.photoRepository = photoRepository;
        this.tenantService = tenantService;
    }

    public Page<Photo> advancedSearch(
        String query,
        String camera,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        Pageable pageable
    ) {
        // Normalize strings (trim; null if blank)
        String q = normalize(query);
        String c = normalize(camera);

        // Convert to LocalDate for DATE column (date_taken_parsed)
        LocalDate start = startDateTime != null
            ? startDateTime.toLocalDate()
            : null;
        LocalDate end = endDateTime != null ? endDateTime.toLocalDate() : null;

        // If nothing provided, fall back to simple page of all photos
        if (q == null && c == null && (start == null || end == null)) {
            return photoRepository.findAllByTenant(currentTenant(), pageable);
        }

        // Delegate to repo’s advancedSearch which applies LOWER(...) and BETWEEN on DATE
        return photoRepository.advancedSearch(
            currentTenant(),
            q,
            c,
            start,
            end,
            pageable
        );
    }

    public Page<Photo> searchByText(String query, Pageable pageable) {
        String q = normalize(query);
        if (q == null) return photoRepository.findAllByTenant(
            currentTenant(),
            pageable
        );
        return photoRepository.findByTextSearch(currentTenant(), q, pageable);
    }

    public Page<Photo> searchByCamera(String camera, Pageable pageable) {
        String c = normalize(camera);
        if (c == null) return photoRepository.findAllByTenant(
            currentTenant(),
            pageable
        );
        return photoRepository.findByCameraIgnoreCaseContaining(
            currentTenant(),
            c,
            pageable
        );
    }

    public Page<Photo> searchByDate(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        Pageable pageable
    ) {
        LocalDate start = startDateTime != null
            ? startDateTime.toLocalDate()
            : null;
        LocalDate end = endDateTime != null ? endDateTime.toLocalDate() : null;
        if (start == null || end == null) return photoRepository.findAllByTenant(
            currentTenant(),
            pageable
        );
        return photoRepository.findByDateRange(
            currentTenant(),
            start,
            end,
            pageable
        );
    }

    private static String normalize(String s) {
        return (StringUtils.hasText(s)) ? s.trim() : null;
    }

    private Tenant currentTenant() {
        return tenantService.getCurrentTenant();
    }
}
