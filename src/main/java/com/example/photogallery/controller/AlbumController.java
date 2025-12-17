package com.example.photogallery.controller;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Gallery;
import com.example.photogallery.service.ShareTokenService;
import com.example.photogallery.service.AlbumService;
import com.example.photogallery.service.CategoryService;
import com.example.photogallery.service.GalleryPhotoService;
import com.example.photogallery.service.GalleryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AlbumController {

    private final AlbumService albumService;
    private final GalleryService galleryService;
    private final GalleryPhotoService galleryPhotoService;
    private final CategoryService categoryService;
    private final ShareTokenService shareTokenService;

    public AlbumController(
        AlbumService albumService,
        GalleryService galleryService,
        GalleryPhotoService galleryPhotoService,
        CategoryService categoryService,
        ShareTokenService shareTokenService
    ) {
        this.albumService = albumService;
        this.galleryService = galleryService;
        this.galleryPhotoService = galleryPhotoService;
        this.categoryService = categoryService;
        this.shareTokenService = shareTokenService;
    }

    @GetMapping("/albums/{id}")
    public String viewAlbum(@PathVariable("id") Long albumId, Model model) {
        Album album = albumService.getById(albumId);
        List<Gallery> galleries = galleryService.getRootGalleriesForAlbum(album);

        Map<Long, Long> galleryThumbnails = new HashMap<>();
        for (Gallery g : galleries) {
            try {
                var photo = galleryPhotoService.getThumbnailPhotoForGallery(
                    g.getId()
                );
                if (photo != null) {
                    galleryThumbnails.put(g.getId(), photo.getId());
                }
            } catch (RuntimeException ignored) {}
        }

        model.addAttribute("categories", categoryService.listForCurrentTenant());
        model.addAttribute("currentAlbum", album);
        model.addAttribute("galleries", galleries);
        model.addAttribute("galleryThumbnails", galleryThumbnails);
        model.addAttribute("shareTokens", shareTokenService.listForAlbum(albumId));
        return "album";
    }

    @PutMapping("/albums/{id}")
    public String updateAlbum(
        @PathVariable("id") Long albumId,
        @RequestParam("name") String name,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(value = "categoryId", required = false) Long categoryId,
        @RequestParam(value = "visibility", required = false) String visibility,
        @RequestParam(value = "redirectTo", required = false) String redirectTo,
        RedirectAttributes redirectAttributes
    ) {
        try {
            albumService.updateAlbum(
                albumId,
                categoryId,
                name,
                description,
                visibility
            );
            redirectAttributes.addFlashAttribute("message", "Album updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Failed to update album: " + ex.getMessage()
            );
        }

        String target = redirectTo != null
            ? safeRedirect(redirectTo).replace("{albumId}", albumId.toString())
            : ("/albums/" + albumId);
        return "redirect:" + target;
    }

    @PostMapping("/albums/{id}/share")
    public String createAlbumShareLink(
        @PathVariable("id") Long albumId,
        @RequestParam(value = "redirectTo", required = false) String redirectTo,
        RedirectAttributes redirectAttributes
    ) {
        try {
            shareTokenService.createForAlbum(albumId);
            redirectAttributes.addFlashAttribute("message", "Share link created.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Failed to create share link: " + ex.getMessage()
            );
        }

        String target = redirectTo != null
            ? safeRedirect(redirectTo).replace("{albumId}", albumId.toString())
            : ("/albums/" + albumId);
        return "redirect:" + target;
    }

    @DeleteMapping("/albums/{albumId}/share/{tokenId}")
    public String revokeAlbumShareLink(
        @PathVariable("albumId") Long albumId,
        @PathVariable("tokenId") UUID tokenId,
        @RequestParam(value = "redirectTo", required = false) String redirectTo,
        RedirectAttributes redirectAttributes
    ) {
        try {
            shareTokenService.revoke(albumId, tokenId);
            redirectAttributes.addFlashAttribute("message", "Share link revoked.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Failed to revoke share link: " + ex.getMessage()
            );
        }

        String target = redirectTo != null
            ? safeRedirect(redirectTo).replace("{albumId}", albumId.toString())
            : ("/albums/" + albumId);
        return "redirect:" + target;
    }

    @DeleteMapping("/albums/{id}")
    public String deleteAlbum(
        @PathVariable("id") Long albumId,
        @RequestParam(value = "redirectTo", required = false) String redirectTo,
        RedirectAttributes redirectAttributes
    ) {
        try {
            albumService.deleteAlbum(albumId);
            redirectAttributes.addFlashAttribute("message", "Album deleted.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Failed to delete album: " + ex.getMessage()
            );
            return "redirect:/albums/" + albumId;
        }

        String target = redirectTo != null
            ? safeRedirect(redirectTo)
            : "/dashboard";
        return "redirect:" + target;
    }

    private static String safeRedirect(String redirectTo) {
        String trimmed = redirectTo.trim();
        if (!trimmed.startsWith("/") || trimmed.contains("://")) {
            return "/";
        }
        return trimmed;
    }
}
