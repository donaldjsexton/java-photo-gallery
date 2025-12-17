package com.example.photogallery.controller;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LandingController {

    @GetMapping("/")
    public String landing(
        Authentication authentication,
        @RequestParam(value = "share", required = false) String share,
        Model model
    ) {
        if (isAuthenticated(authentication)) {
            return "redirect:/dashboard";
        }

        if (share != null && !share.isBlank()) {
            return "redirect:" + resolveShareRedirect(share).orElse("/?shareError=1");
        }

        return "landing";
    }

    @GetMapping("/share/resolve")
    public String resolveShare(
        @RequestParam(value = "link", required = false) String link
    ) {
        return "redirect:" + resolveShareRedirect(link).orElse("/?shareError=1");
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private Optional<String> resolveShareRedirect(String raw) {
        if (raw == null) {
            return Optional.empty();
        }

        String trimmed = raw.trim();
        if (trimmed.isBlank()) {
            return Optional.empty();
        }

        // 1) direct token UUID
        try {
            UUID tokenId = UUID.fromString(trimmed);
            return Optional.of("/share/" + tokenId);
        } catch (IllegalArgumentException ignored) {}

        // 2) full URL or path containing /share/{tokenId}/...
        String path = null;
        try {
            URI uri = URI.create(trimmed);
            path = uri.getPath();
        } catch (IllegalArgumentException ignored) {}

        if (path == null || path.isBlank()) {
            int idx = trimmed.indexOf("/share/");
            if (idx >= 0) {
                path = trimmed.substring(idx);
            }
        }

        if (path == null) {
            return Optional.empty();
        }

        String cleaned = path.trim();
        if (!cleaned.startsWith("/share/") || cleaned.startsWith("//")) {
            return Optional.empty();
        }
        if (cleaned.contains("..")) {
            return Optional.empty();
        }

        String[] parts = cleaned.split("/");
        if (parts.length < 3) {
            return Optional.empty();
        }

        UUID tokenId;
        try {
            tokenId = UUID.fromString(parts[2]);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        String suffix = cleaned.substring(("/share/" + parts[2]).length());
        return Optional.of("/share/" + tokenId + suffix);
    }
}
