package com.example.photogallery.config;

import com.example.photogallery.service.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantResolutionFilter extends OncePerRequestFilter {

    @Value("${photo.gallery.tenant.mode:per-user}")
    private String tenantMode;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            if ("subdomain".equalsIgnoreCase(tenantMode)) {
                String fromHeader = normalize(request.getHeader("X-Tenant"));
                String resolved = StringUtils.hasText(fromHeader)
                    ? fromHeader
                    : resolveFromHost(request.getServerName());
                if (StringUtils.hasText(resolved) && isValidTenantSlug(resolved)) {
                    TenantContext.setTenantSlug(resolved);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private static String resolveFromHost(String serverName) {
        String host = normalize(serverName);
        if (!StringUtils.hasText(host)) return null;

        // strip trailing dot
        if (host.endsWith(".")) host = host.substring(0, host.length() - 1);

        int firstDot = host.indexOf('.');
        if (firstDot <= 0) return null;

        return host.substring(0, firstDot);
    }

    private static String normalize(String raw) {
        return StringUtils.hasText(raw) ? raw.trim().toLowerCase(Locale.ROOT) : null;
    }

    private static boolean isValidTenantSlug(String slug) {
        return slug != null && slug.matches("^[a-z0-9][a-z0-9-]{0,63}$");
    }
}
