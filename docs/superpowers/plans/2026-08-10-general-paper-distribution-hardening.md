# General Paper Distribution Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a generally distributable Paper release that no longer requires Mint or Preferences to compile, starts safely without an economy plugin, removes owner/server-specific defaults, preserves generic starter content, and documents the remaining Craftux dependency honestly.

**Architecture:** Keep the current manual composition root and Paper-only runtime. Replace direct Mint API references with a reflective adapter that resolves the optional Bukkit service and constructs Mint ledger requests without Mint classes on the compile classpath. Select a blackhole provider by default when Mint is unavailable, with an explicit fail policy for operators who require currency. Use the existing local preferences service unconditionally. Leave Craftux-backed UI and its repository/build path unchanged as an explicit deferred boundary.

**Tech Stack:** Java 21/25, Gradle Kotlin DSL, Paper 26.2, JUnit 5, MockBukkit, PostgreSQL-backed tests, Astro/Starlight, YAML/JSON/CSV resources, GitHub Actions.

## Global Constraints

- Target Paper only; do not add Folia or Spigot compatibility in this pass.
- Craftux remains mandatory and unchanged: keep `libs.craftux.paper`, relocation, local repository wiring, CI checkout/publication, and existing Craftux UI classes.
- Mint and Preferences must have zero compile-time or required CI-resolution surface after the changes. No `dev.jlo.mint.*` or `dev.jlo.preferences.*` imports, coordinates, private checkout steps, or private repository credentials may remain in active build paths. Reflection class-name strings for Mint are allowed only inside the optional adapter.
- Preserve namespaced ModularJobs keys and existing starter job/task/boost/upgrade schemas. Do not redesign the content model.
- `economy.required: true` remains compatible and means fail-fast unless an explicit `economy.missing-provider` value overrides it.
- Default missing-economy behavior is a warning-on-selection blackhole, never a reward-time exception or per-reward warning.
- Editor defaults are disabled with empty external URLs; explicit operator configuration remains supported.
- Mark these dated design/plan documents as historical when they mention obsolete paths/providers: `docs/superpowers/plans/2026-08-05-job-skill-tree.md`, `docs/superpowers/plans/2026-08-06-module-layout.md`, `docs/superpowers/plans/2026-08-10-modularjobs-azoth-integration.md`, `docs/superpowers/specs/2026-08-06-module-layout-design.md`, and `docs/superpowers/specs/2026-08-10-modularjobs-azoth-integration-design.md`.
- Every production behavior change ships with focused tests in the same logical commit. Run formatters/linters/full verification only after all implementation commits are complete.

## Implementation Tasks

### 1. Make economy integration optional at compile and runtime

**Files:**
- `paper/build.gradle.kts`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `.github/workflows/ci.yml`
- `paper/src/main/java/net/aincraft/payable/EconomyProviderFactory.java`
- `paper/src/main/java/net/aincraft/payable/MintEconomyProvider.java`
- `paper/src/main/java/net/aincraft/payable/BlackholeEconomyProvider.java` (new)
- `paper/src/main/java/net/aincraft/payable/PayableWiring.java`
- `paper/src/main/resources/config.yml`
- `paper/src/test/java/net/aincraft/payable/EconomyProviderFactoryTest.java`
- focused new/updated tests for `BlackholeEconomyProvider` and reflective absence

**Changes:**

