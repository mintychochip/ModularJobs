# Design: Server-development skill alignment

**Date:** 2026-08-21
**Status:** Approved for implementation
**Living spec:** `docs/living-specs/modularjobs.md` (quality / toolchain); `docs/living-specs/persistence.md` (Hikari only, not schema ownership)

## Problem

ModularJobs is a Paper 26.2 plugin with quality tools and CI, but it does not
follow the 2026-08-21 `server-development-skills` contract (`project-setup`,
`ci-release`, parts of `database-integration` and `docs-maintenance`). Pins are
stale, `check` is report-only, `plugin.yml` still declares `api-version: '1.21'`,
there is no rolling nightly, and docs have no executable gate.

A literal reading of those skills also fights this repo's documented policy:
MySQL 8 connect-only schema (no in-process DDL, no SQLite) and
`YY.M.D.REVISION` versioning. Those policies stay.

## Goal

Align the repo with the skills **except where AGENTS.md / living specs win**.
Land the work in two waves so Wave 1 can stay green without a Google Java Style
reformat of every Java file.

## Invariants (do not change)

- MySQL 8 only. Plugin and REST API never `CREATE TABLE`. `SchemaPolicy` stays
  connect-only; ops apply `paper/src/main/resources/sql/mysql.sql`.
- Release version remains `YY.M.D.REVISION` (`26.8.11.1` today), override
  `-PreleaseVersion=`. Do not switch to skill CalVer
  `YYYY.MM.DD.<GITHUB_RUN_NUMBER>`.
- `paper-api` stays pinned at `26.2.build.65-beta` (skill allows an exact pin).
- Module layout `api` / `common` / `paper` and package `dev.mintychochip` stay.
- Do not add Azalea, do not rename packages to `io.github.*`, do not delete
  `web/fumadocs`.

## Wave 1 — pins, descriptor, CI, nightly, Hikari

Mechanical alignment. Quality analyzers still report-only (`-Pquality.fail` not
the default). No Spotless. No `google_checks.xml`. No docs rewrite.

### Toolchain pins (`gradle/libs.versions.toml`, wrapper)

| Component | From | To |
|---|---|---|
| Gradle wrapper | 9.6.1 | **9.7.1** |
| run-paper | 3.0.2 | **3.1.0** |
| Checkstyle tool | 13.9.0 | **13.11.0** (keep `config/checkstyle/checkstyle.xml`) |
| SpotBugs engine | 4.10.3 | **4.9.7** |
| HikariCP | 5.0.1 | **7.0.2** |
| Java toolchain | paper 25, api/common 21 | **25** for all Java modules |

`pmd` stays 7.26.0. SpotBugs Gradle plugin stays 6.5.10.

Regenerate the wrapper with `./gradlew wrapper --gradle-version 9.7.1` and
commit `gradle/wrapper/**` plus `gradlew*`.

### Plugin descriptor

`paper/src/main/resources/plugin.yml`: `api-version: '26.2'` (quoted). Version
expansion from Gradle is already correct.

### Hikari (MySQL policy preserved)

`HikariConfigProvider` always sets, for MySQL URLs:

```text
cachePrepStmts=true
prepStmtCacheSize=250
```

Do not add SQLite, pool-size-1, or startup `CREATE TABLE`. Close-on-disable
already exists.

### CI (`.github/workflows/ci.yml`)

Keep the existing jobs (sibling checkouts, MySQL schema apply, Rust, React).
Change the GitHub Action majors and the Java gate:

| Component | To |
|---|---|
| `actions/checkout` | **v7** |
| `actions/setup-java` | **v5**, Temurin **25** (drop the extra JDK 21 setup) |
| `gradle/actions/setup-gradle` | **v6** |

- Workflow `permissions.contents`: **read** (not write). `packages: write` only
  on jobs that publish packages, if any still do.
- Java job runs `./gradlew clean check` (not `check` alone). Wave 1 still omits
  `-Pquality.fail=true`.
- `CiTemplateHookupTest` must still pass: no reusable private `ci-template`;
  jobs that create GitHub releases must be schedule/manual-only.

### Nightly (new `.github/workflows/nightly.yml`)

- Triggers: `schedule` cron `0 4 * * *` and `workflow_dispatch` only.
- `permissions.contents: write` on this workflow only.
- Reuse the Java job's sibling checkout + local Maven publish steps (craftux,
  mint, databag, conditions, Preferences). Nightly cannot be the skill's
  three-step toy workflow; those artifacts are not on Maven Central.
- Gate: `./gradlew clean check` then `./gradlew :paper:shadowJar`.
- Publish rolling pre-release tag `nightly` from
  `paper/build/libs/*-all.jar` (delete tag/release first, then create).
