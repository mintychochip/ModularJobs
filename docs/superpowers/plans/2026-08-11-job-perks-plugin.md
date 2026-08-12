# ModularJobs Perks Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a separate `perks` Paper plugin that contributes 19 safe job-perk trees to ModularJobs and renders them through its service-authoritative MapGUI upgrade graph.

**Architecture:** ModularJobs exposes an API-only transactional extension service and owns one immutable upgrade snapshot, generation-aware purchases, persistence, capability reconciliation, and MapGUI. The Java 25 `perks` module installs complete replacement trees plus executable handlers through the shared API class identity. PostgreSQL remains connect-only.

**Tech Stack:** Java 21 API, Java 25 Paper/perks, Gradle 9.6.1, Paper 26.2, MapGUI API 1.0.0, Adventure, PostgreSQL, JUnit 5, MockBukkit 26.2.

## Global Constraints

- Preserve unrelated working-tree changes; stage only each task's named paths.
- `api` remains Paper-free/Java 21; `paper` and `perks` use Java 25.
- `perks` must not depend on `:paper`, shade ModularJobs API/common, or import MapGUI.
- Never execute DDL at runtime; update `postgres.sql` and apply with `scripts/apply-postgres-schema.sh`.
- Publish complete immutable snapshots; never mutate loaded `UpgradeTree`/`SkillTree` objects.
- Every purchase, reset, listener, restore, boost, and GUI path captures one snapshot generation.
- Unknown persisted node IDs block calculation/purchase/reset and are never filtered or refunded.
- Bukkit world/entity/block access is main-thread-only; only pure immutable-snapshot computation is asynchronous.
- Stable node IDs are permanent; version one has no rename/removal migration.

---

## Phase A — Extension API, snapshots, and persistence

### Task 1: Add Paper-free provider contracts

**Files:**
- Create: `api/src/main/java/net/aincraft/upgrade/extension/{UpgradeExtensionService,UpgradeContribution,TreeContribution,CapabilityHandler,CapabilityContext,CapabilityValidation,CapabilityResult,UpgradeRegistrationResult}.java`
- Test: `api/src/test/java/net/aincraft/upgrade/extension/UpgradeExtensionContractsTest.java`
- Modify: `api/src/test/java/net/aincraft/ArchitectureIsolationTest.java`

**Interfaces:** Produces the exact API boundary from design §5.1; consumes `SkillTree`, Adventure `Key`/`Component`, and `UUID` only.

- [ ] Write failing defensive-copy/sealed-result tests. Example:

```java
var input = new HashMap<String, TreeContribution>();
var c = new UpgradeContribution(Key.key("modularjobs-perks", "provider"),
        "ModularJobsPerks", 1, input, Map.of());
input.put("miner", null);
assertTrue(c.trees().isEmpty());
assertThrows(UnsupportedOperationException.class,
        () -> c.trees().put("builder", null));
```

- [ ] Extend architecture isolation to reject `org.bukkit`, `io.papermc`, and `de.flog99.mapgui` references under `api`.
- [ ] Run `./gradlew :api:test --tests 'net.aincraft.upgrade.extension.*' --tests net.aincraft.ArchitectureIsolationTest`; expect compilation failure because contracts are absent.
- [ ] Implement immutable records/interfaces and all typed result variants from the spec using `Map.copyOf`/`Set.copyOf` and null validation.
- [ ] Rerun the focused command; expect PASS on Java 21.
- [ ] Commit: `feat(api): add upgrade extension contracts` with only these files.

### Task 2: Add the capability effect envelope

**Files:**
- Modify: `api/src/main/java/net/aincraft/upgrade/NodeEffect.java`
- Modify: `paper/src/main/java/net/aincraft/upgrade/config/SkillTreeEffectParser.java`
- Modify: temporary exhaustive formatting switch in `paper/src/main/java/net/aincraft/gui/UpgradeTreeGui.java`
- Test: `api/src/test/java/net/aincraft/upgrade/CapabilityEffectTest.java`
- Test: `paper/src/test/java/net/aincraft/upgrade/config/SkillTreeEffectParserTest.java`

