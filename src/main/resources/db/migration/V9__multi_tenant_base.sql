CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPZ NOT NULL DEFAULT NOW()
);

CREATE TYPE membership_role AS ENUM ('OWNER','ADMIN','EDITOR','VIEWER');

CREATE TABLE memberships (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role membership_role NOT NULL,
    created_at TIMESTAMPZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, user_id)
);

- Add tenant to existing tables (backfill with a default tenant id if needed)
ALTER TABLE photos ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE galleries ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE gallery_photos ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE share_tokens ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- TODO: run an UPDATE to set tenant_id to your default tenant before NOT NULL
-- UPDATE photos SET tenant_id = 1 WHERE tenant_id IS NULL; (repeat for others)

ALTER TABLE photos ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE galleries ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE gallery_photos ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE share_tokens ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE photos ADD CONSTRAINT fk_photos_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE galleries ADD CONSTRAINT fk_galleries_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE gallery_photos ADD CONSTRAINT fk_gallery_photos_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE share_tokens ADD CONSTRAINT fk_share_tokens_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

-- Tenant-scoped uniques
CREATE UNIQUE INDEX IF NOT EXISTS uid_photos_hash_per_tenant ON photos (tenant_id, file_hash);
CREATE UNIQUE INDEX IF NOT EXISTS uid_photos_filename_per_tenant ON photos (tenant_id, file_name);
CREATE UNIQUE INDEX IF NOT EXISTS uid_gallery_photo_per_tenant ON gallery_photos (tenant_id, gallery_id, photo_id);
CREATE UNIQUE INDEX IF NOT EXISTS uid_share_tokens_per_tenant ON share_tokens (tenant_id, id);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_photos_tenant ON photos(tenant_id);
CREATE INDEX IF NOT EXISTS idx_galleries_tenant ON galleries(tenant_id);
CREATE INDEX IF NOT EXISTS idx_gallery_photos_tenant ON gallery_photos(tenant_id);
CREATE INDEX IF NOT EXISTS idx_share_tokens_tenant ON share_tokens(tenant_id);
CREATE INDEX IF NOT EXISTS idx_memberships_tenant_user ON memberships(tenant_id, user_id);
