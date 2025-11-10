package com.example.photogallery.service;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Photo;
import com.example.photogallery.model.enums.AlbumType;
import com.example.photogallery.repository.AlbumRepository;
import com.example.photogallery.repository.PhotoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AlbumService {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private PhotoRepository photoRepository;

    // Basic CRUD operations
    public Album createAlbum(String name, AlbumType albumType, Album parent) {
        Album album = new Album(name, albumType, parent);

        // Generate slug from name
        album.setSlug(generateSlug(name));

        // Set sort order based on existing albums in parent
        if (parent != null) {
            int maxOrder = parent
                .getChildren()
                .stream()
                .mapToInt(Album::getSortOrder)
                .max()
                .orElse(0);
            album.setSortOrder(maxOrder + 1);
        } else {
            // Root album - find max order among root albums
            List<Album> rootAlbums =
                albumRepository.findByParentIsNullOrderBySortOrder();
            int maxOrder = rootAlbums
                .stream()
                .mapToInt(Album::getSortOrder)
                .max()
                .orElse(0);
            album.setSortOrder(maxOrder + 1);
        }

        return albumRepository.save(album);
    }

    public Album createClientSession(
        String clientName,
        LocalDateTime shootDate,
        String sessionName
    ) {
        Album clientAlbum = new Album(sessionName, AlbumType.CLIENT);
        clientAlbum.setClientName(clientName);
        clientAlbum.setShootDate(shootDate);
        clientAlbum.setIsClientVisible(true);
        clientAlbum.setSlug(generateSlug(clientName + "-" + sessionName));

        return albumRepository.save(clientAlbum);
    }

    public Album createCollection(
        String name,
        String description,
        boolean isFeatured
    ) {
        Album collection = new Album(name, AlbumType.COLLECTION);
        collection.setDescription(description);
        collection.setIsFeatured(isFeatured);
        collection.setIsPublic(true);
        collection.setSlug(generateSlug(name));

        return albumRepository.save(collection);
    }

    public Album save(Album album) {
        return albumRepository.save(album);
    }

    public Optional<Album> findById(Long id) {
        return albumRepository.findById(id);
    }

    public Optional<Album> findBySlug(String slug) {
        return albumRepository.findBySlug(slug);
    }

    public List<Album> findAll() {
        return albumRepository.findAll();
    }

    public void deleteAlbum(Long albumId) {
        Optional<Album> albumOpt = albumRepository.findById(albumId);
        if (albumOpt.isPresent()) {
            Album album = albumOpt.get();

            // Move photos to parent album or unassigned
            if (!album.getPhotos().isEmpty()) {
                Album targetAlbum = album.getParent();
                for (Photo photo : album.getPhotos()) {
                    photo.setAlbum(targetAlbum);
                    photoRepository.save(photo);
                }
            }

            // Reassign child albums to parent
            for (Album child : album.getChildren()) {
                child.setParent(album.getParent());
                albumRepository.save(child);
            }

            albumRepository.delete(album);
        }
    }

    // Hierarchy navigation
    public List<Album> getRootAlbums() {
        return albumRepository.findByParentIsNullOrderBySortOrder();
    }

    public List<Album> getChildAlbums(Album parent) {
        return albumRepository.findByParentOrderBySortOrder(parent);
    }

    public List<Album> getAlbumsByType(AlbumType albumType) {
        return albumRepository.findByAlbumTypeOrderBySortOrder(albumType);
    }

    public List<Album> getRootAlbumsByType(AlbumType albumType) {
        return albumRepository.findByParentIsNullAndAlbumTypeOrderBySortOrder(
            albumType
        );
    }

    // Professional workflow methods
    public List<Album> getClientAlbums() {
        return getAlbumsByType(AlbumType.CLIENT);
    }

    public List<Album> getCollections() {
        return getAlbumsByType(AlbumType.COLLECTION);
    }

    public List<Album> getFeaturedAlbums() {
        return albumRepository.findByIsFeaturedTrueOrderBySortOrder();
    }

    public List<Album> getPublicAlbums() {
        return albumRepository.findByIsPublicTrueOrderBySortOrder();
    }

    public List<Album> getPortfolioAlbums() {
        return albumRepository.findPortfolioAlbums();
    }

    public List<Album> getClientVisibleAlbums(String clientName) {
        if (clientName != null && !clientName.trim().isEmpty()) {
            return albumRepository.findByClientNameContainingIgnoreCaseOrderByShootDateDesc(
                clientName
            );
        }
        return albumRepository.findByIsClientVisibleTrueOrderBySortOrder();
    }

    // Photo management within albums
    public void addPhotoToAlbum(Photo photo, Album album) {
        photo.setAlbum(album);
        photo.setSortOrderInAlbum(album.getPhotoCount());
        photoRepository.save(photo);
    }

    public void movePhotoToAlbum(Photo photo, Album targetAlbum) {
        photo.setAlbum(targetAlbum);
        if (targetAlbum != null) {
            photo.setSortOrderInAlbum(targetAlbum.getPhotoCount());
        } else {
            photo.setSortOrderInAlbum(0);
        }
        photoRepository.save(photo);
    }

    public void movePhotosToAlbum(List<Photo> photos, Album targetAlbum) {
        int startOrder = targetAlbum != null ? targetAlbum.getPhotoCount() : 0;

        for (int i = 0; i < photos.size(); i++) {
            Photo photo = photos.get(i);
            photo.setAlbum(targetAlbum);
            photo.setSortOrderInAlbum(startOrder + i);
            photoRepository.save(photo);
        }
    }

    public void updatePhotoOrder(Album album, List<Long> photoIds) {
        for (int i = 0; i < photoIds.size(); i++) {
            Long photoId = photoIds.get(i);
            Optional<Photo> photoOpt = photoRepository.findById(photoId);
            if (photoOpt.isPresent()) {
                Photo photo = photoOpt.get();
                if (photo.getAlbum().getId().equals(album.getId())) {
                    photo.setSortOrderInAlbum(i);
                    photoRepository.save(photo);
                }
            }
        }
    }

    // Album organization
    public void updateAlbumOrder(List<Long> albumIds) {
        for (int i = 0; i < albumIds.size(); i++) {
            Long albumId = albumIds.get(i);
            Optional<Album> albumOpt = albumRepository.findById(albumId);
            if (albumOpt.isPresent()) {
                Album album = albumOpt.get();
                album.setSortOrder(i);
                albumRepository.save(album);
            }
        }
    }

    public void moveAlbum(Album album, Album newParent) {
        album.setParent(newParent);

        // Update sort order in new parent
        if (newParent != null) {
            int maxOrder = newParent
                .getChildren()
                .stream()
                .filter(child -> !child.getId().equals(album.getId()))
                .mapToInt(Album::getSortOrder)
                .max()
                .orElse(0);
            album.setSortOrder(maxOrder + 1);
        } else {
            List<Album> rootAlbums =
                albumRepository.findByParentIsNullOrderBySortOrder();
            int maxOrder = rootAlbums
                .stream()
                .filter(root -> !root.getId().equals(album.getId()))
                .mapToInt(Album::getSortOrder)
                .max()
                .orElse(0);
            album.setSortOrder(maxOrder + 1);
        }

        albumRepository.save(album);
    }

    // Search functionality
    public List<Album> searchAlbums(String name) {
        return albumRepository.findByNameContainingIgnoreCaseOrderBySortOrder(
            name
        );
    }

    public List<Album> searchByClient(String clientName) {
        return albumRepository.findByClientNameContainingIgnoreCaseOrderByShootDateDesc(
            clientName
        );
    }

    public List<Album> advancedSearch(
        String name,
        String clientName,
        AlbumType albumType,
        Boolean isPublic,
        Boolean isFeatured
    ) {
        return albumRepository.searchAlbums(
            name,
            clientName,
            albumType,
            isPublic,
            isFeatured
        );
    }

    // Statistics and reporting
    public long getTotalAlbumCount() {
        return albumRepository.count();
    }

    public List<Object[]> getAlbumStatsByType() {
        return albumRepository.getAlbumCountByType();
    }

    public List<Album> getRecentAlbums(int limit) {
        List<Album> recent = albumRepository.findRecentAlbums();
        return recent.size() > limit ? recent.subList(0, limit) : recent;
    }

    public List<Album> getAlbumsWithPhotos() {
        return albumRepository.findAlbumsWithPhotos();
    }

    public List<Album> getEmptyAlbums() {
        return albumRepository.findEmptyAlbums();
    }

    // Auto-organization features
    public void organizePhotosByDate() {
        // Find photos without albums or in temporary albums
        List<Photo> unorganizedPhotos =
            photoRepository.findByAlbumIsNullOrderByUploadDateDesc();

        for (Photo photo : unorganizedPhotos) {
            if (photo.getDateTaken() != null) {
                LocalDateTime dateTaken = photo.getDateTaken();

                // Find or create year album
                String yearName = String.valueOf(dateTaken.getYear());
                Album yearAlbum = findOrCreateDateAlbum(
                    yearName,
                    AlbumType.DATE_ARCHIVE,
                    null
                );

                // Find or create month album
                String monthName = dateTaken.getMonth().toString();
                Album monthAlbum = findOrCreateDateAlbum(
                    monthName,
                    AlbumType.DATE_ARCHIVE,
                    yearAlbum
                );

                // Add photo to month album
                addPhotoToAlbum(photo, monthAlbum);
            }
        }
    }

    public Album createDefaultHierarchy() {
        // Create root structure for professional photography
        Album clientsRoot = findOrCreateAlbum(
            "Clients",
            AlbumType.CLIENT,
            null
        );
        Album collectionsRoot = findOrCreateAlbum(
            "Collections",
            AlbumType.COLLECTION,
            null
        );
        Album portfolioRoot = findOrCreateAlbum(
            "Portfolio",
            AlbumType.PORTFOLIO,
            null
        );
        findOrCreateAlbum("Archive", AlbumType.ARCHIVE, null);

        // Create event type categories
        Album eventsRoot = findOrCreateAlbum(
            "Events by Type",
            AlbumType.EVENT_TYPE,
            null
        );
        findOrCreateAlbum("Weddings", AlbumType.EVENT_TYPE, eventsRoot);
        findOrCreateAlbum("Portraits", AlbumType.EVENT_TYPE, eventsRoot);
        findOrCreateAlbum("Corporate", AlbumType.EVENT_TYPE, eventsRoot);
        findOrCreateAlbum("Events", AlbumType.EVENT_TYPE, eventsRoot);

        // Create featured collections
        findOrCreateAlbum(
            "Best of " + LocalDateTime.now().getYear(),
            AlbumType.COLLECTION,
            collectionsRoot
        );
        findOrCreateAlbum(
            "Featured Weddings",
            AlbumType.COLLECTION,
            collectionsRoot
        );
        findOrCreateAlbum(
            "Portfolio Highlights",
            AlbumType.COLLECTION,
            portfolioRoot
        );

        return clientsRoot;
    }

    // Helper methods
    private Album findOrCreateAlbum(
        String name,
        AlbumType albumType,
        Album parent
    ) {
        // Try to find existing album first
        List<Album> candidates = parent != null
            ? albumRepository.findByParentOrderBySortOrder(parent)
            : albumRepository.findByParentIsNullAndAlbumTypeOrderBySortOrder(
                  albumType
              );

        Optional<Album> existing = candidates
            .stream()
            .filter(a -> a.getName().equalsIgnoreCase(name))
            .findFirst();

        if (existing.isPresent()) {
            return existing.get();
        }

        return createAlbum(name, albumType, parent);
    }

    private Album findOrCreateDateAlbum(
        String name,
        AlbumType albumType,
        Album parent
    ) {
        return findOrCreateAlbum(name, albumType, parent);
    }

    private String generateSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "album-" + System.currentTimeMillis();
        }

        String slug = name
            .toLowerCase()
            .replaceAll("[^a-z0-9\\-\\s]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        // Ensure uniqueness
        String originalSlug = slug;
        int counter = 1;
        while (albumRepository.findBySlug(slug).isPresent()) {
            slug = originalSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    public void setCoverPhoto(Album album, Photo photo) {
        if (photo.getAlbum().getId().equals(album.getId())) {
            album.setCoverPhotoId(photo.getId());
            albumRepository.save(album);
        }
    }

    public Optional<Photo> getCoverPhoto(Album album) {
        if (album.getCoverPhotoId() != null) {
            return photoRepository.findById(album.getCoverPhotoId());
        }

        // Auto-select first photo as cover if none set
        if (!album.getPhotos().isEmpty()) {
            return album
                .getPhotos()
                .stream()
                .min((p1, p2) ->
                    Integer.compare(
                        p1.getSortOrderInAlbum(),
                        p2.getSortOrderInAlbum()
                    )
                );
        }

        return Optional.empty();
    }
}
