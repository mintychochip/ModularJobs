# ModularJobs Secure Session Editor (React)

React UI for loading/editing/saving job task sessions against the Rust
`web/session-api` (Postgres-backed). **Does not use bytebin.lucko.me.**

## Configure

```bash
export VITE_SESSION_API_URL=http://127.0.0.1:18787
npm install
npm run dev
```

Open with query params:

```
http://localhost:5174/?code=<sessionCode>&token=<sessionToken>
```

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
