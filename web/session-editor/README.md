# ModularJobs Secure Session Editor (React)

React UI for loading/editing/saving job task sessions against the Rust
`web/rest-api` (MySQL-backed). **Does not use bytebin.lucko.me.**

Session creation is handled by the Paper server. The editor only consumes the
handoff URL it generates.

## Configure

```bash
# static build-time defaults
export VITE_SESSION_API_URL=http://127.0.0.1:18787
# allow per-server API origins passed via ?api= (comma-separated globs)
export VITE_ALLOWED_API_ORIGINS='https://*.modularjobs.com'
npm install
npm run dev
```

Open with the public code in the query and secret token in the fragment. The
per-server API base is in `?api=`:

```
http://localhost:5174/?api=https://s1.modularjobs.com&code=<sessionCode>#token=<sessionToken>
```

Legacy `&token=<sessionToken>` query URLs are accepted for migration only and
must not be generated.

## Scripts

| Command | Purpose |
|---------|---------|
| `npm test` | Client + edit pipeline unit tests |
| `npm run build` | Production build → `dist/` |
| `npm run dev` | Vite dev server |

## Auth model

Session **code** (public id) + **token** (secret) are required for load and save.
The API rejects missing/wrong tokens so unauthenticated callers cannot read or
overwrite another session.

The editor validates the `?api=` API origin against `VITE_ALLOWED_API_ORIGINS`
before sending the token. Non-HTTPS origins are rejected unless
`VITE_ALLOW_HTTP_API=true` is set for local development.