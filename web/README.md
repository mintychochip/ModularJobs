# ModularJobs web

This workspace contains the ModularJobs documentation site and the secure
session-editor companion. The site documents the Paper plugin, PostgreSQL
configuration, optional integrations, and operator-managed editor sessions.

## Project structure

```text
src/content/docs/   Starlight documentation
src/components/     Astro landing-page components
src/pages/          Astro routes, including the editor handoff page
session-editor/     React secure session editor
rest-api/           Rust REST API backed by PostgreSQL
```

## Documentation site

Run from `web/`:

```bash
npm install
npm run dev       # local site at http://localhost:4321
npm run build     # production site in dist/
npm run preview   # preview the production build
```

The site navigation is configured in `astro.config.mjs`. Markdown and MDX
content lives under `src/content/docs/`.

## Secure session stack

The editor is opt-in from the Paper plugin. Operators must configure the Rust
REST API, PostgreSQL schema, web editor URL, and session-create secret
explicitly; the plugin does not launch either external service.

```bash
cd rest-api && cargo test
cd ../session-editor && npm install && npm test && npm run build
```

The React editor uses the REST API. The deprecated Vue/Bytebin demo remains
only for legacy examples and is not the production session path.
