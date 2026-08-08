# ModularJobs Secure Session Editor (React)

React UI for loading/editing/saving job task sessions against the Rust
`web/rest-api` (Postgres-backed). **Does not use bytebin.lucko.me.**

## Configure

```bash
export VITE_SESSION_API_URL=http://127.0.0.1:18787
npm install
npm run dev
```

Open with the public code in the query and secret token in the fragment:

```
http://localhost:5174/?code=<sessionCode>#token=<sessionToken>
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
