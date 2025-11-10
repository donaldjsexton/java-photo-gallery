package com.example.photogallery.model;

import com.example.photogallery.model.enums.AlbumType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "albums")
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "album_type", nullable = false)
    private AlbumType albumType;

    // Hierarchical structure
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Album parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Album> children = new ArrayList<>();

    // Photos in this album
    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Photo> photos = new ArrayList<>();

    // Professional features
    @Column(name = "client_name", length = 200)
    private String clientName;

    @Column(name = "shoot_date")
    private LocalDateTime shootDate;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "is_client_visible", nullable = false)
    private Boolean isClientVisible = false;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "cover_photo_id")
    private Long coverPhotoId;

    @Column(length = 500)
    private String slug;

    // Constructors
    public Album() {
        this.createdDate = LocalDateTime.now();
    }

    public Album(String name, AlbumType albumType) {
        this();
        this.name = name;
        this.albumType = albumType;
    }

    public Album(String name, AlbumType albumType, Album parent) {
        this(name, albumType);
        this.parent = parent;
        if (parent != null) {
            parent.getChildren().add(this);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AlbumType getAlbumType() {
        return albumType;
    }

    public void setAlbumType(AlbumType albumType) {
        this.albumType = albumType;
    }

    public Album getParent() {
        return parent;
    }

    public void setParent(Album parent) {
        this.parent = parent;
    }

    public List<Album> getChildren() {
        return children;
    }

    public void setChildren(List<Album> children) {
        this.children = children;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public LocalDateTime getShootDate() {
        return shootDate;
    }

    public void setShootDate(LocalDateTime shootDate) {
        this.shootDate = shootDate;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Boolean getIsClientVisible() {
        return isClientVisible;
    }

    public void setIsClientVisible(Boolean isClientVisible) {
        this.isClientVisible = isClientVisible;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Long getCoverPhotoId() {
        return coverPhotoId;
    }

    public void setCoverPhotoId(Long coverPhotoId) {
        this.coverPhotoId = coverPhotoId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    // Helper methods
    public boolean isRootAlbum() {
        return parent == null;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public boolean hasPhotos() {
        return photos != null && !photos.isEmpty();
    }

    public int getPhotoCount() {
        return photos != null ? photos.size() : 0;
    }

    public int getTotalPhotoCount() {
        int count = getPhotoCount();
        if (children != null) {
            for (Album child : children) {
                count += child.getTotalPhotoCount();
            }
        }
        return count;
    }

    public String getFullPath() {
        if (parent == null) {
            return name;
        }
        return parent.getFullPath() + " / " + name;
    }

    public List<Album> getBreadcrumbs() {
        List<Album> breadcrumbs = new ArrayList<>();
        Album current = this;
        while (current != null) {
            breadcrumbs.add(0, current);
            current = current.getParent();
        }
        return breadcrumbs;
    }

    @Override
    public String toString() {
        return "Album{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", albumType=" + albumType +
                ", clientName='" + clientName + '\'' +
                ", photoCount=" + getPhotoCount() +
                '}';
    }
}
