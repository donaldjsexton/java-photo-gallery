package com.example.photogallery.service;

import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.GalleryPhoto;
import com.example.photogallery.model.Photo;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.GalleryPhotoRepository;
import com.example.photogallery.repository.GalleryRepository;
import com.example.photogallery.repository.PhotoRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class GalleryPhotoService {

    @Autowired
    private GalleryRepository galleryRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private GalleryPhotoRepository galleryPhotoRepository;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private PhotoService photoService;

    public Photo getThumbnailPhotoForGallery(Long galleryId) {
        Tenant tenant = tenantService.getCurrentTenant();
        Gallery gallery = galleryRepository
            .findByIdAndTenant(galleryId, tenant)
            .orElseThrow(() -> new NoSuchElementException("Gallery not found"));

        if (gallery.getCoverPhoto() != null) {
            return gallery.getCoverPhoto();
        }

        return galleryPhotoRepository
            .findFirstByGalleryIdAndTenantOrderBySortOrderAscAddedAtAsc(
                galleryId,
                tenant
            )
            .map(GalleryPhoto::getPhoto)
            .orElse(null);
    }

    // ---- Add photo to gallery ----

    @Transactional
    public GalleryPhoto addPhotoToGallery(
        Long galleryId,
        Long photoId,
        Integer sortOrder
    ) {
        Tenant tenant = tenantService.getCurrentTenant();
        Gallery gallery = galleryRepository
            .findByIdAndTenant(galleryId, tenant)
            .orElseThrow(() -> new NoSuchElementException("Gallery not found"));

        Photo photo = photoRepository
            .findByIdAndTenant(photoId, tenant)
            .orElseThrow(() -> new NoSuchElementException("Photo not found"));

        // If already there, just return existing mapping.
        // If two requests race, rely on the DB unique constraint and re-fetch on conflict.
        try {
            return galleryPhotoRepository
                .findByGalleryAndPhoto(gallery, photo)
                .orElseGet(() -> {
                    GalleryPhoto gp = new GalleryPhoto();
                    gp.setTenant(tenant);
                    gp.setGallery(gallery);
                    gp.setPhoto(photo);
                    gp.setSortOrder(sortOrder);
                    return galleryPhotoRepository.save(gp);
                });
        } catch (DataIntegrityViolationException e) {
            return galleryPhotoRepository
                .findByGalleryAndPhoto(gallery, photo)
                .orElseThrow(() -> e);
        }
    }

    // Overload: no explicit sort order
    @Transactional
    public GalleryPhoto addPhotoToGallery(Long galleryId, Long photoId) {
        return addPhotoToGallery(galleryId, photoId, null);
    }

    // ---- Remove photo from gallery ----

    @Transactional
    public void removePhotoFromGallery(Long galleryId, Long photoId) {
        Tenant tenant = tenantService.getCurrentTenant();
        Gallery gallery = galleryRepository
            .findByIdAndTenant(galleryId, tenant)
            .orElseThrow(() -> new NoSuchElementException("Gallery not found"));

        Photo photo = photoRepository
            .findByIdAndTenant(photoId, tenant)
            .orElseThrow(() -> new NoSuchElementException("Photo not found"));

        galleryPhotoRepository.deleteByGalleryAndPhoto(gallery, photo);

        // If that was the last reference to the photo, remove it from DB + disk so it can be reuploaded.
        photoService.purgeOrphanedPhotosForCurrentTenant();
    }

    // ---- List photos in a gallery ----

    @Transactional
    public List<Photo> getPhotosInGallery(Long galleryId) {
        return galleryPhotoRepository
            .findByGalleryIdAndTenantOrderBySortOrderAscAddedAtAsc(
                galleryId,
                tenantService.getCurrentTenant()
            )
            .stream()
            .map(GalleryPhoto::getPhoto)
            .collect(Collectors.toList());
    }

    // ---- Reorder photos ----
    // Accepts a list of photo IDs in the desired order

    @Transactional
    public void reorderPhotos(Long galleryId, List<Long> orderedPhotoIds) {
        Tenant tenant = tenantService.getCurrentTenant();
        Gallery gallery = galleryRepository
            .findByIdAndTenant(galleryId, tenant)
            .orElseThrow(() -> new NoSuchElementException("Gallery not found"));

        List<GalleryPhoto> mappings =
            galleryPhotoRepository.findByGalleryOrderBySortOrderAscAddedAtAsc(
                gallery
            );

        // Simple O(n^2) is fine for small galleries
        for (GalleryPhoto gp : mappings) {
            Long pid = gp.getPhoto().getId();
            int index = orderedPhotoIds.indexOf(pid);
            if (index != -1) {
                gp.setSortOrder(index);
            }
        }

        galleryPhotoRepository.saveAll(mappings);
    }
}
