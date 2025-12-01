package com.example.photogallery.repository;

import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.ShareToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareTokenRepository extends JpaRepository<ShareToken, UUID> {
    Optional<ShareToken> findByIdAndExpiresAtAfter(UUID id, LocalDateTime now);

    List<ShareToken> findByGallery(Gallery gallery);

    void deleteByGallery(Gallery gallery);
}
