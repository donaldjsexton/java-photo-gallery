package com.example.photogallery.repository;

import com.example.photogallery.model.Membership;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository
    extends JpaRepository<Membership, Long> {
    Optional<Membership> findByTenantAndUser(Tenant tenant, User user);
    List<Membership> findByUser(User user);
}
