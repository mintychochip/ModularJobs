# ModularJobs web

This workspace contains the ModularJobs documentation site and the secure
session-editor companion. The site documents the Paper plugin, MySQL 8
configuration, optional integrations, and operator-managed editor sessions.

## Project structure

```text
src/content/docs/   Starlight documentation
src/homepage/       React marketing homepage
src/pages/          Thin Astro hosts, including the editor handoff page
session-editor/     React secure session editor
rest-api/           Rust REST API backed by MySQL
```

## Documentation site

Run from `web/`:

```bash
npm install
npm run dev       # local site at http://localhost:4321
npm run build     # production site in dist/
npm run preview   # preview the production build
```

`npm run build` also builds the React session editor and bundles it under
`dist/editor/` so the Astro site and editor are served from the same
domain.

The site navigation is configured in `astro.config.mjs`. Markdown and MDX
content lives under `src/content/docs/`.

## Landing page

The marketing UI lives in React (`src/homepage/`). `src/pages/index.astro` is a
thin host that mounts that tree. Design tokens stay in `src/styles/site.css` —
Geist Sans/Mono with light and dark palettes selected by `data-theme` on
`<html>` and remembered in `localStorage` (`theme-preference`).

The download section reads GitHub releases through `src/lib/releases.ts`. The
build bakes in the release known at deploy time, then the page re-checks the API
on load, on tab focus, and every five minutes, so a newly published jar shows up
without a redeploy. If GitHub is unreachable the last known build stays visible.

## Secure session stack

The editor is opt-in from the Paper plugin. Operators must configure the Rust
REST API, MySQL 8 schema, web editor URL, and session-create secret explicitly;
the plugin does not launch either external service.

Per-server deployment:

```
                          https://modularjobs.com
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
        ▼                        ▼                        ▼
 /wiki/*                    /editor/                 (React editor)
 (Astro docs)            (React editor)               served at /editor/
                               │
                               │ direct load
                               ▼
                   ?api=https://s1-api.modularjobs.com
                   &code=ABC#token=SECRET

Each Paper server runs its own REST API on a unique origin (e.g.
s1-api.modularjobs.com, s2-api.modularjobs.com). The same editor domain
receives the per-server API base in the `?api=` query and validates it against
a build-time allow-list before sending the token.
```

Build the editor with your allowed API origins (must be HTTPS in production):

```bash
export VITE_ALLOWED_API_ORIGINS='https://*.modularjobs.com'
export VITE_SESSION_API_URL='https://modularjobs.com'   # fallback default
npm run build
```

`VITE_ALLOW_HTTP_API=true` is only for local smoke testing and must never be
set in production.

```bash
cd rest-api && cargo test   # requires a MySQL instance
cd ../session-editor && npm install && npm test && npm run build
```

The React editor uses the REST API. The deprecated Vue/Bytebin demo remains
only for legacy examples and is not the production session path.