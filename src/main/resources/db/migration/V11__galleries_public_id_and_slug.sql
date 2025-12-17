-- Add stable public identifiers and human-friendly slugs to galleries.
-- These are used for shareable URLs and should be unique per tenant.

ALTER TABLE galleries
    ADD COLUMN IF NOT EXISTS public_id UUID,
    ADD COLUMN IF NOT EXISTS slug VARCHAR(255);

-- Enforce tenant-scoped uniqueness (NULLs are allowed and do not collide in Postgres).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_galleries_tenant_public_id'
    ) THEN
        ALTER TABLE galleries
            ADD CONSTRAINT uq_galleries_tenant_public_id UNIQUE (tenant_id, public_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_galleries_tenant_slug'
    ) THEN
        ALTER TABLE galleries
            ADD CONSTRAINT uq_galleries_tenant_slug UNIQUE (tenant_id, slug);
    END IF;
END $$;

-- Helpful lookup indexes (some DBs will use the UNIQUE btree, but keep explicit for clarity).
CREATE INDEX IF NOT EXISTS idx_galleries_tenant_public_id ON galleries (tenant_id, public_id);
CREATE INDEX IF NOT EXISTS idx_galleries_tenant_slug ON galleries (tenant_id, slug);
