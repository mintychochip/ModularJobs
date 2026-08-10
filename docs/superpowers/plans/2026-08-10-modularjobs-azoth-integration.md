# ModularJobs 2.0.0 and Azoth integration implementation plan

> **For agentic workers:** Execute this plan task-by-task in the current session. Preserve unrelated dirty work in both repositories. Run focused checks after each production change and the complete cross-repository checks at the end.

**Goal:** Release ModularJobs 2.0.0 as the profession/progression provider and move all gathering level enforcement into Azoth, including block breaking, fishing, log stripping, and mature plant harvesting.

**Architecture:** ModularJobs publishes Java 21-compatible `api` and `common` artifacts and always exposes `ProfessionService` through Bukkit's service manager. Azoth declares ModularJobs as a required, classpath-joined Paper dependency, resolves `ProfessionService` during enable, loads its own gathering gate configuration, and registers NORMAL-priority listeners that cancel under-level events before ModularJobs MONITOR payment listeners. Azoth's combat-level service is untouched.

**Tech Stack:** Java 21 API/Azoth, Java 25 ModularJobs Paper implementation, Gradle Kotlin DSL, Paper 26.2/1.21.11 APIs, JUnit 5, Mockito for Bukkit-free listener tests, Maven Publish, shell release packaging.

## Global constraints

- Use version `2.0.0` everywhere in release metadata, coordinates, plugin descriptors, docs, and checks.
- Do not shade ModularJobs into Azoth; use `compileOnly` and `join-classpath: true`.
- Keep the user's unrelated dirty changes in both repositories. Stage only files belonging to each logical commit.
- Do not introduce a database schema change or make ModularJobs depend on Azoth.
- Do not migrate or reinterpret Azoth `CombatLevelService` checks.
- Gate listeners ignore already-cancelled events, honor bypass permissions, treat missing profession levels as insufficient, and never let denied events reach payment.

---

## 1. Publish the ModularJobs API and prepare the 2.0.0 release

**Files:**

- `modularjobs/build.gradle.kts`
- `modularjobs/api/build.gradle.kts`
- `modularjobs/common/build.gradle.kts`
- `modularjobs/paper/src/main/resources/plugin.yml`
- `modularjobs/scripts/package-release-assets.sh`
- `modularjobs/scripts/test-package-release-assets.sh`
- `modularjobs/.github/workflows/ci.yml`
- `modularjobs/CHANGELOG.md`, `modularjobs/README.md`

**Changes:**

1. Set the root version and plugin descriptor to `2.0.0`.
2. Configure `api` and `common` to compile with Java 21 while leaving `paper` on Java 25.
3. Apply `maven-publish` to `api` and `common`, publish `org.aincraft:modularjobs-api:2.0.0` and `org.aincraft:modularjobs-common:2.0.0`, include source/javadoc jars, and expose a local `build/maven-repo` repository for Azoth development. Keep optional GitHub Packages publishing credential-gated.
4. Port the existing immutable release asset script/tests and update them for `2.0.0`; the script must verify the embedded `plugin.yml` version and produce the JAR, PostgreSQL schema, and `SHA256SUMS`.
5. Update CI's release job to trigger only for `v2.0.0`, package the exact tag's `*-all.jar`, publish the three assets, and re-download/checksum-verify them.
6. Update release documentation/changelog without claiming a remote release until the tag/publish is actually observed.

**Checks:** `./gradlew :api:test :common:test :paper:test`; `./gradlew :api:publishMavenPublicationToLocalBuildRepo :common:publishMavenPublicationToLocalBuildRepo`; package script test and a clean `2.0.0` asset package.

## 2. Make ProfessionService the stable ModularJobs integration point

**Files:**

- `modularjobs/paper/src/main/java/net/aincraft/ModularJobsBootstrap.java`
- `modularjobs/paper/src/main/java/net/aincraft/PluginContext.java`
- `modularjobs/paper/src/main/resources/config.yml`
- `modularjobs/paper/src/main/resources/plugin.yml`
- ModularJobs gate listener/store/loader classes and their tests
- `modularjobs/paper/src/main/java/net/aincraft/payment/JobPaymentListener.java`
- `modularjobs/paper/src/main/resources/job_tasks.csv`
- `modularjobs/scripts/add-herbalism-tasks.sql`
- relevant ModularJobs docs/living specs

**Changes:**

