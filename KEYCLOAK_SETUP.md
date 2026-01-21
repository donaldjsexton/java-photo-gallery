# Keycloak (production-ready local setup)

## 1) Start Keycloak

From the repo root:

```bash
docker compose -f docker-compose.keycloak.yml up
```

Then open `http://localhost:8080` and log in with:

- username: `admin` (override with `KEYCLOAK_ADMIN`)
- password: `admin` (override with `KEYCLOAK_ADMIN_PASSWORD`)

This compose file uses a persistent Postgres database and imports a realm named
`photo-gallery` with a confidential OIDC client named `photo-gallery`.

If you want non-default DB creds, set:

```bash
export KEYCLOAK_DB_USER=keycloak
export KEYCLOAK_DB_PASSWORD=keycloak
```

## Login styling (Keycloak theme)

This repo includes a custom Keycloak login theme that matches the app’s look-and-feel:

- Theme files: `keycloak/themes/photo-gallery/login`
- CSS to tweak: `keycloak/themes/photo-gallery/login/resources/css/photogallery.css`

The realm import (`keycloak/import/photo-gallery-realm.json`) sets `"loginTheme": "photo-gallery"` and the compose file mounts `./keycloak/themes` into the container.

If you already started Keycloak before adding/changing the theme, restart and re-import the realm:

```bash
docker compose -f docker-compose.keycloak.yml down -v
docker compose -f docker-compose.keycloak.yml up
```

Alternatively, in the Keycloak admin UI: Realm Settings → Themes → Login theme → `photo-gallery`.

## 2) Create a Keycloak user

In the Keycloak admin UI:

- Realm: `photo-gallery`
- Users → Create user
- Set a password (Credentials tab)

## 3) Run the app

Set env vars (or put them into your IDE run config):

```bash
export KEYCLOAK_BASE_URL=http://localhost:8080
export KEYCLOAK_REALM=photo-gallery
export KEYCLOAK_CLIENT_ID=photo-gallery
export KEYCLOAK_CLIENT_SECRET=change-me
```

Then start the app and visit `http://localhost:8090/login`.

If you see “Invalid redirect uri” on sign out:
- In Keycloak admin → Clients → `photo-gallery` → Logout settings, add `http://localhost:8090/*` (or at least `http://localhost:8090/login?logout`) to “Valid post logout redirect URIs”.

## Production notes

- Set `KC_HOSTNAME` to your public hostname and terminate TLS at a reverse proxy.
- If running behind a proxy, keep `KC_PROXY=edge` and ensure proxy headers are correct.
