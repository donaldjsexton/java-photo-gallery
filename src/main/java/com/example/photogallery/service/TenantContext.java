package com.example.photogallery.service;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT_SLUG =
        new ThreadLocal<>();

    private TenantContext() {}

    public static String getTenantSlug() {
        return CURRENT_TENANT_SLUG.get();
    }

    public static void setTenantSlug(String tenantSlug) {
        CURRENT_TENANT_SLUG.set(tenantSlug);
    }

    public static void clear() {
        CURRENT_TENANT_SLUG.remove();
    }
}