- [ ] Test that payload mutation after construction cannot change `CapabilityEffect` and that parser accepts only `type=capability`, namespaced key, positive schema, string map.
- [ ] Run focused API/Paper tests; expect sealed permit/parser failures.
- [ ] Add `CapabilityEffect(Key,int,Map<String,String>)` with `Map.copyOf` and parser support; do not dispatch here.
- [ ] Run `./gradlew :api:test --tests '*CapabilityEffectTest' :paper:test --tests '*SkillTreeEffectParserTest' :paper:compileJava`; expect PASS.
- [ ] Commit: `feat(api): add capability upgrade effects`.

### Task 3: Introduce the canonical snapshot store

**Files:**
- Create: `paper/src/main/java/net/aincraft/upgrade/{UpgradeSnapshot,UpgradeSnapshotStore,UpgradeTreeView}.java`
- Test: `paper/src/test/java/net/aincraft/upgrade/UpgradeSnapshotStoreTest.java`

**Interfaces:** Produces `current()`, `view(job)`, and package-private validated publication.

- [ ] Test immutable maps, generation increment once, invalid build retaining object identity, server-thread publication, and concurrent readers observing entirely old or new snapshots.
- [ ] Run `./gradlew :paper:test --tests '*UpgradeSnapshotStoreTest'`; expect missing-class failure.
- [ ] Implement an `AtomicReference<UpgradeSnapshot>` plus server-thread write lock. Include trees, handlers, owner availability, disabled reasons, fingerprints, and generation.
- [ ] Rerun focused test; expect PASS with no mixed generation.
- [ ] Commit: `feat: add canonical upgrade snapshot store`.

### Task 4: Make upgrade state generation-aware and fail closed

**Files:**
- Create: `api/src/main/java/net/aincraft/upgrade/{UpgradeStateResolution,ResetResult}.java`
- Modify: `api/src/main/java/net/aincraft/upgrade/{UpgradeService,PurchaseResult}.java`
- Modify: `paper/src/main/java/net/aincraft/upgrade/UpgradeServiceImpl.java`
- Modify every caller returned by LSP references for `purchaseSkillLevel`, `purchaseMajor`, and `resetTree`
- Test: `paper/src/test/java/net/aincraft/upgrade/UpgradeUnresolvedStateTest.java`

- [ ] Run LSP references before changing exported signatures; record every caller.
- [ ] Test unknown raw node ID returns `Unresolved`, `UNRESOLVED_STATE`, preserves the raw map after reset attempt, and stale generation writes nothing.
- [ ] Run `./gradlew :paper:test --tests '*UpgradeUnresolvedStateTest'`; expect current code to ignore/drop unknown IDs.
- [ ] Implement `Resolved`/`Unresolved`, expected-generation mutation signatures, typed reset, disabled/owner unavailable outcomes, and migrate every caller. Remove old overloads.
- [ ] Run `./gradlew :api:test :paper:test --tests 'net.aincraft.upgrade.*'`; expect PASS.
- [ ] Commit: `fix: fail closed on unresolved upgrade state`.

### Task 5: Add external reconciliation schema and repository

**Files:**
- Modify: `paper/src/main/resources/sql/postgres.sql`
- Modify: `docs/database-schema.md`
- Create: `paper/src/main/java/net/aincraft/upgrade/{CapabilityReconciliationRecord,CapabilityReconciliationRepository,PostgresCapabilityReconciliationRepository}.java`
- Test: `paper/src/test/java/net/aincraft/upgrade/{ConnectOnlyCapabilitySchemaTest,PostgresCapabilityReconciliationRepositoryTest}.java`

- [ ] Test exact design §6.1 DDL in `postgres.sql`, absence of `CREATE TABLE` in Java, missing-table operator guidance, enqueue/increment/delete semantics.
- [ ] Run focused tests; expect missing schema/repository failure.
- [ ] Add DDL only to `postgres.sql` and JDBC prepared statements only to Java. Runtime must never create/migrate.
- [ ] Rerun focused tests; expect PASS.
- [ ] Commit: `feat: persist capability effect reconciliation`.

