# Unification Notice

This project is being consolidated into the Laravel business platform
**`donald-sexton-photography`**. The decision (recorded on the
`claude/project-unification-plan-qiexk2` branch of that repo, at
`docs/architecture/unification-plan.md`) is to **reimplement this gallery
engine natively in Laravel** rather than run it as a separate service.

## Why this repo still matters

Going forward this codebase is the **authoritative functional spec** for the
gallery ingestion and delivery behaviors. The Laravel port must preserve the
behaviors this project was built to guarantee:

- **Hash-first dedup** — SHA-256 before storage expansion, unique per tenant.
- **Idempotent / restart-safe uploads** — safe to retry; partial failures leave
  a recoverable state.
- **Non-blocking EXIF** — metadata extraction failure never blocks storage.
- **Fail-fast on corrupt files** — validate before persistence.
- **Auditable outcomes** — success / duplicate / failure recorded per upload.

## Mapping to Laravel

| This project | Laravel target |
|--------------|----------------|
| `Tenant` / `TenantResolutionFilter` | existing `Site` / `SiteDomain` |
| `Gallery`, `Album`, `Photo`, `GalleryPhoto`, `ShareToken` | new site-scoped models |
| `ExifService` | Intervention Image / native PHP EXIF |
| `R2PhotoStorageService` / `LocalPhotoStorageService` | existing Laravel `s3` (R2) disk |
| `SignedUrlService` | `URL::temporarySignedRoute` / R2 presign |
| `DownloadService` | streamed ZIP route |

The full phased roadmap, domain mapping, and production-safety notes live in
the Laravel repo's `docs/architecture/unification-plan.md`.

## Status

**Parity reached — this repo is now archived as the spec-of-record.**

The Laravel Galleries domain has been implemented through all planned phases
(data model, ingestion with hash dedup/EXIF/variants, token-gated delivery,
admin CMS, CRM + client-portal integration with an opt-in payment gate, and
native galleries embedded in editorial content). It covers every behaviour
this engine guaranteed.

No data was migrated: production galleries lived in Pic-Time, so the
consolidation was a greenfield capability port (owner decision). This codebase
is retained read-only as the functional reference; **no further feature work
should land here.**
