package com.example.photogallery.service;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.ShareToken;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.ShareTokenRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ShareTokenService {

    private final ShareTokenRepository shareTokenRepository;
    private final AlbumService albumService;
    private final TenantService tenantService;

    public ShareTokenService(
        ShareTokenRepository shareTokenRepository,
        AlbumService albumService,
        TenantService tenantService
    ) {
        this.shareTokenRepository = shareTokenRepository;
        this.albumService = albumService;
        this.tenantService = tenantService;
    }

    public List<ShareToken> listForAlbum(Long albumId) {
        Album album = albumService.getById(albumId);
        return shareTokenRepository.findByAlbum(album);
    }

    public ShareToken createForAlbum(Long albumId) {
        Album album = albumService.getById(albumId);
        Tenant tenant = tenantService.getCurrentTenant();
        ShareToken token = new ShareToken(tenant, album, null, null);
        return shareTokenRepository.save(token);
    }

    public void revoke(Long albumId, UUID tokenId) {
        Album album = albumService.getById(albumId);
        Tenant tenant = tenantService.getCurrentTenant();
        ShareToken token = shareTokenRepository
            .findByIdAndTenant(tokenId, tenant)
            .orElseThrow(() -> new NoSuchElementException("Share token not found"));

        if (!token.getAlbum().getId().equals(album.getId())) {
            throw new NoSuchElementException("Share token not found");
        }

        shareTokenRepository.delete(token);
    }

    public ShareToken resolveValid(UUID tokenId) {
        return shareTokenRepository
            .findValidById(tokenId, LocalDateTime.now())
            .orElseThrow(() -> new NoSuchElementException("Share link not found"));
    }
}