### Task 6: Install complete provider trees transactionally

**Files:**
- Create: `paper/src/main/java/net/aincraft/upgrade/{UpgradeExtensionServiceImpl,SkillTreeFingerprint,UpgradeContributionValidator}.java`
- Test: `paper/src/test/java/net/aincraft/upgrade/{UpgradeExtensionServiceImplTest,SkillTreeFingerprintTest}.java`

- [ ] Test absent bases, exact Miner base, modified cost/effect/requirement, extra ID, duplicate owner, wrong handler classloader, players online, async call, and one invalid tree in a 19-tree batch.
- [ ] Assert every rejection leaves snapshot identity/generation unchanged.
- [ ] Run focused tests; expect missing service failure.
- [ ] Implement semantic fingerprints and validate whole contribution before one `AtomicReference.set`; no raw registry writes or `.findFirst()` resolution.
- [ ] Rerun focused tests; expect PASS.
- [ ] Commit: `feat: install complete provider upgrade trees`.

### Task 7: Dispatch and reconcile capabilities

**Files:**
- Create: `paper/src/main/java/net/aincraft/upgrade/{CapabilityEffectDispatcher,CapabilityReconciliationScheduler}.java`
- Modify: `paper/src/main/java/net/aincraft/upgrade/{UpgradeEffectApplier,UpgradePermissionRestoreListener,UpgradeServiceImpl}.java`
- Test: `paper/src/test/java/net/aincraft/upgrade/{CapabilityEffectDispatcherTest,CapabilityReconciliationSchedulerTest}.java`

- [ ] Test idempotent source IDs, desired active/inactive diff, success deletion, exception→retryable, permanent diagnostics, login retry, 32-row scheduler cap, and unrelated-handler continuation.
- [ ] Run focused tests; expect missing dispatcher failure.
- [ ] Persist desired transition with state, dispatch after commit, retry on login/30-second server-thread cycle, never invoke unavailable-owner handlers.
- [ ] Rerun focused tests and permission restore tests; expect PASS.
- [ ] Commit: `feat: reconcile provider capability effects`.

### Task 8: Wire the foundation through `PluginContext`

**Files:**
- Modify: `paper/src/main/java/net/aincraft/{PluginContext,ModularJobsBootstrap}.java`
- Modify: `paper/src/main/java/net/aincraft/BridgeImpl.java` only if a read-only view is required
- Test: `paper/src/test/java/net/aincraft/UpgradeExtensionBootstrapTest.java`

- [ ] Test Bukkit service lookup and reference identity across service, upgrade service, listeners, boosts, and GUI opener.
- [ ] Run focused test; expect missing wiring.
- [ ] Build initial snapshot from loader output, inject the same store everywhere, register `UpgradeExtensionService`, and mark owner unavailable on `PluginDisableEvent` without claiming live revocation.
- [ ] Run `./gradlew :api:test :paper:test --tests 'net.aincraft.upgrade.*' --tests '*UpgradeExtensionBootstrapTest'`; expect PASS.
- [ ] Commit: `feat: publish upgrade extension service`.

**Phase A gate:** `./gradlew :api:test :paper:test --tests 'net.aincraft.upgrade.*'` passes and a synthetic provider installs one immutable generation.

---

## Phase B — Craftux-to-MapGUI clean cutover

### Task 9: Add MapGUI graph layout and opener

**Files:**
- Create: `paper/src/main/java/net/aincraft/gui/mapgui/{UpgradeTreeScreenOpener,UpgradeTreeGraphLayout,UpgradeTreeScreen}.java`
- Modify: `paper/src/main/java/net/aincraft/gui/UpgradeTreeGui.java`
- Test: `paper/src/test/java/net/aincraft/gui/mapgui/{UpgradeTreeGraphLayoutTest,UpgradeTreeScreenOpenerTest}.java`