- Remove all Mint imports and Mint-typed fields/signatures from `MintEconomyProvider`.
- Preserve the existing ledger semantics in a reflection-only implementation: check the Bukkit plugin manager, load the Mint service class by name, obtain its registered provider from Bukkit services, verify the reflected state is `READY`, construct the existing namespace/actor/client/currency/account/idempotency/money/request values, invoke `client(...).ledger().issue(...)`, await the returned `CompletionStage` for five seconds, and treat committed/rejected/timeout/invocation failures exactly as the current adapter’s boolean/logging contract requires.
- Isolate reflection names and method invocation in private helpers with cached class/method metadata. Catch class/linkage/reflection failures as provider unavailability; never load Mint classes during base plugin class initialization.
- Keep `isCurrencySupported()` true for the reflective Mint adapter and make invalid/nonpositive amounts return false before any reflection or service call.
- Add `BlackholeEconomyProvider` implementing `EconomyProvider`: report currency support, return false for null/nonpositive amounts, return true for positive amounts without changing balances, and log one clear warning when the provider is selected. Do not include player IDs, session secrets, or other sensitive data in the warning.
- Change `EconomyProviderFactory` to select a ready Mint adapter first, then `blackhole` by default or throw an actionable `IllegalStateException` for `fail`. Parse `economy.missing-provider` case-insensitively; map legacy `economy.required: true` to `fail` only when the new key is absent. Reject unknown policy values with a configuration error rather than silently choosing a policy.
- Return a non-null provider for the default blackhole path so economy payables remain valid. Update `PayableWiring.economyHandlerFor` documentation and remove the old null-provider/Mint-only exception path; the handler delegates to the selected provider and preserves the boolean result contract already used by the payable pipeline.
- Remove Mint version/library entries and the Mint repository from the Gradle configuration. Remove the Mint checkout, token requirement, link, publication, and artifact assertion from CI while retaining Craftux checkout/publication and verification. The resulting Paper compile path must not resolve any Mint coordinate.
- Replace factory tests that expect null/no-provider exceptions with tests for blackhole selection, explicit fail behavior, legacy required compatibility, invalid policy handling, positive/invalid blackhole amounts, and handler delegation. Keep the absence test runnable with no Mint classes on the test classpath.

**Focused verification:**

```text
./gradlew :paper:compileJava :paper:test --tests net.aincraft.payable.EconomyProviderFactoryTest --no-daemon
```

**Commit:** `feat: make economy integration optional`

### 2. Remove the external Preferences dependency and adapter

**Files:**
- `paper/src/main/java/net/aincraft/PluginContext.java`
- `paper/src/main/java/net/aincraft/service/PreferencesIntegration.java` (delete)
- `paper/src/main/java/net/aincraft/service/ExternalBackedPreferencesService.java` (delete)
- `paper/src/main/java/net/aincraft/service/PreferencesServiceImpl.java`
- `paper/src/test/java/net/aincraft/service/PreferencesServiceImplTest.java`
- `paper/src/test/java/net/aincraft/BootstrapLifecycleTest.java`
- `paper/build.gradle.kts`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `.github/workflows/ci.yml`

**Changes:**

- Construct `new PreferencesServiceImpl(plugin)` directly in `PluginContext`; remove the external cleanup callback from the resource lifecycle.
- Delete the external adapter and integration tests plus `PreferencesIntegrationTest.java`; retain `PreferencesServiceImplTest.java` as the local config/defaults and round-trip coverage.
- Remove Preferences compile/test dependencies, version/library catalog entries, GitHub Packages repository, stale Preferences comments, and all CI checkout/publication/token-resolution steps. Keep Craftux’s local repository and artifact verification intact.
- Update bootstrap lifecycle assertions so they verify direct `new PreferencesServiceImpl(plugin)` wiring and no external registration requirement rather than searching for the removed integration.

**Focused verification:**

```text
./gradlew :paper:compileJava :paper:test --tests net.aincraft.service.PreferencesServiceImplTest --tests net.aincraft.BootstrapLifecycleTest --no-daemon
```

**Commit:** `refactor: use local preferences service`

### 3. Make editor defaults opt-in and owner-neutral

**Files:**
- `paper/src/main/java/net/aincraft/editor/EditorConfig.java`
- `paper/src/main/resources/config.yml`
- `paper/src/test/java/net/aincraft/editor/EditorConfigTest.java` (new)
- `paper/src/test/java/net/aincraft/editor/EditorSessionStoreTest.java`
- `paper/src/main/resources/plugin.yml`

