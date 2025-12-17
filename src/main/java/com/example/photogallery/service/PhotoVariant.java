package com.example.photogallery.service;

import org.springframework.util.StringUtils;

public enum PhotoVariant {
    ORIGINAL,
    WEB;

    public static PhotoVariant fromString(String raw) {
        if (!StringUtils.hasText(raw)) {
            return ORIGINAL;
        }
        return switch (raw.trim().toLowerCase()) {
            case "web", "web-size", "websize", "small" -> WEB;
            case "original", "orig", "full" -> ORIGINAL;
            default -> ORIGINAL;
        };
    }
}

