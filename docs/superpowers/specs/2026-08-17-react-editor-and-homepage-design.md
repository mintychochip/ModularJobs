# React Editor + Plugin Homepage Design

## Goal

Replace the Astro `/editor` and `/editor/session` pages with a single Vite React session editor using a clean dashboard layout, and redesign the Astro site homepage to match the same visual system.

## Context

- The current `web/session-editor` is a Vite React app served under `/session-editor/`.
- `web/src/pages/editor/index.astro` is a legacy landing form.
- `web/src/pages/editor/session.astro` is a handoff page that iframes the React editor.
- The Paper `EditorService` currently generates `https://<site>/editor/session?api=...&code=...#token=...`.
- `web/src/pages/index.astro` plus `Body.astro` / `Hero.astro` is the current plugin homepage.

## Architecture

### React editor

A single React app remains in `web/session-editor` but becomes a full-screen editor dashboard with an inline auth card. It is built into `public/editor` and served at `https://modularjobs.com/editor/`. The app detects URL credentials; if they are missing it shows the auth card, otherwise it loads the session. The token stays in the hash, the per-server API origin stays in `?api=`.

### Plugin homepage

The Astro `index.astro` and its supporting components are redesigned in the same Tailwind v4 + daisyUI dark theme with a hero, feature grid, quick-start steps, and an editor teaser.

### Shared visual system

Both the React editor and the Astro homepage use the same color palette and Tailwind/daisy setup. The React editor switches from its current hand-written BEM CSS to Tailwind classes anchored by `SessionEditor.css` with `@import "tailwindcss"` and `@plugin "daisyui"`.

## Security invariants

- The session token is placed in the URL **hash fragment**, not the query string, so it is not sent to the server in the request line or leaked via `Referer`.
- `?api=` is validated against `VITE_ALLOWED_API_ORIGINS` before any request carrying the token.
- The token is sent in the `Authorization: Bearer` and `X-Session-Token` headers to the validated API origin.
- Non-HTTPS `?api=` origins are rejected unless `VITE_ALLOW_HTTP_API=true` (dev only).
- Paper still creates sessions; the browser editor only loads and saves.

## Deployment

Per Paper server:

```yaml
editor:
  enabled: true
  session-api-url: https://s1-api.modularjobs.com
  web-editor-url: https://modularjobs.com/editor
  session-create-secret: <same as REST API>
  session-ttl-minutes: 1440
```

Build command:

```bash
cd web
export VITE_ALLOWED_API_ORIGINS='https://*.modularjobs.com'
export VITE_SESSION_API_URL='https://modularjobs.com'
npm run build
```

Reverse proxy:

- `https://modularjobs.com/` → `web/dist/`
- `https://modularjobs.com/editor/` → `web/dist/editor/`
- `https://s1-api.modularjobs.com/` → the Rust REST API

## Acceptance criteria

1. `npm run build` in `web/` produces `dist/editor/index.html` and `dist/editor/assets/*`.
2. The Paper-generated URL `https://modularjobs.com/editor/?api=...&code=...#token=...` opens the editor with the correct session loaded.
3. Visiting `https://modularjobs.com/editor/` with no credentials shows an auth card.
4. The Astro homepage renders the new hero, feature grid, and quick-start sections.
5. `npm test` in `web/session-editor` passes.
6. `./gradlew :paper:compileJava` passes after `EditorService` changes.
7. `cargo test --lib` in `web/rest-api` still passes.