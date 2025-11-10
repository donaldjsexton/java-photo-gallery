package com.example.photogallery.model.enums;

public enum AlbumType {
    CLIENT("Client Session"),
    COLLECTION("Curated Collection"),
    EVENT_TYPE("Event Category"),
    DATE_ARCHIVE("Date Archive"),
    PORTFOLIO("Portfolio"),
    ARCHIVE("Archive"),
    ROOT("Root Album");

    private final String displayName;

    AlbumType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
