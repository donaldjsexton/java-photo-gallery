package com.example.photogallery.controller;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Photo;
import com.example.photogallery.model.enums.AlbumType;
import com.example.photogallery.model.enums.WorkflowStatus;
import com.example.photogallery.service.AlbumService;
import com.example.photogallery.service.PhotoService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/albums")
public class AlbumController {

    @Autowired
    private AlbumService albumService;

    @Autowired
    private PhotoService photoService;

    @GetMapping
    public String albumIndex(Model model) {
        // Get root structure for navigation
        List<Album> rootAlbums = albumService.getRootAlbums();
        List<Album> featuredAlbums = albumService.getFeaturedAlbums();
        List<Album> recentAlbums = albumService.getRecentAlbums(6);

        model.addAttribute("rootAlbums", rootAlbums);
        model.addAttribute("featuredAlbums", featuredAlbums);
        model.addAttribute("recentAlbums", recentAlbums);
        model.addAttribute("albumStats", albumService.getAlbumStatsByType());

        return "albums/index";
    }

    @GetMapping("/{albumId}")
    public String viewAlbum(@PathVariable Long albumId, Model model,
                          @RequestParam(value = "status", required = false) WorkflowStatus status,
                          @RequestParam(value = "view", defaultValue = "grid") String view) {
        Optional<Album> albumOpt = albumService.findById(albumId);
        if (!albumOpt.isPresent()) {
            return "redirect:/albums";
        }

        Album album = albumOpt.get();
        List<Album> childAlbums = albumService.getChildAlbums(album);
        List<Photo> photos = photoService.getPhotosInAlbum(album, status);

        model.addAttribute("album", album);
        model.addAttribute("childAlbums", childAlbums);
        model.addAttribute("photos", photos);
        model.addAttribute("breadcrumbs", album.getBreadcrumbs());
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentView", view);
        model.addAttribute("workflowStatuses", WorkflowStatus.values());
        model.addAttribute("coverPhoto", albumService.getCoverPhoto(album));

        return "albums/view";
    }

    @GetMapping("/slug/{slug}")
    public String viewAlbumBySlug(@PathVariable String slug, Model model) {
        Optional<Album> albumOpt = albumService.findBySlug(slug);
        if (!albumOpt.isPresent()) {
            return "redirect:/albums";
        }
        return "redirect:/albums/" + albumOpt.get().getId();
    }

    @GetMapping("/type/{albumType}")
    public String viewAlbumsByType(@PathVariable AlbumType albumType, Model model) {
        List<Album> albums = albumService.getAlbumsByType(albumType);

        model.addAttribute("albums", albums);
        model.addAttribute("albumType", albumType);
        model.addAttribute("pageTitle", albumType.getDisplayName());

        return "albums/by-type";
    }

    @GetMapping("/clients")
    public String viewClientAlbums(Model model,
                                 @RequestParam(value = "client", required = false) String clientName) {
        List<Album> clientAlbums;
        if (clientName != null && !clientName.trim().isEmpty()) {
            clientAlbums = albumService.searchByClient(clientName);
            model.addAttribute("searchClient", clientName);
        } else {
            clientAlbums = albumService.getClientAlbums();
        }

        model.addAttribute("clientAlbums", clientAlbums);
        model.addAttribute("pageTitle", "Client Sessions");

        return "albums/clients";
    }

    @GetMapping("/collections")
    public String viewCollections(Model model) {
        List<Album> collections = albumService.getCollections();
        List<Album> featuredCollections = albumService.getFeaturedAlbums();

        model.addAttribute("collections", collections);
        model.addAttribute("featuredCollections", featuredCollections);
        model.addAttribute("pageTitle", "Collections");

        return "albums/collections";
    }

    @GetMapping("/portfolio")
    public String viewPortfolio(Model model) {
        List<Album> portfolioAlbums = albumService.getPortfolioAlbums();
        List<Photo> featuredPhotos = photoService.getFeaturedPhotos();

        model.addAttribute("portfolioAlbums", portfolioAlbums);
        model.addAttribute("featuredPhotos", featuredPhotos);
        model.addAttribute("pageTitle", "Portfolio");

        return "albums/portfolio";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model,
                               @RequestParam(value = "parent", required = false) Long parentId,
                               @RequestParam(value = "type", defaultValue = "CLIENT") AlbumType albumType) {
        Album parentAlbum = null;
        if (parentId != null) {
            parentAlbum = albumService.findById(parentId).orElse(null);
        }

        model.addAttribute("parentAlbum", parentAlbum);
        model.addAttribute("albumType", albumType);
        model.addAttribute("albumTypes", AlbumType.values());

        return "albums/create";
    }

