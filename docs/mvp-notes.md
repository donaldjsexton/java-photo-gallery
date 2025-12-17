# MVP Recon Notes (Albums-First, Keycloak-Only)

This doc maps where the current app implements (or is missing) the MVP requirements, and lists the most likely files to touch per phase.

## Baseline recon (what exists today)

### Dashboard listing logic
- `src/main/java/com/example/photogallery/controller/DashboardController.java` — `GET /dashboard` currently loads **root galleries** (`galleryService.getRootGalleries()`), plus all albums/categories for the sidebar stats.
- `src/main/resources/templates/dashboard.html` — renders a **Galleries** grid (not albums); contains a filter form UI (`categoryId`, `albumId`, `sort`, `q`) that is not backed by controller logic yet.

### Category sorting UI and handlers
- `src/main/resources/templates/dashboard.html` — category “chip cloud” is static (no links/handlers), and the filter `<form>` has category/album/sort/search inputs but no server handling.
- `src/main/java/com/example/photogallery/service/CategoryService.java` — CRUD + deletion cascades; categories are tenant-scoped.
- `src/main/java/com/example/photogallery/model/Album.java` — already has `album.category` (matches “Category lives on album” requirement).
- `src/main/java/com/example/photogallery/repository/AlbumRepository.java` — has `findByTenantAndCategory(...)` (used by `CategoryService`).

### Masonry layout component
- `src/main/resources/templates/gallery.html` — photo grid uses `<div class="masonry" id="masonryGrid">…</div>`.
- `src/main/resources/static/css/style.css` — masonry is implemented via CSS columns (`.masonry { column-count: ... }`).

### Lightbox component
- `src/main/resources/templates/gallery.html` — inline JS attaches click handler to masonry tiles and toggles `.lightbox.open`.
- `src/main/resources/static/css/style.css` — `.lightbox` + `.lightbox-img` styles (currently includes rounded corners).

### Gallery management flow
- `src/main/resources/templates/gallery.html` — serves two modes:
  - `flowMode=true`: “wizard” steps (category → album → gallery → upload)
  - `flowMode=false`: gallery viewer + “Manage gallery” section + upload UI
- `src/main/java/com/example/photogallery/controller/AlbumFlowController.java` — `GET /flow/album` sets `flowMode=true` and reuses `gallery.html`.
- `src/main/java/com/example/photogallery/controller/GalleryController.java` — page routes (`/`, `/gallery/{id}`, `/{identifier}`) and form endpoints for categories/albums/galleries/uploads.
- `src/main/resources/static/js/upload.js` + inline JS in `gallery.html` — bulk upload via `fetch('/api/photos?...')` (plus a legacy auto-upload script).
- `src/main/java/com/example/photogallery/controller/PhotoRestController.java` — `POST /api/photos` supports `galleryId` for gallery-aware upload behavior.
- `src/main/java/com/example/photogallery/controller/GalleryRestController.java` — CRUD for galleries + add/remove/reorder photos in a gallery.

### Authentication/session logic
- `src/main/java/com/example/photogallery/config/SecurityConfig.java` — Keycloak-only OIDC using `oauth2Login()` + OIDC logout handler.
- `src/main/java/com/example/photogallery/controller/AuthController.java` — `GET /login` serves the Keycloak login handoff page.
- `src/main/resources/templates/login.html` — “Continue with Keycloak” only.
- `KEYCLOAK_SETUP.md` — documents Keycloak dev setup.

### Sign-out logic and styling
- `src/main/resources/templates/dashboard.html` — `POST /logout` button is rendered as a destructive action.
- `src/main/java/com/example/photogallery/config/SecurityConfig.java` — OIDC chain uses `OidcClientInitiatedLogoutSuccessHandler` with post-logout redirect to `/login?logout`.
- `src/main/resources/static/css/style.css` — `button.danger` exists (used for “Delete gallery”) and can be reused for sign-out.

### Sharing routes
- Data model exists for **gallery-level tokens**, but no user-facing share endpoints were found yet:
  - `src/main/java/com/example/photogallery/model/ShareToken.java`
  - `src/main/java/com/example/photogallery/repository/ShareTokenRepository.java`
  - `src/main/resources/db/migration/V7__create_share_tokens.sql`
- `src/main/java/com/example/photogallery/controller/GalleryController.java` — “public-ish” gallery identifiers exist (`/{slug}` or `/{uuid}`), but access is still gated by Spring Security (no public allowlist beyond `/login` and static assets).
- `TENANCY.md` — documents tenant-scoped slug/UUID gallery URLs and multi-tenant uploads; relevant for share links.

### Download logic
- No dedicated “download” endpoints were found.
- Images are currently served via static file mapping:
  - `src/main/java/com/example/photogallery/config/WebConfig.java` — maps `/uploads/**` → filesystem `uploads/`.
  - Templates link images directly via `/uploads/{photo.fileName}`.

## Data model baseline (albums vs galleries today)
- `src/main/java/com/example/photogallery/model/Album.java` — tenant + optional category; **no visibility field** yet.
- `src/main/java/com/example/photogallery/model/Gallery.java` — `album_id` is **NOT NULL** (already enforces “galleries cannot exist without an album” at DB/model level); has `visibility` field today.
- `src/main/resources/db/migration/V10__category_album_structure.sql` — introduces categories/albums and backfills `galleries.album_id`, then enforces NOT NULL.

## Files likely to be touched (one-line intent each)

