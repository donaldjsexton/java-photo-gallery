package com.example.photogallery.repository;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.ShareToken;
import com.example.photogallery.model.Tenant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShareTokenRepository extends JpaRepository<ShareToken, UUID> {
    @Query(
        """
        SELECT st FROM ShareToken st
        JOIN FETCH st.tenant
        JOIN FETCH st.album
        WHERE st.id = :id
          AND (st.expiresAt IS NULL OR st.expiresAt > :now)
        """
    )
    Optional<ShareToken> findValidById(
        @Param("id") UUID id,
        @Param("now") LocalDateTime now
    );

    Optional<ShareToken> findByIdAndTenant(UUID id, Tenant tenant);

    List<ShareToken> findByAlbum(Album album);

    void deleteByAlbum(Album album);
}