    @PostMapping("/create")
    public String createAlbum(@RequestParam String name,
                            @RequestParam(required = false) String description,
                            @RequestParam AlbumType albumType,
                            @RequestParam(required = false) Long parentId,
                            @RequestParam(required = false) String clientName,
                            @RequestParam(required = false) String shootDateStr,
                            @RequestParam(defaultValue = "false") boolean isPublic,
                            @RequestParam(defaultValue = "false") boolean isClientVisible,
                            @RequestParam(defaultValue = "false") boolean isFeatured,
                            RedirectAttributes redirectAttributes) {

        try {
            Album parent = null;
            if (parentId != null) {
                parent = albumService.findById(parentId).orElse(null);
            }

            Album album = albumService.createAlbum(name, albumType, parent);
            album.setDescription(description);
            album.setClientName(clientName);
            album.setIsPublic(isPublic);
            album.setIsClientVisible(isClientVisible);
            album.setIsFeatured(isFeatured);

            if (shootDateStr != null && !shootDateStr.trim().isEmpty()) {
                try {
                    album.setShootDate(LocalDateTime.parse(shootDateStr + "T00:00:00"));
                } catch (Exception e) {
                    // Invalid date format, ignore
                }
            }

            albumService.save(album);

            redirectAttributes.addFlashAttribute("success", "Album created successfully: " + name);
            return "redirect:/albums/" + album.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create album: " + e.getMessage());
            return "redirect:/albums/create";
        }
    }

    @GetMapping("/{albumId}/edit")
    public String showEditForm(@PathVariable Long albumId, Model model) {
        Optional<Album> albumOpt = albumService.findById(albumId);
        if (!albumOpt.isPresent()) {
            return "redirect:/albums";
        }

        Album album = albumOpt.get();
        model.addAttribute("album", album);
        model.addAttribute("albumTypes", AlbumType.values());

        return "albums/edit";
    }

    @PostMapping("/{albumId}/edit")
    public String updateAlbum(@PathVariable Long albumId,
                            @RequestParam String name,
                            @RequestParam(required = false) String description,
                            @RequestParam AlbumType albumType,
                            @RequestParam(required = false) String clientName,
                            @RequestParam(required = false) String shootDateStr,
                            @RequestParam(defaultValue = "false") boolean isPublic,
                            @RequestParam(defaultValue = "false") boolean isClientVisible,
                            @RequestParam(defaultValue = "false") boolean isFeatured,
                            RedirectAttributes redirectAttributes) {

        try {
            Optional<Album> albumOpt = albumService.findById(albumId);
            if (!albumOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Album not found");
                return "redirect:/albums";
            }

            Album album = albumOpt.get();
            album.setName(name);
            album.setDescription(description);
            album.setAlbumType(albumType);
            album.setClientName(clientName);
            album.setIsPublic(isPublic);
            album.setIsClientVisible(isClientVisible);
            album.setIsFeatured(isFeatured);

            if (shootDateStr != null && !shootDateStr.trim().isEmpty()) {
                try {
                    album.setShootDate(LocalDateTime.parse(shootDateStr + "T00:00:00"));
                } catch (Exception e) {
                    // Invalid date format, ignore
                }
            }

            albumService.save(album);

            redirectAttributes.addFlashAttribute("success", "Album updated successfully");
            return "redirect:/albums/" + albumId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update album: " + e.getMessage());
            return "redirect:/albums/" + albumId + "/edit";
        }
    }

