CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name)
);

CREATE TABLE albums (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name)
);

-- Add album link to galleries
ALTER TABLE galleries ADD COLUMN IF NOT EXISTS album_id BIGINT;

-- Seed default category/album per tenant
INSERT INTO categories (tenant_id, name, description)
SELECT t.id, 'General', 'Default category'
FROM tenants t
ON CONFLICT (tenant_id, name) DO NOTHING;

INSERT INTO albums (tenant_id, category_id, name, description)
SELECT t.id, c.id, 'Default Album', 'Auto-created album'
FROM tenants t
LEFT JOIN categories c ON c.tenant_id = t.id AND c.name = 'General'
ON CONFLICT (tenant_id, name) DO NOTHING;

-- Backfill existing galleries to default album
WITH a AS (
    SELECT a.id, a.tenant_id FROM albums a WHERE a.name = 'Default Album'
)
UPDATE galleries g
SET album_id = a.id
FROM a
WHERE g.tenant_id = a.tenant_id AND g.album_id IS NULL;

-- Enforce NOT NULL and FK after backfill
ALTER TABLE galleries ALTER COLUMN album_id SET NOT NULL;
ALTER TABLE galleries ADD CONSTRAINT fk_galleries_album FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE;

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_categories_tenant ON categories(tenant_id);
CREATE INDEX IF NOT EXISTS idx_albums_tenant ON albums(tenant_id);
