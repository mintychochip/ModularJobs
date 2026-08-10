# General Paper Distribution Hardening Design

**Date:** 2026-08-10

**Status:** Design approved; written specification pending user review.


## Goal

Make the ModularJobs Paper release more generally distributable by removing the
Mint and Preferences build/runtime requirements and reducing organization-specific
defaults. Keep the existing Paper target and Craftux-backed UI for now; Craftux
remains the explicitly deferred private dependency.

## Scope

### Included

- Mint is optional at compile time and runtime.
- Mint is used when its Bukkit service is available and ready.
- Missing Mint defaults to a loud, rate-limited blackhole economy provider that
  consumes money payables without granting currency.
- Operators can select a fail-fast missing-provider policy when real currency is
  mandatory.
- Preferences uses ModularJobs' local implementation; the external Preferences
  API is removed from required compilation and CI resolution.
- Remove tracked IDE metadata and personal/local database paths.
- Keep a generic Minecraft starter content pack, but remove AzothMC roadmap and
  server-specific branding from runtime/public API content.
- Make the editor opt-in and remove owner-hosted endpoint defaults.
- Remove starter-template documentation and correct stale integration/database
  claims.
- Keep Craftux and the existing Craftux UI wiring unchanged as an explicit
  deferred dependency.

### Deferred

- Replacing Craftux with native Paper inventory/chat UI.
- Removing the Craftux artifact from the Paper runtime jar.
- Supporting non-Paper server platforms.
- Reworking the full jobs/task content model into a separately downloadable pack.

## Architecture

### Economy provider selection

`EconomyProviderFactory` selects providers in this order:

1. Detect the Mint plugin and its service class reflectively, without importing
   Mint types or resolving the Mint artifact during base compilation.
2. If Mint is present and ready, use `ReflectiveMintEconomyProvider`.
3. If Mint is absent or unavailable, select the configured missing-provider
   policy:
   - `blackhole` (default): use `BlackholeEconomyProvider`.
   - `fail`: throw during plugin wiring with an actionable message.

The blackhole provider:

- Reports currency support so economy payables remain valid and do not throw at
  payment time.
- Validates positive amounts and returns success after recording no currency.
- Logs one warning per plugin enable/configuration selection, not one warning per
  reward.
- States clearly that currency rewards are discarded until an economy provider is
  installed.

Configuration:

```yaml
economy:
  required: false
  missing-provider: blackhole # blackhole | fail
```

`economy.required: true` remains accepted for compatibility and maps to the
`fail` policy unless `missing-provider` is explicitly set. The generated default
uses `required: false` and `missing-provider: blackhole`.

### Preferences

The local `PreferencesServiceImpl` remains the always-available implementation.
The external Preferences adapter, dependency coordinate, repository, and CI
checkout are removed. Existing command behavior continues through the local
service and config defaults.

### Content and public API

- Preserve useful starter jobs/tasks/boosts/upgrades.
- Remove AzothMC roadmap references, server-specific comments, and claims that
  imply a fixed external progression design.
- Mark shipped YAML/JSON/CSV values as starter content in operator documentation.
- Keep namespaced ModularJobs keys stable for compatibility.
- Do not redesign the task schema in this hardening pass.

### Editor defaults

The editor is disabled by default. Session API and web editor URLs are empty in
fresh defaults and must be explicitly configured when enabled. No owner-hosted
Vercel URL is shipped as a runtime default.

### Repository hygiene and documentation

- Remove tracked `.idea` metadata; add `.idea/` to `.gitignore`.
- Remove generic Astro starter pages/readme content.
- Correct landing-page claims to match PostgreSQL-only persistence and the current
  optional Mint model.
- Mark historical plans as archival where they mention obsolete module paths or
  removed providers.
- Document Craftux as the remaining deferred distribution dependency.

## Error handling

- Reflection failures for Mint are treated as provider absence, not plugin-enable
  failure when the selected policy is `blackhole`.
- Mint deposit failures return false and are logged by the Mint adapter; they do
  not trigger retries or blackhole fallback after a partially completed Mint
  operation.
- `fail` policy errors identify the missing provider and configuration key.
- Blackhole logging must not expose player secrets or session tokens.

## Verification

- Unit-test blackhole amount handling and provider selection.
- Unit-test reflective Mint absence without Mint classes on the test classpath.
- Compile and run the Paper module without Mint or Preferences artifacts in the
  dependency repositories; Craftux remains available through its existing
  deferred path.
- Run API/common/Paper tests and static checks.
- Inspect the shadow jar for generated defaults and the intentionally deferred
  Craftux classes.
- Run web tests/build and scan tracked files for personal paths, starter text,
  owner-hosted URLs, AzothMC roadmap references, and stale database claims.