- [ ] Test 24-pixel projection, `x=0..12/y=-4..4`, Miner cluster, edge-before-node rendering, bounded pan, hit boundaries, null positions noninteractive, and captured view generation.
- [ ] Run focused MapGUI tests; expect missing classes.
- [ ] Implement `Draw` graph and `MapGui.get().open`; drawing remains read-only.
- [ ] Rerun focused tests; expect PASS.
- [ ] Commit: `feat: render upgrade graph with MapGUI`.

### Task 10: Add detail and purchase screens

**Files:**
- Create: `paper/src/main/java/net/aincraft/gui/mapgui/{SkillNodeDetailScreen,MajorPurchaseConfirmationScreen}.java`
- Modify: `paper/src/main/java/net/aincraft/gui/mapgui/UpgradeTreeScreen.java`
- Test: `paper/src/test/java/net/aincraft/gui/mapgui/{SkillNodeDetailScreenTest,UpgradeTreePurchaseFlowTest}.java`

- [ ] Test capability descriptions without activation, disabled reason/no service call, skill call once with generation, major confirm once, cancel zero, and stale refresh.
- [ ] Run focused tests; expect failure.
- [ ] Implement keyed `Scroll` details, existing feedback mapping, and generation-aware service delegation only.
- [ ] Rerun focused tests; expect PASS.
- [ ] Commit: `feat: add MapGUI upgrade node purchase flow`.

### Task 11: Remove the Craftux upgrade route and require MapGUI

**Files:**
- Modify: `paper/src/main/java/net/aincraft/{PluginContext.java,commands/UpgradesCommand.java}`
- Modify: `paper/src/main/java/net/aincraft/gui/CraftuxUiHost.java`
- Delete obsolete upgrade-only Craftux actions after LSP reference checks
- Create: `paper/src/main/resources/paper-plugin.yml`
- Modify: `paper/build.gradle.kts`
- Test: `paper/src/test/java/net/aincraft/gui/CraftuxGuiMigrationTest.java`
- Test: `paper/src/test/java/net/aincraft/PaperPluginDescriptorTest.java`

- [ ] Update tests to require the MapGUI opener, no upgrade actions in Craftux, required MapGUI `load: BEFORE`/`join-classpath`, and no shaded MapGUI classes.
- [ ] Run focused tests; expect failure on current Craftux path.
- [ ] Migrate all LSP callers, retain Craftux for unrelated UIs, add descriptor/runServer dependency, then delete only unreferenced upgrade actions.
- [ ] Run focused tests plus `:paper:shadowJar`; expect PASS.
- [ ] Commit: `feat: require MapGUI for job upgrades`.

**Phase B gate:** focused MapGUI tests pass; `/jobs upgrade` has one MapGUI route; unrelated Craftux UIs still compile; `paper-all.jar` excludes MapGUI classes.

---

## Phase C — Separate perks plugin and content

### Task 12: Add the Java 25 `perks` module

**Files:**
- Modify: `settings.gradle.kts`, `build.gradle.kts`
- Create: `perks/build.gradle.kts`
- Create: `perks/src/main/java/net/aincraft/perks/ModularJobsPerksPlugin.java`
- Create: `perks/src/main/resources/paper-plugin.yml`
- Test: `perks/src/test/java/net/aincraft/perks/{GradleAndDescriptorTest,BootstrapLifecycleTest}.java`

- [ ] Test Java class major 69, required/joined ModularJobs dependency, no shaded API/MapGUI, one install call, missing service failure, and separate artifact name.
- [ ] Run `./gradlew :perks:test`; expect project-not-found failure.
- [ ] Add module/bootstrap/descriptor. `onEnable` validates then installs once; typed failure disables plugin; no reload path.
- [ ] Run `./gradlew :perks:test :perks:shadowJar`; expect PASS and `modularjobs-perks-all.jar`.
- [ ] Commit: `feat(perks): add standalone plugin foundation`.

### Task 13: Add exact config, provenance, and 19 tree resources