1. Register `ProfessionService` unconditionally at normal service priority. Leave Recipe/Buff/Station/NodeHarvest registration behind the existing optional configuration flag.
2. Remove ModularJobs-owned gathering enforcement and its wiring: block-break/fish listeners, their Paper stores/loaders, gate-only API contracts, gate-only permissions, and gate configuration sections. Keep the progression API, payment listeners, and gathering task data.
3. Port the approved Herbalism task rows and migration script from the existing gathering branch; keep SQL connect-only and do not execute it from plugin startup.
4. Ensure payment predicates still pay only supported mature plant harvests/log strips and continue to use `ignoreCancelled=true`. Keep behavior aligned with Azoth predicates through tests/documented semantics rather than a Paper-dependent API coupling.
5. Document that Azoth owns enforcement and that ModularJobs alone does not block interactions.

**Checks:** focused API and payment tests; full ModularJobs test suite; inspect plugin service registration and resource configuration in the built artifact.

## 3. Add the ModularJobs dependency to Azoth

**Files:**

- `azoth/build.gradle.kts`
- `azoth/paper/build.gradle.kts`
- `azoth/paper/src/main/resources/paper-plugin.yml`
- `azoth/paper/src/test/java/dev/jlo/azoth/paper/PluginDescriptorTest.java`

**Changes:**

1. Add the sibling ModularJobs local Maven repository and credential-gated remote repository resolution. Add `compileOnly("org.aincraft:modularjobs-api:2.0.0")` and test visibility without embedding the API.
2. Update resource expansion to process `paper-plugin.yml`, preserving the user's descriptor migration.
3. Declare:
   ```yaml
   dependencies:
     server:
       ModularJobs:
         load: BEFORE
         required: true
         join-classpath: true
   ```
4. Update descriptor tests to inspect `paper-plugin.yml` and assert the required dependency metadata.

**Checks:** publish ModularJobs API/common locally, then `../azoth/./gradlew :paper:test` and `../azoth/./gradlew :paper:build`.

## 4. Implement Azoth-owned gathering gates

**Files under `azoth/paper/src/main/java/dev/jlo/azoth/paper/gathering/`:**

- `GatheringGate.java`
- `GatheringGateStore.java`
- `GatheringGateAction.java`
- `GatheringInteractionPredicates.java`
- `YamlBlockBreakGateLoader.java`
- `YamlFishCatchGateLoader.java`
- `YamlInteractionGateLoader.java`
- `GatheringGateListener.java`

Also update:

- `azoth/paper/src/main/java/dev/jlo/azoth/paper/AzothPlugin.java`
- `azoth/paper/src/main/resources/config.yml`

**Changes:**

1. Define a small internal gate record keyed by action/material, canonical profession ID, and positive minimum level. Store block, fish, and interaction gates in immutable maps.
2. Load `block-break-gates`, `fish-catch-gates`, and `interaction-gates` from Azoth config. Validate Bukkit materials, positive integer levels, and profession IDs through `ProfessionService.resolve`; warn and skip invalid entries.
3. Classify only these interactions:
   - `BlockBreakEvent` by block material;
   - `PlayerFishEvent` only in `CAUGHT_FISH` state and only cod/salmon/tropical_fish/pufferfish caught items;
   - `PlayerInteractEvent` RIGHT_CLICK_BLOCK with an axe on an unstripped log/stem/wood/hyphae, mature sweet-berry bush, fully grown cocoa, or berry-bearing cave vines/cave-vines plant.
4. Register NORMAL, `ignoreCancelled=true` handlers. For a configured gate, accept exact/above levels; cancel below-level and missing-level events, deny both interaction results, and send the configured denial message. Skip players with the documented Azoth bypass permissions.
5. Resolve the ModularJobs service before constructing listeners. If the required service is unavailable, log a clear error and disable Azoth. Unregister listeners/services on disable. Do not touch the combat service.
6. Copy the approved tier values into Azoth's config, including mining, woodcutting, farming, herbalism, fishing, strip-log, and plant-harvest gates.

**Checks:** focused listener/loader/predicate tests for below/exact/above/missing/cancelled/bypass boundaries and all four interaction families; verify an under-level event is cancelled before a MONITOR listener can observe payment.

## 5. Add cross-repository verification and finish release

**Files:** test/build resources only where required by observed failures.

**Changes:**

1. Run ModularJobs API/common/paper tests and release packaging checks.
2. Publish ModularJobs API/common to the local build repository and build/test Azoth against exactly `2.0.0`.
3. Run a smoke scenario using the gate listeners and a mocked `ProfessionService`: each configured gate denies below/missing levels and allows exact/above levels; cancelled events remain ignored; bypass permissions remain allowed.
4. Review both worktrees' status/diffs, stage only owned files, and make atomic commits: release/publication, ModularJobs ownership cutover, Azoth dependency, Azoth gates, and release automation/docs as separate logical units when files do not overlap.
5. Create local tag `v2.0.0` only after all checks pass. Do not claim remote publication without observed push/CI output.