- Do not use skill CalVer for the artifact version. Nightly keeps the Gradle
  version already in source (`26.8.11.1` until the next `-PreleaseVersion`).

### Public README

Repo `aincraft-org/modularjobs` is public. Default branch is **master**.

- Add MIT `LICENSE` (POM already claims MIT; there is no file).
- README badges: build status for `ci.yml` on `master`, license, latest
  release, Paper 26.2 platform badge. Shields paths use the workflow
  **filename**.

### Tests / docs touched in Wave 1

- Any test or doc that asserts `api-version: '1.21'` updates to `'26.2'`.
- AGENTS.md / README build notes: Gradle 9.7.1, Java 25 for all modules,
  `api-version` 26.2, nightly workflow exists.
- Do not rewrite `scripts/validate-release-version.sh`.

## Wave 2 — Google style gate + docs gate

Depends on Wave 1 being green.

### Formatter and analyzers

- Add Spotless **8.10.0** with `googleJavaFormat("1.36.1")` on Java sources.
- Checkstyle loads the pinned URL:
  `https://raw.githubusercontent.com/checkstyle/checkstyle/checkstyle-13.11.0/src/main/resources/google_checks.xml`
- `ignoreFailures` default **false** for Checkstyle, PMD, SpotBugs (remove the
  report-only default; `-Pquality.fail` becomes unnecessary).
- `tasks.named("check")` depends on Checkstyle, PMD, and SpotBugs task types,
  plus Spotless check.
- Local: `./gradlew spotlessApply` once, then fix remaining Checkstyle/PMD/
  SpotBugs findings until `./gradlew clean check` is green. CI Wave 2 runs
  that same command.
- Pre-commit may keep compiling/testing; it does not have to run the full
  quality gate (slow). CI is the fail-closed gate.

### Docs maintenance (adapted)

Skill wants repo-root `content/docs/` and a hub elsewhere. This repo already
publishes from `web/fumadocs`. Do **not** add a second tree at repo root and
do **not** scaffold another Next app.

- Copy `docs-maintenance/references/verify-docs.mjs` to
  `web/fumadocs/scripts/verify-docs.mjs` and wire `npm test` there.
- Pin hub packages to the skill table: fumadocs-core **16.14.5**, fumadocs-mdx
  **15.3.0**, fumadocs-ui alias `@fumadocs/base-ui@16.14.5`, next **16.3.2**.
- Fix `web/fumadocs/app/global.css` import order: `tailwindcss`, then
  `neutral.css`, then `preset.css`.
- Add explicit **basics / Everyday / Advanced** labels starting with
  `index.mdx` and `guides/getting-started.mdx`. Run `node scripts/verify-docs.mjs`
  and fix every page the gate reports; do not invent behaviour the source
  does not support.
- Keep MySQL-only operator guidance. Do not document SQLite or in-process DDL.

## Out of scope (both waves)

- CalVer `YYYY.MM.DD.<run_number>` and changing `validate-release-version.sh`
- In-process schema migrations / SQLite / Flyway
- Azalea autonomous testing
- Package rename, `rootProject.name` (`jobs2`), module DAG rewrite
  (`paper` → `api` → `common`)
- Deleting Astro `web/` or the Fumadocs app
- Spark profiling / performance rewrite
- Making pre-commit run `clean check` (too slow)

## Verification

Wave 1 is done when:

```bash
./gradlew wrapper --gradle-version 9.7.1   # already committed
./gradlew :api:test :common:test :paper:test
./gradlew clean check                      # analyzers report-only, compile/tests pass
```

`plugin.yml` packaged resource has `api-version: '26.2'`. CI YAML uses v7/v5/v6
and `clean check`. `nightly.yml` exists and is schedule/manual only.

Wave 2 is done when:

```bash
./gradlew clean check                      # FAILS on Spotless/Checkstyle/PMD/SpotBugs
cd web/fumadocs && npm test && npm run build
```

## Decisions

| Date | Decision | Why |
|---|---|---|
| 2026-08-21 | Keep MySQL connect-only and `YY.M.D.REVISION` | AGENTS.md / living specs outrank skill templates |
| 2026-08-21 | Two waves | Spotless + Google Checks rewrite most Java; must not block pin/CI landing |
| 2026-08-21 | Keep `paper-api` 26.2.build.65-beta | Skill allows exact pin; MockBukkit line is already on this build |
| 2026-08-21 | Nightly reuses CI sibling checkouts | craftux/mint/databag/conditions are not Central |
| 2026-08-21 | Docs gate against `web/fumadocs` | Existing hub; no second content tree |