**Files:**
- Create: `perks/src/main/java/net/aincraft/perks/config/{PerksConfig,PerksConfigLoader}.java`
- Create: `perks/src/main/java/net/aincraft/perks/tree/{PerksTreeLoader,PerksTreeValidator}.java`
- Create: `perks/src/main/java/net/aincraft/perks/PerksContributionFactory.java`
- Create: `perks/src/main/resources/{config.yml,provenance.json}`
- Create: `perks/src/main/resources/trees/{builder,lumberjack,miner,farmer,hunter,fisherman,blacksmith,enchanter,alchemist,herbalism,smelting,milling,tanning,refining,cooking,armorsmithing,tailoring,engineering,artisan}.json`
- Test: `perks/src/test/resources/config/{valid,invalid,missing-hooks}.yml`
- Test: `perks/src/test/java/net/aincraft/perks/config/PerksConfigLoaderTest.java`
- Test: `perks/src/test/java/net/aincraft/perks/tree/{PerksTreeCorpusTest,ProvenanceCoverageTest,MinerReplacementSafetyTest}.java`

- [ ] Parameterize tests over exactly 19 roster IDs; assert topology, IDs, costs, gates, prerequisites, exclusions, positions, capabilities/recipes, provenance, and core Miner semantic preservation.
- [ ] Run focused config/tree tests; expect absent-loader/resource failures.
- [ ] Transcribe design §§8.1–8.5 exactly; production JSON uses parser-supported fields only; provenance remains separate.
- [ ] Run `:perks:processResources` and focused tests; expect PASS for packaged resources.
- [ ] Commit: `feat(perks): add nineteen validated perk trees`.

### Task 14: Implement bounded world and combat capabilities

**Files:**
- Create focused handlers under `perks/src/main/java/net/aincraft/perks/capability/`: `OreRadarHandler`, `AreaOperationHandler`, `FallImmunityHandler`, `TreeProcessingHandler`, `TrackingHandler`, `TrappingHandler`, `ProjectileHandler`, `SpongeHandler`, `GrapplingHookHandler`, `CapabilityRegistry`
- Create event adapters under `perks/src/main/java/net/aincraft/perks/listener/`
- Test corresponding classes under `perks/src/test/java/net/aincraft/perks/capability/`

- [ ] Test radius 1/32, 35,937 snapshot cap, no chunk loads, queue 32, two-second timeout, logout/stale cancellation, cancelled protection events, 256-work cap, privacy ranges, trap/entity allowlists, projectile terrain disabled, and sponge 5×5×5/ten uses.
- [ ] Run capability tests; expect missing-handler failures.
- [ ] Implement main-thread snapshots/listeners and one bounded executor for pure radar calculation; handlers are idempotent by source effect.
- [ ] Rerun capability tests; expect PASS.
- [ ] Commit: `feat(perks): add bounded job capability handlers`.

### Task 15: Add boosts, recipes, and remaining action handlers

**Files:**
- Create: `perks/src/main/java/net/aincraft/perks/boost/PerksRuledBoostDefinitions.java`
- Create: `perks/src/main/java/net/aincraft/perks/recipe/PerksRecipeRegistry.java`
- Create remaining focused job handlers under `perks/src/main/java/net/aincraft/perks/capability/`
- Test: `perks/src/test/java/net/aincraft/perks/{boost/PerksRuledBoostDefinitionsTest,recipe/PerksRecipeRegistryTest,capability/JobActionCapabilityHandlersTest}.java`

- [ ] Add a parameterized oracle row for every design boost and recipe; test exact multiplier, predicate, no cross-job leakage, namespace collision, allowlists, cancellation, and disabled capabilities.
- [ ] Run focused tests; expect missing definitions.
- [ ] Implement ruled boosts via existing effect engine and namespaced recipes; no duplicate payout, refund duplication, or arbitrary commands.
- [ ] Rerun focused tests; expect PASS for every matrix row.
- [ ] Commit boosts as `feat(perks): add job specialization boosts` and recipes/handlers as `feat(perks): add perk recipes and action handlers`.

### Task 16: Make eight taskless jobs progress

