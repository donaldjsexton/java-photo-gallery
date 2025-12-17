# Multi-tenant URL + uploads

## Tenant resolution

`photo.gallery.tenant.mode` (or `PHOTO_GALLERY_TENANT_MODE`) supports:

- `per-user` (default): tenant derived from the authenticated user identity.
- `default`: always uses the `default` tenant.
- `subdomain`: resolves tenant from `X-Tenant` header (preferred) or the request host subdomain.

For `subdomain`, `{tenant}` must match `^[a-z0-9][a-z0-9-]{0,63}$`.

## Gallery URLs

- New: `/{slug}` or `/{uuid}` (gallery `slug` or `publicId`)
- Legacy: `/gallery/{id}` redirects to the slug URL when available.

## Uploads

New uploads are stored tenant-segmented:

- `uploads/{tenantSlug}/{uuid}.{ext}`

Legacy flat files (`uploads/{uuid}.{ext}`) still work; deletion/reads handle both formats.

## Reset / migration notes

This project currently uses `spring.jpa.hibernate.ddl-auto=update` and has Flyway disabled by default.
If you’d rather “start fresh” than deal with incremental schema changes:

- Delete the H2 dev DB: `rm -f target/devdb*`
- (Optional) delete uploads: `rm -rf uploads/`

## Deletion + cleanup

- Deleting a gallery removes its gallery mappings; any photos that become unreferenced (not in any gallery and not used as a cover photo) are automatically purged from the DB and `uploads/`, so they can be reuploaded.
- If you already have orphaned photos from earlier versions, run `POST /api/maintenance/purge-orphaned-photos` for the current tenant.

## Album names

- Album names are allowed to repeat within a tenant (e.g., two clients with the same last name).
- If you have an existing dev DB created when album names were unique, the simplest fix is to recreate it: `rm -f target/devdb*` (Hibernate `ddl-auto=update` may not drop old unique constraints automatically).