**Changes:**

- Set `EditorConfig` default URLs to empty strings and `enabled` to false. Preserve explicit configured URLs and existing TTL validation.
- Set matching YAML defaults and explain that enabling the editor requires an operator-managed REST session API, web editor URL, and create secret.
- Keep conditional command registration in `PluginContext`; verify editor commands are absent under defaults and remain available with explicit enabled configuration.
- Remove `Preferences` from `plugin.yml` soft dependencies. Keep Mint listed as an optional soft dependency for service startup ordering/discovery, and replace the personal author value with neutral ModularJobs contributor metadata.

**Focused verification:**

```text
./gradlew :paper:test --tests net.aincraft.editor.EditorConfigTest --tests net.aincraft.editor.EditorSessionStoreTest --no-daemon
```

**Commit:** `fix: make editor integration opt in`

### 4. Normalize bundled starter content and public/runtime branding

**Files:**
- `paper/src/main/resources/jobs.yml`
- `paper/src/main/resources/job_tasks.yml`
- `paper/src/main/resources/job_tasks.csv`
- `paper/src/main/resources/fisherman.yml`
- `paper/src/main/resources/boost_sources_default.json`
- `paper/src/main/resources/upgrade_trees/miner.json`
- `paper/src/main/resources/exploit-config.yml`
- `paper/src/main/resources/plugin.yml`
- `paper/src/main/java/net/aincraft/payment/CraftRecipeGateListener.java`
- `paper/src/main/java/net/aincraft/payment/ExploitProtectionSettings.java`
- `paper/src/main/java/net/aincraft/payment/ExploitService.java`
- `paper/src/main/java/net/aincraft/payment/HopperPayDisableStore.java`
- `paper/src/main/java/net/aincraft/payment/HopperPayListener.java`
- `paper/src/main/java/net/aincraft/payment/JobPaymentListener.java`
- `paper/src/main/java/net/aincraft/payment/MemoryExploitProtectionStoreImpl.java`
- `paper/src/main/java/net/aincraft/payment/MobDamageTrackerController.java`
- `paper/src/main/java/net/aincraft/payment/OreGeneratorProtectionListener.java`
- `paper/src/main/java/net/aincraft/payment/PistonProtectionListener.java`
- `paper/src/main/java/net/aincraft/profession/ProfessionCatalog.java`
- `api/src/main/java/net/aincraft/Bridge.java`
- `api/src/main/java/net/aincraft/profession/ProfessionCategory.java`
- `api/src/main/java/net/aincraft/profession/ProfessionDefinition.java`
- `api/src/main/java/net/aincraft/profession/RecipeDefinition.java`
- `api/src/main/java/net/aincraft/service/BuffService.java`
- `api/src/main/java/net/aincraft/service/NodeHarvestService.java`
- `api/src/main/java/net/aincraft/service/ProfessionService.java`
- `api/src/main/java/net/aincraft/service/RecipeService.java`
- `api/src/main/java/net/aincraft/service/StationService.java`
- `README.md`
- `docs/living-specs/modularjobs.md`
- `docs/living-specs/payables-economy.md`

**Changes:**

- Preserve generic starter job/task/boost/upgrade records and stable ModularJobs aliases; remove AzothMC roadmap language, server-specific comments, and `Jobs Reborn` references.
- Add concise operator-facing starter-content wording so shipped YAML/JSON/CSV values are understood as editable examples, not a fixed progression contract.
- Replace public API roadmap/product comments with capability-oriented descriptions while keeping interfaces, namespaced keys, and behavior unchanged.
- Update README and living specs to describe optional Mint/blackhole economy behavior, local preferences, PostgreSQL-only persistence, opt-in editor, and the intentionally deferred Craftux dependency. Remove claims that Mint is mandatory or the integration cutover is pending.
- Keep current project identity and compatibility names unless they identify an external server or owner-hosted service; neutralize only the server-specific/owner-specific metadata in shipped runtime content.