**Files:**
- Modify: `paper/src/main/resources/job_tasks.csv`
- Test: `paper/src/test/java/net/aincraft/service/TasklessJobProgressionRowsTest.java`

- [ ] Test exact action families for smelting, milling, tanning, refining, cooking, armorsmithing, tailoring, engineering; positive XP/money, valid keys/materials, no duplicate rows.
- [ ] Run focused test; expect all eight absent.
- [ ] Add explicit balanced CSV rows while preserving existing rows exactly.
- [ ] Rerun focused test; expect PASS.
- [ ] Commit: `feat(content): add progression for processing jobs`.

**Phase C gate:** `./gradlew :perks:test :paper:test --tests '*TasklessJobProgressionRowsTest' :perks:shadowJar` passes; the separate jar contains 19 trees and no API/MapGUI classes.

---

## Phase D — Integration, docs, packaging, and smoke

### Task 17: Add two-plugin integration tests

**Files:**
- Create: `paper/src/test/java/net/aincraft/integration/{PerksPluginIntegrationTest,PerksPersistenceIntegrationTest}.java`
- Create: `paper/src/test/resources/perks/unknown-node-state.json`
- Modify test source-set/dependencies only as required

- [ ] Start a two-descriptor fixture and test one API class identity, one generation, 19 trees, Miner preservation, stale rejection, unknown-state blocking, disabled-hook reason, capability retry, and restart restoration.
- [ ] Run `./gradlew :paper:test --tests 'net.aincraft.integration.Perks*'`; expect any remaining integration failure.
- [ ] Fix failures only in the owning implementation; never weaken fixtures or add compatibility shims.
- [ ] Run `./gradlew :api:test :common:test :paper:test :perks:test`; expect PASS.
- [ ] Commit each confirmed owning fix with its integration test and a specific message.

### Task 18: Document and package separate artifacts

**Files:**
- Modify: `README.md`, `CHANGELOG.md`, `docs/database-schema.md`
- Create/update exact pages under `web/src/content/docs/` for perk catalog, config, and provider API
- Modify: `scripts/{package-release-assets.sh,test-package-release-assets.sh}`
- Modify release build configuration without overwriting unrelated user changes

- [ ] Extend package test to require `paper-all.jar` and `modularjobs-perks-all.jar`, inspect descriptors, and reject API/MapGUI classes in perks.
- [ ] Run `scripts/test-package-release-assets.sh`; expect failure before packaging changes.
- [ ] Document dependency order, manual schema application, bounds/defaults, archive/new provenance, disabled hooks, stable IDs/full replacement, no reload, and both artifact paths.
- [ ] Update packaging for two separate artifacts.
- [ ] Run `./gradlew :paper:build :perks:build && scripts/test-package-release-assets.sh && npm --prefix web run build`; expect PASS.
- [ ] Commit docs as `docs: document ModularJobs perks plugin`; commit packaging separately as `build: package separate perks plugin artifact`.

### Task 19: Final verification and Paper smoke

- [ ] Apply schema externally with `scripts/apply-postgres-schema.sh`; expect successful manual application and no plugin DDL.
- [ ] Run `./gradlew :api:test :common:test :paper:test :perks:test :paper:build :perks:build`; expect PASS and both jars.
- [ ] Start `./gradlew :paper:runServer`; expect MapGUI, ModularJobs, then ModularJobsPerks with one installed generation and no linkage/schema error.
- [ ] Open `/jobs upgrade miner`; verify preserved core cluster plus provider branches/edges. Purchase one ruled boost and one capability; exercise, reconnect, restart, and verify state/effects.
- [ ] Open one archive-backed and one original tree; remove an optional hook and verify visible disabled reason; load unknown-node fixture and verify purchase/reset fail closed.
- [ ] Run `scripts/test-package-release-assets.sh && git diff --check && git status --short`; expect packaging PASS, no whitespace errors, and only intentional plus pre-existing unrelated paths.
- [ ] Invoke requesting-code-review against the full branch, resolve confirmed findings, rerun affected/full verification, then use finishing-a-development-branch for integration choice.
