# Per-Player XP Boss Bar Color via Preferences API

> Status: proposed
> Date: 2026-08-25
> Owners: modularjobs maintainers

## Problem

The XP boss bar shown during job experience gain is hardcoded green
(`ExperienceBarColorProvider` returns `BossBar.Color.GREEN`). The stub's javadoc
already anticipates a preferences-backed implementation ("Experience bar color
when preferences are not available (default green)").

## Goal

Let each player choose their own XP boss bar color through the external
`aincraft-org/preferences` plugin's native dialog GUI, falling back to the
current green default when that plugin is absent.

## Non-goals

- Chat theme colors (`Messages` MiniMessage tags). Out of scope; would require
  threading a per-player theme through every call site.
- Global (server-wide) color preference. Player-scoped only.
- Implementing a preferences system inside ModularJobs. The external plugin
  owns dialogs, validation, caching, and YAML persistence.

## Background

ModularJobs previously shipped an external Preferences adapter
(`ExternalBackedPreferencesService`, `PreferencesIntegration`) that registered
`entries-per-page` and `gui-mode` as player preferences. It was removed during
2026-08-10 distribution hardening in favor of a local in-memory
`PreferencesServiceImpl`. The `aincraft-org/preferences` plugin (API coordinate
`dev.mintychochip:preferences-api`) is a superset of the old `dev.jlo`
API — same `PreferencesService.register` / `Preference<T>` shape, plus a native
Paper dialog GUI and YAML persistence.

## Architecture

### Dependency

- `compileOnly("dev.mintychochip:preferences-api:0.2.0")` in
  `paper/build.gradle.kts`. Not shaded — the Preferences plugin provides the API
  at runtime, same pattern as Mint.
- Add `Preferences` to `plugin.yml` `softdepend`.

### Reproducible resolution (CI / clean environments)

The preferences repo publishes only via `publishToMavenLocal` (no
`build/maven-repo` task), so the reproducible path mirrors the existing stale CI
step, pointed at the new repo:

1. Pinned checkout of `aincraft-org/preferences` at SHA
   `c18236c1fa844eb0ae26824e524ae4605a9b41df` into `preferences/`.
2. Publish the API module at the pinned `0.2.0` version:
   `./gradlew :preferences-api:publishToMavenLocal -PbuildVersion=0.2.0
   -Dmaven.repo.local=${{ runner.temp }}/m2`. The preferences repo's version
   defaults to a dated CalVer `-SNAPSHOT`; `-PbuildVersion` forces the exact
   coordinate ModularJobs depends on. Verified to produce
   `dev.mintychochip:preferences-api:0.2.0` in the isolated repo.
3. The `java` CI job's `clean check` and `shadowJar` already pass
   `-Dmaven.repo.local=${{ runner.temp }}/m2`, so the artifact resolves there.

Locally, the sibling checkout (`../preferences`) publishes to `mavenLocal()`
(already a repository in `settings.gradle.kts`):
`./gradlew -p ../preferences :preferences-api:publishToMavenLocal -PbuildVersion=0.2.0`.

### Registration

A new wiring class (e.g. `PreferencesIntegration` in `dev.mintychochip.service`)
performs soft integration at enable:

1. Load `Bukkit.getServicesManager().load(PreferencesService.class)`.
2. If present, register a player-scoped `Preference<BossBar.Color>` named
   `experience-bar-color` with an enumerated codec over `BossBar.Color` (dialog
   option picker).
3. If absent, yield a null provider; log once at enable.

### Provider seam

`ExperienceBarColorProvider` takes the optional `Preference<BossBar.Color>` and
returns the player's value, or `BossBar.Color.GREEN` when the preference or
service is unavailable. `ExperienceBarFormatterImpl` already calls
`colorProvider.getColor(player)` on every render, so no formatter change is
needed beyond constructor wiring.

### Data flow

Player opens `/preferences` (owned by the Preferences plugin) → picks a color →
`PreferenceChangeEvent` → provider reads `preference.get(player)` on the next
bar render. No ModularJobs command needed.

## Error handling

- Missing service/plugin → green default, log once at enable.
- Corrupt stored value → Preferences plugin falls back to its default; provider
  also guards null → green.

## CI changes

- Update the stale "Checkout Preferences API dependency" step: repo
  `aincraft-org/preferences`, ref `c18236c1fa844eb0ae26824e524ae4605a9b41df`.
  Change the publish command from the old `:api:publishToMavenLocal` to
  `:preferences-api:publishToMavenLocal -PbuildVersion=0.2.0` with the isolated
  `-Dmaven.repo.local`.
- Same update in `.github/workflows/nightly.yml`.

## Testing

- Unit test `ExperienceBarColorProvider` with a mock `Preference<BossBar.Color>`:
  returns the player's value; null/absent preference → green.
- Wiring test asserting registration happens when the service is present
  (MockBukkit service registration).

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-25 | Player-scoped XP bar color preference | Matches "different color preferences" and existing per-player pattern |
| 2026-08-25 | `compileOnly` + soft-depend, green fallback | Preserves distribution-hardening stance: Preferences stays optional |
| 2026-08-25 | Enumerated `BossBar.Color` codec | Native option-picker dialog; no custom codec needed |
| 2026-08-25 | Pinned checkout + isolated `maven.repo.local` | Reproducible in clean CI; matches craftux/mint/databag pattern |
