package com.example.photogallery.model;

import com.example.photogallery.model.enums.WorkflowStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "photos")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalName;
    private String fileName;
    private String contentType;
    private Long size;
    private String fileHash;
    private LocalDateTime uploadDate;

    //Exif Fields
    private String camera;
    private LocalDateTime dateTaken;
    private String gpsLatitude;
    private String gpsLongitude;
    private String orientation;
    private String focalLength;
    private String aperture;
    private String shutterSpeed;
    private String iso;
    private String imageHeight;
    private String imageWidth;

    @Column(columnDefinition = "TEXT")
    private String allExifData;

    @Column(name = "date_taken_parsed", columnDefinition = "DATE")
    @Temporal(TemporalType.DATE)
    private Date dateTakenParsed;

    @Column(length = 500)
    private String searchableText;

    @Column(length = 100)
    private String locationText;

    @Column(length = 100)
    private String cameraInfo;

    // Album relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    // Professional workflow fields
    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_status", nullable = false)
    private WorkflowStatus workflowStatus = WorkflowStatus.RAW;

    @Column(name = "sort_order_in_album", nullable = false)
    private Integer sortOrderInAlbum = 0;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "client_approved", nullable = false)
    private Boolean clientApproved = false;

    @Column(name = "is_portfolio_image", nullable = false)
    private Boolean isPortfolioImage = false;

    @Column(length = 1000)
    private String clientNotes;

    @Column(length = 1000)
    private String internalNotes;

    public Photo() {}

    public Photo(
        String originalName,
        String fileName,
        String contentType,
        Long size,
        String fileHash,
        Album album
    ) {
        this(originalName, fileName, contentType, size, fileHash);
        this.album = album;
        if (album != null) {
            this.sortOrderInAlbum = album.getPhotoCount();
        }
    }

    public Photo(
        String originalName,
        String fileName,
        String contentType,
        Long size,
        String fileHash
    ) {
        this.originalName = originalName;
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.fileHash = fileHash;
        this.uploadDate = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getCamera() {
        return camera;
    }

    public void setCamera(String camera) {
        this.camera = camera;
    }

    public LocalDateTime getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(LocalDateTime dateTaken) {
        this.dateTaken = dateTaken;
    }

    public Date getDateTakenParsed() {
        return dateTakenParsed;
    }

    public void setDateTakenParsed(Date dateTakenParsed) {
        this.dateTakenParsed = dateTakenParsed;
    }

    public String getSearchableText() {
        return searchableText;
    }

    public void setSearchableText(String searchableText) {
        this.searchableText = searchableText;
    }

    public String getLocationText() {
        return locationText;
    }

    public void setLocationText(String locationText) {
        this.locationText = locationText;
    }

    public String getCameraInfo() {
        return cameraInfo;
    }

    public void setCameraInfo(String cameraInfo) {
        this.cameraInfo = cameraInfo;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public WorkflowStatus getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(WorkflowStatus workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public Integer getSortOrderInAlbum() {
        return sortOrderInAlbum;
    }

    public void setSortOrderInAlbum(Integer sortOrderInAlbum) {
        this.sortOrderInAlbum = sortOrderInAlbum;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Boolean getClientApproved() {
        return clientApproved;
    }

    public void setClientApproved(Boolean clientApproved) {
        this.clientApproved = clientApproved;
    }

    public Boolean getIsPortfolioImage() {
        return isPortfolioImage;
    }

    public void setIsPortfolioImage(Boolean isPortfolioImage) {
        this.isPortfolioImage = isPortfolioImage;
    }

    public String getClientNotes() {
        return clientNotes;
    }

    public void setClientNotes(String clientNotes) {
        this.clientNotes = clientNotes;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
    }

    public String getGpsLatitude() {
        return gpsLatitude;
    }

    public void setGpsLatitude(String gpsLatitude) {
        this.gpsLatitude = gpsLatitude;
    }

    public String getGpsLongitude() {
        return gpsLongitude;
    }

    public void setGpsLongitude(String gpsLongitude) {
        this.gpsLongitude = gpsLongitude;
    }

    public String getFocalLength() {
        return focalLength;
    }

    public void setFocalLength(String focalLength) {
        this.focalLength = focalLength;
    }

    public String getAperture() {
        return aperture;
    }

    public void setAperture(String aperture) {
        this.aperture = aperture;
    }

    public String getShutterSpeed() {
        return shutterSpeed;
    }

    public void setShutterSpeed(String shutterSpeed) {
        this.shutterSpeed = shutterSpeed;
    }

    public String getIso() {
        return iso;
    }

    public void setIso(String iso) {
        this.iso = iso;
    }

    public String getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(String imageWidth) {
        this.imageWidth = imageWidth;
    }

    public String getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(String imageHeight) {
        this.imageHeight = imageHeight;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public String getAllExifData() {
        return allExifData;
    }

    public void setAllExifData(String allExifData) {
        this.allExifData = allExifData;
    }
}
