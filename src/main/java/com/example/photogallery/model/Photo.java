package com.example.photogallery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "photos")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

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

    public Photo() {}

    public Photo(
        Tenant tenant,
        String originalName,
        String fileName,
        String contentType,
        Long size,
        String fileHash
    ) {
        this.tenant = tenant;
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

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
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