**Verification:** scan tracked source/resources/docs for `AzothMC`, `Jobs Reborn`, owner-hosted editor URLs, and obsolete Mint-required claims; inspect changed YAML/JSON/CSV syntax with the existing build.

**Commit:** `docs: neutralize starter content and branding`

### 5. Remove tracked IDE/private build artifacts

**Files:**
- `.gitignore`
- all currently tracked `.idea/**` files (delete)

**Changes:**

- Add `.idea/` to the root ignore rules.
- Remove tracked IntelliJ metadata, obsolete module references, local Windows paths, local SQLite data sources, and personal database mappings. Do not remove unrelated user files or modify the out-of-scope Azoth checkout.

**Verification:** confirm no tracked `.idea` paths or personal absolute paths remain.

**Commit:** `chore: remove tracked IDE metadata`

### 6. Replace stale web starter pages and operator claims

**Files:**
- `web/README.md`
- `web/astro.config.mjs`
- `web/src/components/Body.astro`
- `web/src/content/docs/wiki/index.mdx`
- `web/src/content/docs/wiki/guides/example.md` (delete)
- `web/src/content/docs/wiki/guides/operations.md` (new)
- `web/src/content/docs/wiki/reference/example.md` (delete)
- `web/src/content/docs/wiki/reference/configuration.md` (new)
- `docs/superpowers/plans/2026-08-05-job-skill-tree.md`
- `docs/superpowers/plans/2026-08-06-module-layout.md`
- `docs/superpowers/plans/2026-08-10-modularjobs-azoth-integration.md`
- `docs/superpowers/specs/2026-08-06-module-layout-design.md`
- `docs/superpowers/specs/2026-08-10-modularjobs-azoth-integration-design.md`

**Changes:**

- Replace the Astro/Starlight starter README with ModularJobs-specific development and documentation instructions.
- Remove the two example pages and replace the wiki landing page/navigation with concise real ModularJobs overview and operator/reference guidance. Do not retain astronaut/template copy or wrong package/command examples.
- Rewrite `Body.astro` claims to match the current project: Paper jobs/tasks, PostgreSQL persistence, optional Mint economy with blackhole/fail policy, optional supported integrations, and the Craftux-backed UI dependency.
- Correct the web navigation to point at real ModularJobs pages. Remove stale MySQL/SQLite/Vault/Treasury/Service-IO/MythicMobs claims from the landing page and docs; do not remove the separate deprecated Vue/Bytebin demo in this pass because it is not the production editor path.
- Add an archival status note to each dated plan/spec listed above; leave historical technical content intact rather than presenting it as current setup guidance.

**Focused verification:**

```text
(cd web && npm run build)
(cd web/session-editor && npm test && npm run build)
(cd web/rest-api && cargo test) # requires the configured PostgreSQL test service
```

**Commit:** `docs: refresh ModularJobs web documentation`

## Final Verification

Run from the ModularJobs root after all commits:

```text
./gradlew :api:test :common:test :paper:test --no-daemon
./gradlew :paper:build --no-daemon
(cd web && npm run build)
(cd web/session-editor && npm test && npm run build)
(cd web/rest-api && cargo test) # requires the configured PostgreSQL test service
```

Then inspect the release artifact and tracked tree:

```text
jar tf paper/build/libs/paper-all.jar
```

Confirm the artifact contains Craftux relocation/classes as the deferred dependency, contains no Mint or Preferences API classes, and that generated defaults show disabled editor, empty owner URLs, and blackhole economy policy. Search tracked files for private Mint/Preferences repositories, `dev.jlo.mint`, `dev.jlo.preferences`, personal absolute paths, starter-template copy, AzothMC/Jobs Reborn references, stale database claims, and owner-hosted endpoint defaults. Confirm no files outside the ModularJobs worktree were changed.
