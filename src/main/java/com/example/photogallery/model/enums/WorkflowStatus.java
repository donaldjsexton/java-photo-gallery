package com.example.photogallery.model.enums;

public enum WorkflowStatus {
    RAW("Raw Upload"),
    IN_PROGRESS("In Progress"),
    EDITED("Edited"),
    CLIENT_REVIEW("Client Review"),
    CLIENT_APPROVED("Client Approved"),
    PORTFOLIO_READY("Portfolio Ready"),
    PUBLISHED("Published"),
    ARCHIVED("Archived");

    private final String displayName;

    WorkflowStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isClientVisible() {
        return this == CLIENT_REVIEW || this == CLIENT_APPROVED ||
               this == PORTFOLIO_READY || this == PUBLISHED;
    }

    public boolean isPortfolioReady() {
        return this == PORTFOLIO_READY || this == PUBLISHED;
    }
}