    @PostMapping("/{albumId}/delete")
    public String deleteAlbum(@PathVariable Long albumId, RedirectAttributes redirectAttributes) {
        try {
            Optional<Album> albumOpt = albumService.findById(albumId);
            if (!albumOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Album not found");
                return "redirect:/albums";
            }

            Album album = albumOpt.get();
            Long parentId = album.getParent() != null ? album.getParent().getId() : null;

            albumService.deleteAlbum(albumId);

            redirectAttributes.addFlashAttribute("success", "Album deleted successfully");

            if (parentId != null) {
                return "redirect:/albums/" + parentId;
            } else {
                return "redirect:/albums";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete album: " + e.getMessage());
            return "redirect:/albums/" + albumId;
        }
    }

    @GetMapping("/search")
    public String searchAlbums(@RequestParam(required = false) String name,
                             @RequestParam(required = false) String client,
                             @RequestParam(required = false) AlbumType type,
                             @RequestParam(required = false) Boolean isPublic,
                             @RequestParam(required = false) Boolean isFeatured,
                             Model model) {

        List<Album> results = albumService.advancedSearch(name, client, type, isPublic, isFeatured);

        model.addAttribute("results", results);
        model.addAttribute("searchName", name);
        model.addAttribute("searchClient", client);
        model.addAttribute("searchType", type);
        model.addAttribute("searchIsPublic", isPublic);
        model.addAttribute("searchIsFeatured", isFeatured);
        model.addAttribute("albumTypes", AlbumType.values());

        return "albums/search";
    }

    // AJAX endpoints for dynamic operations
    @PostMapping("/{albumId}/photos/{photoId}/move")
    @ResponseBody
    public ResponseEntity<Map<String, String>> movePhoto(@PathVariable Long albumId,
                                                       @PathVariable Long photoId,
                                                       @RequestParam Long targetAlbumId) {
        Map<String, String> response = new HashMap<>();

        try {
            Optional<Photo> photoOpt = photoService.findById(photoId);
            Optional<Album> targetAlbumOpt = albumService.findById(targetAlbumId);

            if (!photoOpt.isPresent() || !targetAlbumOpt.isPresent()) {
                response.put("status", "error");
                response.put("message", "Photo or target album not found");
                return ResponseEntity.badRequest().body(response);
            }

            albumService.movePhotoToAlbum(photoOpt.get(), targetAlbumOpt.get());

            response.put("status", "success");
            response.put("message", "Photo moved successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to move photo: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{albumId}/photos/reorder")
    @ResponseBody
    public ResponseEntity<Map<String, String>> reorderPhotos(@PathVariable Long albumId,
                                                           @RequestBody List<Long> photoIds) {
        Map<String, String> response = new HashMap<>();

        try {
            Optional<Album> albumOpt = albumService.findById(albumId);
            if (!albumOpt.isPresent()) {
                response.put("status", "error");
                response.put("message", "Album not found");
                return ResponseEntity.badRequest().body(response);
            }

            albumService.updatePhotoOrder(albumOpt.get(), photoIds);

            response.put("status", "success");
            response.put("message", "Photos reordered successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to reorder photos: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{albumId}/cover/{photoId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> setCoverPhoto(@PathVariable Long albumId,
                                                           @PathVariable Long photoId) {
        Map<String, String> response = new HashMap<>();

        try {
            Optional<Album> albumOpt = albumService.findById(albumId);
            Optional<Photo> photoOpt = photoService.findById(photoId);

            if (!albumOpt.isPresent() || !photoOpt.isPresent()) {
                response.put("status", "error");
                response.put("message", "Album or photo not found");
                return ResponseEntity.badRequest().body(response);
            }

            albumService.setCoverPhoto(albumOpt.get(), photoOpt.get());

            response.put("status", "success");
            response.put("message", "Cover photo set successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to set cover photo: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/initialize")
    @ResponseBody
    public ResponseEntity<Map<String, String>> initializeDefaultHierarchy() {
        Map<String, String> response = new HashMap<>();

        try {
            albumService.createDefaultHierarchy();
            response.put("status", "success");
            response.put("message", "Default album hierarchy created successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to initialize hierarchy: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/organize-by-date")
    @ResponseBody
    public ResponseEntity<Map<String, String>> organizePhotosByDate() {
        Map<String, String> response = new HashMap<>();

        try {
            albumService.organizePhotosByDate();
            response.put("status", "success");
            response.put("message", "Photos organized by date successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to organize photos: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Helper method to get album navigation data
    @ModelAttribute("albumNavigation")
    public Map<String, Object> albumNavigation() {
        Map<String, Object> nav = new HashMap<>();
        nav.put("clientCount", albumService.getClientAlbums().size());
        nav.put("collectionCount", albumService.getCollections().size());
        nav.put("portfolioCount", albumService.getPortfolioAlbums().size());
        return nav;
    }
}