### Phase 1 — Keycloak-only auth + logout
- `src/main/java/com/example/photogallery/config/SecurityConfig.java` — remove local auth chain; ensure Keycloak-only login and no “silent” re-auth after logout.
- `src/main/java/com/example/photogallery/controller/AuthController.java` — serve a single Keycloak-only login page (no branching).
- `src/main/resources/templates/login.html` — remove non-Keycloak login UI; keep “Continue with Keycloak” only.
- `src/main/resources/templates/dashboard.html` — update sign-out button styling to be clearly destructive (likely `button.danger`).
- `src/main/resources/application.yml` — delete/adjust config flags/profiles that enable non-Keycloak auth paths.
- `KEYCLOAK_SETUP.md` — remove/replace “Local auth fallback” docs once removed from code.

### Phase 2 — Album-first hierarchy + dashboard
- `src/main/java/com/example/photogallery/controller/DashboardController.java` — change `GET /dashboard` to list **albums only** (not root galleries), with optional category filter/sort.
- `src/main/resources/templates/dashboard.html` — replace “Galleries” grid with “Albums”; click album → album view (show its galleries).
- `src/main/java/com/example/photogallery/service/AlbumService.java` + `src/main/java/com/example/photogallery/repository/AlbumRepository.java` — add album listing helpers (filter/sort) to support dashboard.
- `src/main/java/com/example/photogallery/service/GalleryService.java` + `src/main/java/com/example/photogallery/repository/GalleryRepository.java` — add “list galleries by album” queries for album view.
- `src/main/java/com/example/photogallery/controller/GalleryController.java` — adjust routes so albums are the entrypoint and galleries live under an album view.
- `src/main/resources/templates/gallery.html` — split/adjust UI so “galleries list” is album-scoped (no dashboard root galleries).

### Phase 3 — Album privacy + sharing (public/private at album level)
- `src/main/java/com/example/photogallery/model/Album.java` — add `visibility = public|private` (default `private`).
- `src/main/resources/db/migration/*` — add migration for `albums.visibility` + indexes; reconcile with existing schema strategy (Flyway currently disabled by default).
- `src/main/java/com/example/photogallery/config/SecurityConfig.java` — allow unauthenticated access to public-album landing/share routes, while keeping private albums gated.
- `src/main/java/com/example/photogallery/controller/*` — add album share routes (album is the shareable unit) and enforcement checks.
- `src/main/java/com/example/photogallery/model/ShareToken.java` + `src/main/resources/db/migration/V7__create_share_tokens.sql` — likely needs refactor from gallery-level tokens → album-level tokens (or add a parallel album share token model/table).

### Phase 4 — Gallery management inside an album
- `src/main/java/com/example/photogallery/service/GalleryService.java` — enforce “create gallery only inside album” and inherit album visibility (gallery visibility likely becomes derived/unused).
- `src/main/java/com/example/photogallery/controller/GalleryController.java` + `src/main/resources/templates/*` — move “create gallery” action into album view only.
- `src/main/java/com/example/photogallery/controller/PhotoRestController.java` + `src/main/resources/templates/gallery.html` — keep incremental uploads to an existing gallery (already mostly present).
- `src/main/java/com/example/photogallery/service/AlbumService.java` (or new helper) — implement album cover derivation (first gallery’s cover image).

### Phase 5 — Layout & visual fixes
- `src/main/resources/static/css/style.css` — masonry last-row alignment fix (confirm whether the bug is in `.gallery-grid` or `.masonry`); remove rounded corners from lightbox and thumbnails.
- `src/main/resources/templates/gallery.html` — if masonry/lightbox needs markup/JS tweaks.

### Phase 6 — Multi-tenancy (tenant resolution + tenant-segmented uploads)
- `src/main/java/com/example/photogallery/config/TenantResolutionFilter.java` — resolve tenant from `X-Tenant` or host subdomain when `photo.gallery.tenant.mode=subdomain`.
- `src/main/java/com/example/photogallery/service/TenantService.java` — resolve/create the current tenant (default/per-user/subdomain).
- `src/main/java/com/example/photogallery/service/PhotoService.java` + `src/main/java/com/example/photogallery/service/PhotoStorageService.java` — store uploads under `uploads/{tenantSlug}/...` while keeping legacy flat files readable/deletable.
- `TENANCY.md` — tenancy modes + URL/upload behavior notes and reset instructions.

### Phase 7 — Downloads (single + full album zip)
- `src/main/java/com/example/photogallery/controller/*` — add endpoints for:
  - single-image downloads (web-size vs original)
  - album ZIP download (streaming)
- `src/main/java/com/example/photogallery/service/*` — add server-side ZIP generation and (if needed) web-optimized derivative generation/storage.
- `src/main/java/com/example/photogallery/config/WebConfig.java` — may need tightening (serving originals directly via `/uploads/**` can bypass visibility rules).

### Phase 8 — Category sorting on albums
- `src/main/java/com/example/photogallery/controller/DashboardController.java` — accept `categoryId` + stable sort, apply to album list.
- `src/main/resources/templates/dashboard.html` — wire category chips/form to actual filtering; ensure deterministic sorting.
- `src/main/java/com/example/photogallery/service/CategoryService.java` — keep category list behavior; update deletion behavior only if album visibility/share introduces new references.

### Phase 9 — Public landing page
- `src/main/java/com/example/photogallery/controller/*` — add unauthenticated landing route (product name “Deliverable” + login button + share link resolution).
- `src/main/resources/templates/*` — add landing page template and navigation updates.
