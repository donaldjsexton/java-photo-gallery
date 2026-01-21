package com.example.photogallery.service;

import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.TenantRepository;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    @Value("${photo.gallery.default-tenant-slug:default}")
    private String defaultTenantSlug;

    @Value("${photo.gallery.default-tenant-name:Default Tenant}")
    private String defaultTenantName;

    @Value("${photo.gallery.tenant.mode:per-user}")
    private String tenantMode;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @PostConstruct
    public void ensureDefaultTenant() {
        getOrCreateBySlug(defaultTenantSlug, defaultTenantName);
    }

    public Tenant getDefaultTenant() {
        return getOrCreateBySlug(defaultTenantSlug, defaultTenantName);
    }

    /**
     * Resolves the tenant for the current authenticated user.
     *
     * <p>Modes:
     * <ul>
     *   <li><code>default</code>: always returns the default tenant</li>
     *   <li><code>per-user</code> (default): creates/uses a tenant derived from the user identity</li>
     * </ul>
     */
    public Tenant getCurrentTenant() {
        if ("subdomain".equalsIgnoreCase(tenantMode)) {
            String slug = TenantContext.getTenantSlug();
            if (StringUtils.hasText(slug)) {
                return getOrCreateBySlug(slug, slug);
            }
            return getDefaultTenant();
        }

        if ("default".equalsIgnoreCase(tenantMode)) {
            return getDefaultTenant();
        }

        String userKey = resolveCurrentUserKey();
        if (!StringUtils.hasText(userKey)) {
            return getDefaultTenant();
        }

        String slug = slugForUserKey(userKey);
        String name = nameForUserKey(userKey);
        return getOrCreateBySlug(slug, name);
    }

    public Tenant save(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    public Tenant getOrCreateBySlug(String slug, String name) {
        return tenantRepository
            .findBySlug(slug)
            .orElseGet(() -> {
                try {
                    return tenantRepository.save(new Tenant(slug, name));
                } catch (DataIntegrityViolationException e) {
                    return tenantRepository.findBySlug(slug).orElseThrow(() -> e);
                }
            });
    }

    private static String resolveCurrentUserKey() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String s && StringUtils.hasText(s)) {
            if (!"anonymousUser".equalsIgnoreCase(s)) return s;
        }

        String name = auth.getName();
        return StringUtils.hasText(name) ? name : null;
    }

    private static String nameForUserKey(String userKey) {
        String trimmed = userKey.trim();
        if (trimmed.length() > 255) {
            return trimmed.substring(0, 255);
        }
        return trimmed;
    }

    private static String slugForUserKey(String userKey) {
        String normalized = userKey.trim().toLowerCase(Locale.ROOT);
        String base =
            normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (!StringUtils.hasText(base)) {
            base = "user";
        }

        String hash = sha256Hex(normalized).substring(0, 12);
        String prefix = "u-";
        String suffix = "-" + hash;
        int maxBaseLen = 64 - (prefix.length() + suffix.length());
        if (base.length() > maxBaseLen) {
            base = base.substring(0, maxBaseLen);
        }
        return prefix + base + suffix;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
