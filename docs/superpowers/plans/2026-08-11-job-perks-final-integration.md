# Job Perks Final Integration and Release Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete only the final integration, documentation, and release work approved by `docs/superpowers/specs/2026-08-11-job-perks-plugin-design.md`: the externally applied PostgreSQL rollout fixture, two-plugin classloading/install proof, all 19 upgrade-graph/persistence E2E cases, a Paper lifecycle smoke scenario, synchronized documentation/catalog/configuration/API/database/changelog surfaces, and separate artifact packaging plus CI assertions.

**Architecture:** Earlier subplans are inputs. They already own the Java 21 public contracts in `api`, the Java 25 Paper/perks implementation in `paper`, the upgrade-tree/skill-tree domain, repository/service wiring, and the MapGUI-facing UI. This plan deliberately does not recreate those components. The work here exercises the contracts end to end through the existing `PluginContext` composition root, the existing `PlayerUpgradeRepository`/`UpgradeService` graph boundary, PostgreSQL applied by `scripts/apply-postgres-schema.sh`, and a real Paper 26.2 lifecycle. Release work keeps `modularjobs-api`/`modularjobs-common` Maven artifacts separate from the `paper-all.jar` plugin artifact and checks their classfile/package boundaries.

**Tech Stack:** Java 21 API; Java 25 Paper/perks; Gradle 9.6.1; Paper 26.2; MapGUI API `io.github.flog99:mapgui-api:1.0.0`; PostgreSQL 16-compatible SQL; HikariCP; JUnit 5.11.4; MockBukkit 26.2 where a live server is not required; Gradle Shadow; GitHub Actions workflow `.github/workflows/ci.yml`; existing shell scripts and release asset conventions.

## Global Constraints

- **Scope:** final integration/docs/release only. Do not reimplement API, upgrade graph, persistence, service, GUI, or wiring components delivered by earlier subplans.
- **Working tree:** preserve unrelated existing changes. Before each commit, stage only files named by that task; do not run cleanup commands that can remove user work.
- **PostgreSQL ownership:** PostgreSQL is connect-only. The Paper plugin and `web/rest-api` never create or migrate schema objects. Operators/CI apply `paper/src/main/resources/sql/postgres.sql` through `scripts/apply-postgres-schema.sh` before startup.
- **Versions:** API Java release is 21; Paper/perks Java release is 25; runtime is Paper 26.2; external MapGUI boundary is API 1.0.0; Gradle wrapper is 9.6.1.
- **Existing package names:** Public Java API types use `net.aincraft`, for example `net.aincraft.Job`, `net.aincraft.JobTask`, `net.aincraft.Bridge`, `net.aincraft.container.Payable`, `net.aincraft.container.Boost`, and `net.aincraft.container.ActionTypes`. Paper upgrade types use `net.aincraft.upgrade`, including `PlayerUpgradeRepository`, `UpgradeService`, `UpgradeTree`, `SkillTree`, and `SkillTreeState`.
- **TDD:** Every implementation task writes an observable failing test or executable assertion first, runs the exact focused command to capture failure, makes the smallest implementation change, reruns for a pass, and ends with one atomic commit.
- **No vague work:** Do not write “similar tests,” “appropriate validation,” or unnamed artifact paths. The exact files, symbols, assertions, commands, and expected results below are the acceptance contract.
- **No runtime DDL:** A passing smoke test must prove schema was applied before plugin enable, not that the plugin silently repaired the database.

## Evidence map and file ownership

The current repository already provides these integration anchors. Task 1 extends `paper/src/test/java/net/aincraft/repository/PostgresSchemaFidelityTest.java`, whose existing `requirePostgres()` opens a real JDBC connection and currently calls `DatabaseType.POSTGRES.getSQLTables()` directly. The final connect-only test must preserve the real connection but move schema application responsibility to the external script fixture; it must not broaden runtime DDL. `paper/src/test/java/net/aincraft/repository/SchemaPresenceTest.java`, `SchemaPolicyTest.java`, `DatabaseConfigSectionsTest.java`, and `PluginResourcesLifecycleTest.java` are sibling tests that must remain passing.

The actual rollout script is `scripts/apply-postgres-schema.sh`; it already resolves `paper/src/main/resources/sql/postgres.sql`, uses `psql -v ON_ERROR_STOP=1` when available, and supports a Docker fallback. Do not replace its working PostgreSQL-only behavior with a generic migration framework. The current schema ownership documentation is `docs/database-schema.md`, and the current configuration template is `paper/src/main/resources/database.yml` with `payable`, `timed-boost`, `progression`, and `upgrades` sections, each using `type: postgres`, `jdbc-url`, `username`, `password`, and `maximum-pool-size`.

The current upgrade implementation is not the hypothetical `JobUpgradeNode`/`UserUpgradeRepository` naming from the rejected draft. Current evidence identifies `net.aincraft.upgrade.PlayerUpgradeRepository` as the relational implementation, with `loadPlayerData(String,String)`, `savePlayerData(PlayerUpgradeDataImpl)`, `loadState(String,String)`, `saveState(SkillTreeState)`, and `hydrate(SkillTree, SkillTreeState)`. Current service boundary is `net.aincraft.upgrade.UpgradeService`; current graph models are `UpgradeTree`, `UpgradeNode`, `SkillTree`, `SkillNode`, and `SkillTreeState`. All E2E tasks below consume these actual symbols. If the approved design names an adapter in a new final-integration package, the adapter may be tested, but existing symbols remain the source of truth.

The root build already sets `javaVersion = if (moduleName == "paper") 25 else 21`, applies `options.release`, publishes `modularjobs-api` and `modularjobs-common`, and configures Paper Shadow. `paper/build.gradle.kts` already has `compileOnly("io.github.flog99:mapgui-api:1.0.0")`, `implementation(libs.postgresql)`, `shadowJar` relocation for Craftux, and a Java 25 Paper run server on Minecraft/Paper version `26.2`. The current CI file `.github/workflows/ci.yml` already provisions PostgreSQL on port `55432`, applies the schema, runs `./gradlew check`, builds `:paper:shadowJar`, and uploads `paper/build/libs/*-all.jar`. Task 6 extends these exact paths rather than inventing a second workflow.

The current release asset script `scripts/package-release-assets.sh` names outputs `modularjobs-paper-$VERSION.jar` and `modularjobs-postgres-$VERSION.sql`, and the existing test `scripts/test-package-release-assets.sh` expects those exact names plus `SHA256SUMS`. Separate API packaging must use the existing Maven publication artifact `modularjobs-api`, while the Paper release continues to use `modularjobs-paper-2.0.0.jar` for the immutable release flow. No task may rename existing release assets without updating their existing test and CI call sites in the same atomic change.

---

## Task 1: PostgreSQL rollout fixture and connect-only startup

**Purpose:** Prove that an operator/CI applies the shipped SQL externally and that the Paper process only connects and verifies required relations. This task does not add migrations and does not move DDL into plugin startup.

**Files:**

- Modify: `paper/src/test/java/net/aincraft/repository/PostgresSchemaFidelityTest.java`.
- Modify: `paper/src/test/java/net/aincraft/repository/SchemaPresenceTest.java` only if its existing startup-failure assertion needs the exact external-script message.
- Modify: `paper/src/test/java/net/aincraft/repository/SchemaPolicyTest.java` only if the existing policy test must assert the new fixture command.
- Modify: `paper/src/test/java/net/aincraft/test/TestPostgres.java` for a unique database/schema reset helper, if the current helper lacks it.
- Modify: `scripts/apply-postgres-schema.sh` only where its existing behavior does not support the required fixture assertion.
- Modify: `paper/src/main/resources/sql/postgres.sql` only if a failing catalog assertion demonstrates that the approved schema omits an object required by current repositories.
- Modify: `.github/workflows/ci.yml` only to make manual schema application a distinct, named prerequisite before Java tests; this is included here rather than Task 6 because it defines the fixture lifecycle.

**Interfaces consumed:**

- `net.aincraft.repository.DatabaseType.POSTGRES.getSQLTables()` and `paper/src/main/resources/sql/postgres.sql`.
- `net.aincraft.repository.ConnectionSourceFactory` and `HikariConfigProvider`.
- Current repositories exercised by the schema test: `JobRepository`, `JobProgressionRepository`, `TimedBoostRepository`, `PlayerUpgradeRepository`, and any repository names discovered in the existing package.
- Existing `MODULARJOBS_TEST_PG_URL`, `MODULARJOBS_TEST_PG_USER`, and `MODULARJOBS_TEST_PG_PASSWORD` environment variables used by `PostgresSchemaFidelityTest`.

**Interfaces produced:**

- `SchemaFixture.applyExternally()` (test helper only) invokes `scripts/apply-postgres-schema.sh` against the configured JDBC/PostgreSQL endpoint.
- `SchemaFixture.resetToEmptySchema()` leaves no application-owned tables before script execution.
- A plugin/repository startup against an empty schema throws a missing-relation failure and does not issue `CREATE TABLE`.
- A script-applied schema exposes the exact relations used by current repository SQL, including the existing `job_tasks`, `job_task_payables`, `payable_records`, `job_progression`, and player-upgrade relations named by `postgres.sql`.

- [ ] **Step 1: Write the failing tests.** Add these concrete methods to `PostgresSchemaFidelityTest.java`, using the existing JDBC setup and the actual table names from `postgres.sql` rather than guessed names:

```java
@Test
void externalSchemaApplicationCreatesEveryRelationUsedByRepositories() throws SQLException {
  requirePostgresWithoutApplyingSchema();
  database.resetToEmptySchema();

  schemaFixture.applyExternally();

  assertEquals("job_tasks", database.scalar("select to_regclass('public.job_tasks')"));
  assertEquals("job_task_payables", database.scalar("select to_regclass('public.job_task_payables')"));
  assertEquals("payable_records", database.scalar("select to_regclass('public.payable_records')"));
  assertEquals("job_progression", database.scalar("select to_regclass('public.job_progression')"));
  assertEquals("player_upgrades", database.scalar("select to_regclass('public.player_upgrades')"));
  assertEquals(0, database.applicationDdlStatements().size());
}

@Test
void missingSchemaFailsConnectionUseWithoutCreatingTables() throws SQLException {
  requirePostgresWithoutApplyingSchema();
  database.resetToEmptySchema();

  SQLException failure = assertThrows(SQLException.class, () -> repositoryProbe.loadProgression());

  assertTrue(failure.getMessage().contains("does not exist"));
  assertEquals(0, database.scalarInt(
      "select count(*) from pg_catalog.pg_tables where schemaname='public'"));
}
```

The helper must not call the current `requirePostgres()` method if that method still applies DDL internally. Split it into a connection-only setup and an explicit test fixture so the test detects accidental application DDL.

- [ ] **Step 2: Run the focused failure.** Use a disposable PostgreSQL 16 endpoint on the repository’s documented `127.0.0.1:55432` default, or set the existing environment variables:

```bash
./gradlew :paper:test --tests 'net.aincraft.repository.PostgresSchemaFidelityTest.externalSchemaApplicationCreatesEveryRelationUsedByRepositories' --info
./gradlew :paper:test --tests 'net.aincraft.repository.PostgresSchemaFidelityTest.missingSchemaFailsConnectionUseWithoutCreatingTables' --info
```

Expected initial result: the first test fails because the fixture does not yet call the external script or because an expected relation is absent; the second fails if current test setup silently applies SQL or if startup creates tables. A skipped test because PostgreSQL is unreachable is not an acceptable pass for the integration task; CI must have the service available and the local focused run must report the exact endpoint used.

- [ ] **Step 3: Implement the minimal fixture/script change.** Preserve the existing script’s URL argument and `DATABASE_URL` fallback, source path, `psql -v ON_ERROR_STOP=1`, and Docker fallback. Add only a test-facing invocation and explicit `--help`/`--check` behavior if the current script lacks them. The required external command behavior is:

```bash
# from repository root; no Java process is involved
./scripts/apply-postgres-schema.sh \
  "postgres://test:test@127.0.0.1:55432/modularjobs"

# equivalent environment form used by docs and CI
DATABASE_URL="postgres://test:test@127.0.0.1:55432/modularjobs" \
  ./scripts/apply-postgres-schema.sh
```

Do not add a `CREATE TABLE` call to `PluginContext`, `ConnectionSourceFactory`, a repository constructor, or a Paper lifecycle listener. If a repository currently performs a schema creation call, remove only that call and retain its connection/query behavior; the existing `SchemaPolicyTest` must prove the removal.

- [ ] **Step 4: Verify focused pass, idempotency, and negative path.** Run:

```bash
./scripts/apply-postgres-schema.sh --help
./scripts/apply-postgres-schema.sh postgres://test:test@127.0.0.1:55432/modularjobs
./scripts/apply-postgres-schema.sh postgres://test:test@127.0.0.1:55432/modularjobs
./gradlew :paper:test --tests 'net.aincraft.repository.PostgresSchemaFidelityTest' --tests 'net.aincraft.repository.SchemaPresenceTest' --tests 'net.aincraft.repository.SchemaPolicyTest' --info
```

Expected pass: help exits `0`; the first script invocation applies `postgres.sql` with `ON_ERROR_STOP=1`; the second invocation has the approved idempotent result; the focused classes pass; the missing-schema path reports PostgreSQL SQLSTATE `42P01`/“relation does not exist” and catalog count `0`; no application DDL statement is observed. If the approved SQL is intentionally non-idempotent, the second invocation must fail with the documented duplicate-object error and the docs/CI must state one-shot behavior; do not mask it.

- [ ] **Step 5: Commit exactly this slice.**

```bash
git add paper/src/test/java/net/aincraft/repository/PostgresSchemaFidelityTest.java \
  paper/src/test/java/net/aincraft/repository/SchemaPresenceTest.java \
  paper/src/test/java/net/aincraft/repository/SchemaPolicyTest.java \
  paper/src/test/java/net/aincraft/test/TestPostgres.java \
  scripts/apply-postgres-schema.sh \
  paper/src/main/resources/sql/postgres.sql \
  .github/workflows/ci.yml
git commit -m "test(db): verify connect-only postgres schema rollout"
```

**Failure risks:** Tests can accidentally pass against the wrong schema through `search_path`; set `search_path=public` and assert `pg_catalog`. Existing `PostgresSchemaFidelityTest` currently applies `DatabaseType.POSTGRES.getSQLTables()` directly; the final design must preserve its round-trip coverage while making the new external rollout assertion explicit. Do not delete unrelated fidelity tests.

---

## Task 2: Two-plugin classloading and installation isolation

**Purpose:** Prove the API/Paper artifact boundary under two independent plugin classloaders and a real plugin installation layout. MapGUI remains external compile-only API 1.0.0.

**Files:**

- Modify: `paper/build.gradle.kts` only for a focused packaging-test task or artifact dependency needed by the test.
- Modify: `api/build.gradle.kts` only if the existing `jar`/Maven publication is not available to the test.
- Create: `paper/src/test/java/net/aincraft/packaging/TwoPluginClassLoadingTest.java`.
- Create: `paper/src/test/resources/net/aincraft/packaging/first-plugin.yml` and `second-plugin.yml` only if the existing `paper/src/main/resources/plugin.yml` cannot represent two distinct descriptors.
- Modify: `paper/src/test/java/net/aincraft/PluginYmlProductionReadinessTest.java` only if the packaging assertion must share its existing descriptor parser.

**Interfaces consumed:**

- `net.aincraft.Job`, `net.aincraft.JobTask`, `net.aincraft.Bridge`.
- `net.aincraft.container.Payable`, `net.aincraft.container.Boost`, and `net.aincraft.container.ActionTypes`.
- Paper main class and `PluginContext.create(this)` lifecycle boundary.
- Gradle outputs `api/build/libs/modularjobs-api-0.0.0-SNAPSHOT.jar` for the default version and `paper/build/libs/paper-all.jar` for the Shadow output.
- External compile-only dependency `io.github.flog99:mapgui-api:1.0.0`.

**Interfaces produced:**

- `ArtifactInspection.entries(Path): Set<String>` reads a JAR using `java.util.jar.JarFile`.
- `ArtifactInspection.pluginDescriptor(Path): PluginDescriptor` reads the `plugin.yml` entry and returns its parsed `name`.
- Two independent `URLClassLoader` instances that resolve the Paper main class from two jar URLs.
- An assertion that API artifact contains `net/aincraft/Job.class` exactly once, contains no `net/aincraft/paper/` implementation package, and contains no `org/mapgui/` classes.
- An installation fixture with two distinct plugin descriptor names and no duplicate plugin identity.

- [ ] **Step 1: Write the failing test.** Create `TwoPluginClassLoadingTest.java` with these literal assertions (use the main class name from the existing `paper/src/main/resources/plugin.yml`, not a guessed class name):

```java
@Test
void twoPluginJarsResolveThroughIndependentClassloaders() throws Exception {
  Path first = artifacts.paperShadowJar();
  Path second = artifacts.copyForSecondInstallation();

  try (URLClassLoader firstLoader = new URLClassLoader(
           new URL[] {first.toUri().toURL()}, null);
       URLClassLoader secondLoader = new URLClassLoader(
           new URL[] {second.toUri().toURL()}, null)) {
    Class<?> firstMain = Class.forName(artifacts.mainClassName(), true, firstLoader);
    Class<?> secondMain = Class.forName(artifacts.mainClassName(), true, secondLoader);

    assertNotSame(firstMain.getClassLoader(), secondMain.getClassLoader());
    assertEquals(first.toUri().toURL(),
        firstMain.getProtectionDomain().getCodeSource().getLocation());
    assertEquals(second.toUri().toURL(),
        secondMain.getProtectionDomain().getCodeSource().getLocation());
  }
}

@Test
void apiArtifactContainsOnlyPublicApiAndNotMapGuiOrPaperImplementation() {
  Set<String> entries = ArtifactInspection.entries(artifacts.apiJar());

  assertEquals(1, entries.stream().filter("net/aincraft/Job.class"::equals).count());
  assertTrue(entries.stream().noneMatch(name -> name.startsWith("net/aincraft/paper/")));
  assertTrue(entries.stream().noneMatch(name -> name.startsWith("org/mapgui/")));
}

@Test
void twoInstalledDescriptorsHaveDistinctPluginNames() throws IOException {
  Path plugins = tempDir.resolve("plugins");
  Files.copy(artifacts.paperShadowJar(), plugins.resolve("modularjobs-one.jar"));
  Files.copy(artifacts.paperShadowJar(), plugins.resolve("modularjobs-two.jar"));

  List<String> names = Files.list(plugins)
      .map(ArtifactInspection::pluginDescriptor)
      .map(PluginDescriptor::name)
      .toList();
  assertEquals(names.size(), new HashSet<>(names).size());
}
```

The installation test must use two distinct descriptor fixtures if the production descriptor necessarily has one fixed plugin name; it must not modify production `plugin.yml` merely to make the test pass.

- [ ] **Step 2: Run focused failure.** Run:

```bash
./gradlew :paper:test --tests 'net.aincraft.packaging.TwoPluginClassLoadingTest' --info
```

Expected initial result: `FAIL` naming unavailable artifact paths, inability to load the main class, duplicate API classes, a bundled MapGUI package, or duplicate plugin names. A successful load through one shared Gradle test classloader is not sufficient.

- [ ] **Step 3: Implement minimal packaging-test wiring.** Make the test depend on built artifacts, not `sourceSets.main.output`. Use the current `paper` Shadow output and the current API jar. Keep `mapgui-api:1.0.0` compile-only. Preserve `relocate("dev.craftux", "net.aincraft.libs.craftux")`; do not introduce a broad relocation of public API. If the artifact currently embeds API/common through the Craftux dependency, use the existing production exclusion convention and assert the final package contents rather than blindly relocating `net.aincraft`.

- [ ] **Step 4: Run focused pass and inspect package entries.** Run:

```bash
./gradlew :api:jar :paper:shadowJar
./gradlew :paper:test --tests 'net.aincraft.packaging.TwoPluginClassLoadingTest' --info
jar tf api/build/libs/modularjobs-api-0.0.0-SNAPSHOT.jar > /tmp/modularjobs-api.entries
jar tf paper/build/libs/paper-all.jar > /tmp/modularjobs-paper.entries
```

Expected pass: classloaders and code-source URLs are distinct; the API class occurs once; no `net/aincraft/paper/` or `org/mapgui/` entries occur in the API jar; descriptor names are unique. The default local version is `0.0.0-SNAPSHOT`; release CI supplies an explicit `releaseVersion`, and the assertion must resolve that exact filename rather than use a wildcard.

- [ ] **Step 5: Commit exactly this slice.**

```bash
git add paper/build.gradle.kts api/build.gradle.kts \
  paper/src/test/java/net/aincraft/packaging \
  paper/src/test/resources/net/aincraft/packaging \
  paper/src/test/java/net/aincraft/PluginYmlProductionReadinessTest.java
git commit -m "test(packaging): prove two-plugin classloader isolation"
```

**Failure risks:** Parent-first loading can conceal duplicate classes. The test therefore checks classloader identity and `ProtectionDomain.codeSource`. Do not bundle MapGUI to make loading succeed; the contract is an external API boundary. Do not use a second plugin name in production metadata.

---

## Task 3: All 19 graph/persistence E2E cases

**Purpose:** Exercise the completed upgrade graph and persistence implementation through the real PostgreSQL repository/service boundary. Exactly 19 tests must be separately reported.

**Files:**

- Create: `paper/src/test/java/net/aincraft/upgrade/UpgradeGraphPersistenceE2ETest.java`.
- Modify: `paper/src/test/java/net/aincraft/test/TestPostgres.java` for per-test transaction/cleanup and service recreation helpers.
- Modify: `paper/src/test/java/net/aincraft/upgrade/SkillTreeTest.java` only if a shared fixture helper can be extracted without changing its existing unit assertions; prefer no change.
- Modify: `paper/build.gradle.kts` only if a named `integrationTest` task is required to run the new class against the PostgreSQL service.
- Modify: production `paper/src/main/java/net/aincraft/upgrade/PlayerUpgradeRepository.java` or `UpgradeService.java` only when an E2E test proves an integration defect; do not reimplement graph behavior.

**Interfaces consumed:**

- `net.aincraft.upgrade.PlayerUpgradeRepository(ConnectionSource)` methods `loadPlayerData`, `savePlayerData`, `loadState`, `saveState`, and static `hydrate`.
- `net.aincraft.upgrade.UpgradeService`, `UpgradeTree`, `UpgradeNode`, `SkillTree`, `SkillNode`, and `SkillTreeState`.
- Existing `ConnectionSourceFactory`, `PluginContext`/upgrade wiring, and PostgreSQL schema relations from Task 1.
- Current command/UI paths are not substituted for service assertions; this task tests domain persistence directly and Task 4 tests lifecycle commands.

**Interfaces produced:**

- `UpgradeGraphPersistenceE2ETest` with exactly 19 `@Test` methods.
- `E2eFixture.userId()`, `jobKey()`, `skillTree()`, `createRepository()`, `createUpgradeService()`, `recreateUpgradeService()`, `countPersistedRows()`, and `resetDatabase()`.
- Every test asserts a domain result and the matching persisted SQL state where persistence is part of the case.

- [ ] **Step 1: Write all 19 failing tests.** Create the class with these exact method names and observable contracts; use actual constructors/factories from the current source when wiring the fixture:

```java
@Test
void createsAndReloadsACompleteSkillTree() {
  SkillTree tree = fixture.skillTree();
  fixture.register(tree);
  assertEquals(tree, fixture.createUpgradeService().getSkillTree("miner").orElseThrow());
}

@Test
void rootNodeIsAvailableBeforeAnyPurchase() {
  assertTrue(fixture.availableNodes(fixture.userId(), fixture.jobKey()).contains("root"));
}

@Test
void rejectsNodeWhenSinglePrerequisiteIsMissing() {
  assertFalse(fixture.canPurchase(fixture.userId(), fixture.jobKey(), "iron-tier"));
}

@Test
void acceptsNodeWhenSinglePrerequisiteIsPurchased() {
  fixture.purchase(fixture.userId(), fixture.jobKey(), "root");
  assertTrue(fixture.canPurchase(fixture.userId(), fixture.jobKey(), "iron-tier"));
}

@Test
void rejectsNodeWhenOneOfMultiplePrerequisitesIsMissing() {
  fixture.purchase(fixture.userId(), fixture.jobKey(), "root");
  assertFalse(fixture.canPurchase(fixture.userId(), fixture.jobKey(), "master"));
}

@Test
void rejectsDuplicateNodeDefinition() {
  assertThrows(IllegalArgumentException.class, () -> fixture.registerDuplicateNode("iron-tier"));
}

@Test
void rejectsDuplicatePrerequisiteEdge() {
  assertThrows(IllegalArgumentException.class, () -> fixture.registerDuplicatePrerequisite("iron-tier", "root"));
}

@Test
void rejectsCyclicPrerequisiteGraph() {
  assertThrows(IllegalArgumentException.class, () -> fixture.registerCycle("root", "iron-tier"));
}

@Test
void rejectsPrerequisiteReferencingMissingNode() {
  assertThrows(IllegalArgumentException.class, () -> fixture.registerMissingPrerequisite("iron-tier", "unknown"));
}

@Test
void savesPurchasedNodeAndReloadsIt() {
  fixture.purchase(fixture.userId(), fixture.jobKey(), "root");
  assertEquals(1, fixture.reloadedState().levelOf("root"));
}

@Test
void duplicatePurchaseDoesNotCreateSecondPersistedRow() {
  fixture.purchase(fixture.userId(), fixture.jobKey(), "root");
  fixture.purchase(fixture.userId(), fixture.jobKey(), "root");
  assertEquals(1, fixture.persistedStateRows());
}

@Test
void rejectsPurchaseWhenSkillPointsAreInsufficient() {
  assertThrows(IllegalStateException.class,
      () -> fixture.purchase(fixture.userId(), fixture.jobKey(), "master"));
}

@Test
void separatesPlayerAndJobOwnershipAcrossUsers() {
  fixture.purchase("player-a", fixture.jobKey(), "root");
  assertFalse(fixture.stateFor("player-b", fixture.jobKey()).nodeLevels().containsKey("root"));
}

@Test
void rollsBackPurchaseWhenPersistenceFails() {
  fixture.failNextSave();
  assertThrows(RuntimeException.class,
      () -> fixture.purchase(fixture.userId(), fixture.jobKey(), "root"));
  assertEquals(0, fixture.persistedStateRows());
}

@Test
void makesCommittedPurchaseVisibleToNewConnection() {
  fixture.purchase(fixture.userId(), fixture.jobKey(), "root");
  assertEquals(1, fixture.newConnectionState().levelOf("root"));
}

@Test
void persistedPurchasedNodeSurvivesServiceRecreation() {
  String playerId = fixture.userId();
  String jobKey = fixture.jobKey();
  SkillTree tree = fixture.skillTree();
  SkillTreeState before = fixture.stateWithPurchased(tree, "iron-tier");
  fixture.repository().saveState(before);
  UpgradeService restarted = fixture.recreateUpgradeService();
  SkillTreeState after = restarted.getSkillTreeState(playerId, jobKey);
  assertEquals(1, after.levelOf("iron-tier"));
  assertEquals(1, fixture.countPersistedRows(playerId, jobKey, "iron-tier"));
}

@Test
void concurrentPurchasesPreserveUniqueState() {
  fixture.purchaseConcurrently(fixture.userId(), fixture.jobKey(), "root");
  assertEquals(1, fixture.persistedStateRows());
}

@Test
void deletingJobCleansDependentUpgradeState() {
  fixture.purchase(fixture.userId(), fixture.jobKey(), "root");
  fixture.deleteJob(fixture.jobKey());
  assertEquals(0, fixture.persistedStateRows());
}

@Test
void traversesAllPurchasedPrerequisitesInStableOrder() {
  fixture.purchase(fixture.userId(), fixture.jobKey(), "root");
  fixture.purchase(fixture.userId(), fixture.jobKey(), "iron-tier");
  assertEquals(List.of("root", "iron-tier"), fixture.purchasedPrerequisites());
}
```

Do not leave ellipses in the implementation. The first test’s literal body must be concrete and use current symbols:

```java
@Test
void persistedPurchasedNodeSurvivesServiceRecreation() {
  String playerId = fixture.userId();
  String jobKey = fixture.jobKey();
  SkillTree tree = fixture.skillTree();
  SkillTreeState before = fixture.stateWithPurchased(tree, "iron-tier");

  fixture.repository().saveState(before);

  UpgradeService restarted = fixture.recreateUpgradeService();
  SkillTreeState after = restarted.getSkillTreeState(playerId, jobKey);

  assertEquals(1, after.levelOf("iron-tier"));
  assertEquals(1, fixture.countPersistedRows(playerId, jobKey, "iron-tier"));
}
```

Every method above must be implemented with the fixture calls and assertions shown; no ellipsis or empty test body is permitted. The graph cases use `SkillTree`/`SkillNode` for v2 tree persistence and `UpgradeTree`/`UpgradeNode` only for legacy cases where the approved design requires them. Database assertions query columns actually present in `postgres.sql`; they never assume a hypothetical `user_upgrades(node_key)` table.

- [ ] **Step 2: Run against an unapplied real database.** Run the exact class through the configured integration task:

```bash
./gradlew :paper:integrationTest --tests 'net.aincraft.upgrade.UpgradeGraphPersistenceE2ETest' --info
```

Expected initial result: `FAIL` because no schema has been externally applied or because the first real repository/wiring contract is incomplete. If the repository has no `integrationTest` task, first run `./gradlew tasks --all | grep -E 'integration|test'` and use the existing named task; do not silently run only MockBukkit unit tests.

- [ ] **Step 3: Apply schema and implement only the minimal integration correction.** Run Task 1’s script against a unique PostgreSQL database, then fix the first real defect in field mapping, SQL transaction boundaries, service recreation, or composition wiring. Preserve `PlayerUpgradeRepository`’s explicit connect-only comment and its public method signatures. If a test’s expected column differs from `postgres.sql`, correct the test to the approved current schema instead of adding an incompatible column.

- [ ] **Step 4: Run exact pass and enforce count.** Run:

```bash
./gradlew :paper:integrationTest --tests 'net.aincraft.upgrade.UpgradeGraphPersistenceE2ETest' --info
```

Expected output must include exactly `19 tests completed, 0 failed`. A report showing 18 or 20 tests fails the task. Verify cleanup with:

```bash
psql "postgres://test:test@127.0.0.1:55432/modularjobs" -Atc \
  "select count(*) from player_upgrades"
```

The final count must match only intentionally retained fixture rows, and each test must use unique player/job keys or transaction rollback.

- [ ] **Step 5: Commit exactly this slice.**

```bash
git add paper/src/test/java/net/aincraft/upgrade/UpgradeGraphPersistenceE2ETest.java \
  paper/src/test/java/net/aincraft/test/TestPostgres.java paper/build.gradle.kts
# Add a production file only if a focused failure proves the smallest integration defect.
git add paper/src/main/java/net/aincraft/upgrade/PlayerUpgradeRepository.java \
  paper/src/main/java/net/aincraft/upgrade/UpgradeService.java
git commit -m "test(upgrades): cover nineteen graph persistence e2e cases"
```

**Failure risks:** Service caches can hide persisted state, transaction connections can read stale rows, and concurrent writes can create duplicate serialized entries. Assert both domain state and SQL state, reset between tests, and use a real PostgreSQL endpoint. Do not make a flaky test pass by removing concurrency, rollback, or restart coverage.

---

## Task 4: Paper 26.2 lifecycle smoke scenario

**Purpose:** Start the actual Paper 26.2 server with the built plugin, verify enablement and the registered upgrade command, check persistence, and shut down cleanly. The smoke test is intentionally separate from the 19-case service E2E suite.

**Files:**

- Modify: `paper/build.gradle.kts`, specifically the existing `tasks.named<xyz.jpenilla.runpaper.task.RunServer>("runServer")` block, to add a disposable `runServerSmoke` task without changing Java 25/Paper 26.2 settings. The fully qualified type already used by the build is `xyz.jpenilla.runpaper.task.RunServer`.
- Create: `paper/src/test/java/net/aincraft/smoke/PaperJobPerksSmokeTest.java`.
- Create: `paper/src/test/resources/smoke/miner-upgrade-tree.yml` with the deterministic `miner`/`iron-tier` fixture.
- Modify: `paper/src/main/resources/plugin.yml` only when the smoke test exposes a missing command/plugin descriptor entry.
- Modify: `scripts/apply-postgres-schema.sh` only when Task 1's fixture invocation cannot support the smoke database.

**Interfaces consumed:**

- Main Paper plugin class and `PluginContext.create(this)`.
- `net.aincraft.upgrade.UpgradeService` and `net.aincraft.commands.UpgradesCommand`.
- Existing `DomainWiring`, `PayableWiring`, `PaymentWiring`, `PlayerUpgradeRepository`, and command registration.
- Existing `paper/src/main/resources/plugin.yml` descriptor and Paper run-server task configured for Java 25 and Minecraft/Paper `26.2`.

**Interfaces produced:**

- `./gradlew :paper:runServerSmoke` exits `0` only after the full lifecycle scenario.
- Smoke output contains literal marker `JOB_PERKS_SMOKE_PASS`.
- Temporary server/database directories are removed by a trap after both success and failure.
- Database query observes one persisted upgrade state after the command path.

- [ ] **Step 1: Write the failing smoke test/script.** The test waits for the actual Paper readiness line, plugin enable line, and command output. The current `UpgradesCommand` registers the `jobs upgrades <job>` path and opens the upgrade UI; the smoke driver must invoke that path, then send the registered Craftux action `modularjobs.upgrades.node` with payload `iron-tier`. The executable scenario is:

```text
wait_for_log "Done (.*)! For help, type \\\"help\\\""
wait_for_log "[ModularJobs] Enabled"
send_command "jobs create miner"
expect_output "Created job miner"
send_command "jobs upgrades miner"
expect_output "Opening upgrade tree"
send_upgrade_action "iron-tier"
expect_output "Purchased upgrade iron-tier"
psql "$DATABASE_URL" -Atc "select count(*) from player_upgrades where player_id='$PLAYER_ID' and job_key='miner'"
expect_output "1"
print "JOB_PERKS_SMOKE_PASS"
```

The implementation must replace every pseudo-command in this snippet with the literal command or stdin/UI action supported by current code; no pseudo-command may remain in the executable script. The database query must use the actual `player_upgrades` columns from `postgres.sql`.

- [ ] **Step 2: Run before wiring and verify failure.** Run:

```bash
./gradlew :paper:runServerSmoke --info
```

Expected initial result: `FAIL` at a concrete readiness, plugin enable, command/UI action, database row, or shutdown assertion. A server process that starts but never executes the upgrade path is a failure.

- [ ] **Step 3: Implement minimal smoke wiring.** Configure a unique temporary directory under `paper/run-smoke/` using `mktemp -d paper/run-smoke/job-perks.XXXXXX`, a fixed test player UUID `11111111-2222-3333-4444-555555555555`, and the PostgreSQL test database configured by `MODULARJOBS_TEST_PG_URL`. Require Task 1's schema command before launching Paper. Capture logs and fail immediately on plugin exception, `onDisable` before the scenario, command timeout, or nonzero server exit. Keep optional PlaceholderAPI/Bolt behavior consistent with current `paper/build.gradle.kts` downloads; do not bundle MapGUI.

- [ ] **Step 4: Run focused pass.** Run:

```bash
./gradlew :paper:shadowJar :paper:runServerSmoke --info
```

Expected pass: Paper 26.2 readiness, `[ModularJobs] Enabled`, the current job/upgrade action output, one matching persisted `player_upgrades` row, literal `JOB_PERKS_SMOKE_PASS`, and clean shutdown. Any schema creation log from the plugin is a failure even if the command succeeds.

- [ ] **Step 5: Commit exactly this slice.**

```bash
git add paper/build.gradle.kts paper/src/test/java/net/aincraft/smoke \
  paper/src/test/resources/smoke paper/src/main/resources/plugin.yml scripts/apply-postgres-schema.sh
git commit -m "test(paper): add lifecycle smoke scenario for job perks"
```

**Failure risks:** Paper startup is asynchronous; match readiness and enable lines rather than sleeping a fixed number of seconds. Command output may be Adventure-formatted; normalize only terminal color/control codes, not semantic text. A missing optional plugin must not fail if the current composition root treats it as optional; a missing required database relation must fail with the documented schema command.

---

## Task 5: Documentation, living catalog, configuration, API, database, and changelog

**Purpose:** Publish only facts verified by the repository and approved design. Documentation must match executable scripts, actual YAML keys, public API signatures, schema ownership, artifact names, and compatibility versions.

**Files:**

- Read-only source: `docs/superpowers/specs/2026-08-11-job-perks-plugin-design.md`.
- Modify: `docs/database-schema.md`.
- Modify: `paper/src/main/resources/database.yml` only if configuration defaults or comments are missing from the actual parser contract.
- Modify: the existing living catalog under `docs/living-specs/` (exact existing file selected by the job-perks domain name; do not create a duplicate catalog).
- Modify: the existing public API docs location under `api/` or the API section of the docs site, preserving current Astro/Starlight/frontmatter conventions.
- Modify: `CHANGELOG.md`.
- Create/modify: `scripts/check-docs.sh` only if the repository lacks a docs assertion script; otherwise modify the existing script in place.

**Interfaces consumed:**

- API signatures: `net.aincraft.Job` methods `displayName()`, `getPlainName()`, `description()`, `levelingCurve()`, `payableCurves()`, `maxLevel()`, `upgradeLevel()`, and `perkUnlocks()`; `net.aincraft.JobTask` record `(Key jobKey, Key actionTypeKey, Key contextKey, List<Payable> payables)`; `net.aincraft.container.Payable` record `(PayableType type, PayableAmount amount)`; `net.aincraft.container.ActionTypes.BLOCK_BREAK`; `net.aincraft.Bridge.bridge()`; `net.aincraft.container.Boost.boost(BigDecimal)`.
- Upgrade symbols: `net.aincraft.upgrade.PlayerUpgradeRepository`, `UpgradeService`, `SkillTree`, `SkillTreeState`, and `UpgradeTree`.
- Configuration parser/resource: `paper/src/main/resources/database.yml` sections `payable`, `timed-boost`, `progression`, `upgrades`, keys `type`, `jdbc-url`, `username`, `password`, `maximum-pool-size`.
- Database source: `paper/src/main/resources/sql/postgres.sql` and `docs/database-schema.md`.
- Release paths: `paper/build/libs/paper-all.jar`, `modularjobs-paper-$VERSION.jar`, `modularjobs-postgres-$VERSION.sql`, `SHA256SUMS`, and Maven `modularjobs-api`.

**Interfaces produced:**

- Docs explicitly say PostgreSQL only, schema manually applied, game/API process connect-only, Paper 26.2, Java 25 Paper, Java 21 API, MapGUI API 1.0.0, artifact names, and smoke/E2E commands.
- Config docs show actual nested sections, not an invented `database.apply-schema` key unless the parser actually supports it.
- API docs use real constructors/records, not the rejected draft’s nonexistent `Job.builder`, `JobTask.of`, or `Payable.of` factories.
- Catalog checkboxes identify this integration/release work as current and leave unrelated future work untouched.
- Changelog entry states factual compatibility and schema rollout requirements.

- [ ] **Step 1: Write failing documentation assertions.** Add exact checks to `scripts/check-docs.sh` (or the current docs-check script) for the following strings and paths:

```text
Java 21
Java 25
Gradle 9.6.1
Paper 26.2
MapGUI API 1.0.0
PostgreSQL only
connect-only
scripts/apply-postgres-schema.sh
paper/src/main/resources/sql/postgres.sql
paper/build/libs/paper-all.jar
modularjobs-paper-$VERSION.jar
modularjobs-postgres-$VERSION.sql
SHA256SUMS
net.aincraft.Job
net.aincraft.JobTask
net.aincraft.container.Payable
net.aincraft.container.ActionTypes.BLOCK_BREAK
net.aincraft.upgrade.PlayerUpgradeRepository
19 tests completed, 0 failed
JOB_PERKS_SMOKE_PASS
```

Add a configuration assertion that documented keys match the actual YAML resource:

```bash
for key in 'payable:' 'timed-boost:' 'progression:' 'upgrades:' 'jdbc-url:' 'maximum-pool-size:'; do
  grep -qF "$key" paper/src/main/resources/database.yml
 done
```

If repository conventions prohibit shell `grep`, use the existing script’s current helper; the assertion behavior is required. Add a negative assertion that docs do not claim `Job.builder`, `JobTask.of`, `Payable.of`, `database.apply-schema`, or runtime schema creation unless those symbols/keys are actually introduced by an earlier approved component.

- [ ] **Step 2: Run focused failure.** Run:

```bash
./scripts/check-docs.sh
./gradlew :api:javadoc
```

Expected initial result: docs script fails with named missing terms or stale examples. `:api:javadoc` must pass under Java 21; if it fails, report the actual API documentation error and fix only a real public-doc defect in this task.

- [ ] **Step 3: Make minimal documentation/catalog/config/changelog edits.** Preserve existing `docs/database-schema.md` statements that already correctly say the game process and REST API never run DDL. Extend its startup sequence and upgrade persistence section with actual relation names from SQL and the command:

```bash
./scripts/apply-postgres-schema.sh \
  postgres://user:pass@host:5432/modularjobs
```

Preserve actual `database.yml` shape:

```yaml
payable:
  type: postgres
  jdbc-url: jdbc:postgresql://127.0.0.1:5432/modularjobs
  username: modularjobs
  password: change-me
  maximum-pool-size: 10

timed-boost:
  type: postgres
  jdbc-url: jdbc:postgresql://127.0.0.1:5432/modularjobs
  username: modularjobs
  password: change-me
  maximum-pool-size: 10

progression:
  type: postgres
  jdbc-url: jdbc:postgresql://127.0.0.1:5432/modularjobs
  username: modularjobs
  password: change-me
  maximum-pool-size: 10

upgrades:
  type: postgres
  jdbc-url: jdbc:postgresql://127.0.0.1:5432/modularjobs
  username: modularjobs
  password: change-me
  maximum-pool-size: 10
```

Use a truthful API example based on actual records rather than nonexistent factories:

```java
JobTask task = new JobTask(
    Key.key("modularjobs", "miner"),
    ActionTypes.BLOCK_BREAK.key(),
    Key.key("minecraft", "iron_ore"),
    List.of(new Payable(payableType, PayableAmount.create(new BigDecimal("10")))));
```

The example must import actual `net.aincraft` symbols and define `payableType` using the actual public API; if `PayableType` is registry-provided, show the registry lookup rather than invent a constructor. Document `Job` as an interface with implementations supplied by the plugin, not as a builder unless the actual API has one.

- [ ] **Step 4: Run focused pass and inspect rendered/content checks.** Run:

```bash
./scripts/check-docs.sh
./gradlew :api:javadoc
./gradlew :paper:test --tests 'net.aincraft.repository.DatabaseConfigSectionsTest' --tests 'net.aincraft.repository.PluginResourcesLifecycleTest'
```

Expected pass: docs checks exit `0`; Javadocs pass; config/resource tests pass; every command/table/key in docs maps to an executable file or actual parser symbol; no stale API factory names remain.

- [ ] **Step 5: Commit exactly this slice.**

```bash
git add docs/database-schema.md paper/src/main/resources/database.yml \
  docs/living-specs CHANGELOG.md scripts/check-docs.sh api docs
 git commit -m "docs(job-perks): publish integration and database contract"
```

**Failure risks:** The existing docs already use a flat set of named sections rather than a generic `database:` mapping, and the actual API is record/interface based. Do not paste illustrative YAML or factory snippets if source inspection disproves them. Do not alter unrelated living-spec horizons or changelog entries.

---

## Task 6: Separate API/Paper artifacts and CI assertions

**Purpose:** Make the release boundary executable: API and Paper artifacts are separate, classfile targets are correct, MapGUI is external, package duplication is rejected, and CI runs all final-integration gates in dependency order.

**Files:**

- Modify: `build.gradle.kts` only for existing publication/toolchain assertions; it already sets API/common release 21 and Paper release 25 and publishes `modularjobs-api`/`modularjobs-common`.
- Modify: `api/build.gradle.kts` only for API artifact verification/publication metadata.
- Modify: `paper/build.gradle.kts` only for Shadow output/package assertions and `runServerSmoke` dependency.
- Modify: `.github/workflows/ci.yml`.
- Modify: `scripts/test-package-release-assets.sh` if the new API assertion belongs beside existing package tests.
- Create: `scripts/assert-job-perks-artifacts.sh`.
- Modify: `scripts/package-release-assets.sh` only if separate API artifact publication requires a tested, backward-compatible release input; preserve existing four-argument invocation and asset names.

**Interfaces consumed:**

- Root `build.gradle.kts` toolchain logic: `moduleName == "paper"` → Java 25, all other modules → Java 21; `options.release.set(javaVersion)`.
- Root Maven publication artifact ID `modularjobs-$moduleName` for `api` and `common`.
- `paper/build.gradle.kts` Shadow task, `paper/build/libs/*-all.jar`, Craftux relocation, compile-only MapGUI API, PostgreSQL runtime dependency.
- `.github/workflows/ci.yml` PostgreSQL service on host port 55432, JDK setup, schema step, `check`, Shadow build, and artifact upload.
- Existing release script/test naming `modularjobs-paper-$VERSION.jar`, `modularjobs-postgres-$VERSION.sql`, `SHA256SUMS`.
- Modify: `scripts/test-package-release-assets.sh` only to add an API artifact assertion beside its existing `modularjobs-paper-2.0.0.jar`/`modularjobs-postgres-2.0.0.sql` checks.
**Interfaces produced:**

- `scripts/assert-job-perks-artifacts.sh api/build/libs/modularjobs-api-0.0.0-SNAPSHOT.jar paper/build/libs/paper-all.jar` exits 0 only when boundaries pass for a default local build.
- API classfiles major version 65; Paper classfiles major version 69.
- API artifact includes public API classes and excludes `net/aincraft/paper/` and `org/mapgui/`; Paper artifact includes plugin descriptor and no unrelocated MapGUI classes.
- CI runs schema application, unit tests, exact 19-case E2E, Paper smoke, docs checks, API jar, Paper Shadow jar, artifact assertions, and existing release asset tests.

- [ ] **Step 1: Write the failing package assertions.** Create `scripts/assert-job-perks-artifacts.sh` using `jar tf`, `javap`, and the repository’s existing shell style. Required behavior:

```bash
#!/usr/bin/env bash
set -euo pipefail

api_jar=${1:?API jar path is required}
paper_jar=${2:?Paper jar path is required}
[[ -f "$api_jar" ]] || { printf '%s\n' "missing API jar: $api_jar" >&2; exit 1; }
[[ -f "$paper_jar" ]] || { printf '%s\n' "missing Paper jar: $paper_jar" >&2; exit 1; }

api_entries=$(jar tf "$api_jar")
paper_entries=$(jar tf "$paper_jar")
[[ "$(printf '%s\n' "$api_entries" | while read -r line; do [[ "$line" == "net/aincraft/Job.class" ]] && echo x; done | wc -l)" -eq 1 ]]
! printf '%s\n' "$api_entries" | while read -r line; do [[ "$line" == net/aincraft/paper/* ]] && exit 0; done
! printf '%s\n' "$api_entries" | while read -r line; do [[ "$line" == org/mapgui/* ]] && exit 0; done
printf '%s\n' "$paper_entries" | while read -r line; do [[ "$line" == plugin.yml ]] && exit 0; done
printf '%s\n' 'ARTIFACT_ASSERTIONS_PASS'
```

The final script may use a safer Python/JAR reader if shell pipeline exit semantics make the negative checks unreliable, but it must fail closed and emit the same marker. Add an assertion that `api/build/libs/modularjobs-api-0.0.0-SNAPSHOT.jar` has major version 65 and `paper/build/libs/paper-all.jar` has major version 69.

- [ ] **Step 2: Run focused failure.** Run:

```bash
./gradlew :api:jar :paper:shadowJar
./scripts/assert-job-perks-artifacts.sh \
  api/build/libs/modularjobs-api-0.0.0-SNAPSHOT.jar \
  paper/build/libs/paper-all.jar
```

Expected initial result: `FAIL` if the API jar has not been built, if implementation/MapGUI packages are present, if `plugin.yml` is absent, or if classfile versions are wrong. The default local artifact path is `api/build/libs/modularjobs-api-0.0.0-SNAPSHOT.jar`; release CI must substitute the explicit `releaseVersion` path before invoking the assertion, never a wildcard.

- [ ] **Step 3: Implement minimal Gradle and CI assertions.** Preserve current root toolchains and publication. Add explicit artifact tasks only if existing task outputs do not provide the required files. Keep `io.github.flog99:mapgui-api:1.0.0` compile-only and do not shade it. Keep API/common Maven publication separate from Paper Shadow. In `.github/workflows/ci.yml`, add ordered named steps after PostgreSQL health/schema setup:

```yaml
- name: API and common unit tests
  run: ./gradlew :api:test :common:test --console=plain --no-daemon
- name: Paper unit tests
  run: ./gradlew :paper:test --console=plain --no-daemon
- name: Upgrade graph persistence E2E
  run: ./gradlew :paper:integrationTest --tests 'net.aincraft.upgrade.UpgradeGraphPersistenceE2ETest' --console=plain --no-daemon
- name: Paper job perks smoke
  run: ./gradlew :paper:runServerSmoke --console=plain --no-daemon
- name: Documentation contract
  run: ./scripts/check-docs.sh
- name: Build separated artifacts
  run: ./gradlew :api:jar :paper:shadowJar --console=plain --no-daemon
- name: Assert separated artifacts
  run: ./scripts/assert-job-perks-artifacts.sh api/build/libs/modularjobs-api-0.0.0-SNAPSHOT.jar paper/build/libs/paper-all.jar
- name: Package release asset tests
  run: ./scripts/test-package-release-assets.sh
```

Replace the literal API version with the CI project version expression if `releaseVersion` is always supplied; the generated command must resolve to an exact file and must be tested locally. Preserve existing Craftux checkout/publication steps and the existing release job’s `paper-all` artifact dependency.

- [ ] **Step 4: Run packaging and CI-equivalent pass.** Run these exact checks locally with the actual resolved version:

```bash
./gradlew :api:jar :paper:shadowJar
./scripts/assert-job-perks-artifacts.sh api/build/libs/modularjobs-api-0.0.0-SNAPSHOT.jar paper/build/libs/paper-all.jar
javap -verbose -classpath api/build/libs/modularjobs-api-0.0.0-SNAPSHOT.jar net.aincraft.Job | grep 'major version'
javap -verbose -classpath paper/build/libs/paper-all.jar net.aincraft.PluginContext | grep 'major version'
./scripts/test-package-release-assets.sh
./gradlew :api:test :common:test :paper:test
./gradlew :paper:integrationTest --tests 'net.aincraft.upgrade.UpgradeGraphPersistenceE2ETest'
./gradlew :paper:runServerSmoke
./scripts/check-docs.sh
```

Expected pass: API `major version: 65`; Paper `major version: 69`; artifact script emits `ARTIFACT_ASSERTIONS_PASS`; package script emits `package release assets tests: PASS`; all unit tests pass; E2E reports exactly 19 completed/0 failed; smoke emits `JOB_PERKS_SMOKE_PASS`; docs pass. If `net.aincraft.PluginContext` is not packaged in the artifact under that exact class name, inspect `plugin.yml` and choose the actual Paper class while retaining Java 25 assertion.

- [ ] **Step 5: Commit exactly this slice.**

```bash
git add build.gradle.kts api/build.gradle.kts paper/build.gradle.kts \
  .github/workflows/ci.yml scripts/assert-job-perks-artifacts.sh \
  scripts/test-package-release-assets.sh scripts/package-release-assets.sh
git commit -m "ci(release): assert separated job perks artifacts"
```

**Failure risks:** Current CI already applies SQL and builds Shadow; duplicate steps can create ordering confusion. Extend the existing `java` job rather than adding a parallel job that lacks the PostgreSQL service. Keep release job names and artifact upload paths stable. Do not claim a separate Paper Maven artifact if the project only publishes API/common; the separate plugin artifact is the Shadow jar and the separate public dependency is `modularjobs-api`.

---

## Task 7: Final release-equivalent verification and working-tree safety

**Purpose:** Verify all affected artifacts end to end after the six implementation commits, while proving unrelated working-tree changes were not staged or modified.

**Files:**

- No production edits are permitted in this task.
- Read/execute only: all files named by Tasks 1–6, especially `paper/src/main/resources/sql/postgres.sql`, `scripts/apply-postgres-schema.sh`, `scripts/assert-job-perks-artifacts.sh`, `scripts/check-docs.sh`, `paper/build/libs/paper-all.jar`, and `api/build/libs/modularjobs-api-0.0.0-SNAPSHOT.jar` for the default local build.

**Interfaces consumed:** Every produced contract from Tasks 1–6: external schema fixture, classloader test, 19-test E2E class, smoke command, docs assertions, separate artifacts, and CI-equivalent command ordering.

**Interfaces produced:** A release evidence record containing exact command output/status, artifact paths, classfile versions, E2E count, smoke marker, docs/schema result, and a clean staged-diff scope. No code or documentation changes are allowed as a side effect.

- [ ] **Step 1: Capture initial working-tree safety evidence.** Run:

```bash
git status --short
```

Record existing unrelated paths mentally/output-only; do not stage or reset them. Do not use `git clean`, `git reset --hard`, `git checkout --`, or a broad formatter.

- [ ] **Step 2: Run the complete release-equivalent sequence.** With PostgreSQL service available and schema applied externally, run:

```bash
./gradlew :api:test :common:test :paper:test :api:javadoc :api:jar :paper:shadowJar --console=plain --no-daemon
./scripts/apply-postgres-schema.sh postgres://test:test@127.0.0.1:55432/modularjobs
./scripts/assert-job-perks-artifacts.sh \
  api/build/libs/modularjobs-api-0.0.0-SNAPSHOT.jar \
  paper/build/libs/paper-all.jar
./gradlew :paper:integrationTest --tests 'net.aincraft.upgrade.UpgradeGraphPersistenceE2ETest' --console=plain --no-daemon
./gradlew :paper:runServerSmoke --console=plain --no-daemon
./scripts/check-docs.sh
./scripts/test-package-release-assets.sh
```

The default local verification uses `api/build/libs/modularjobs-api-0.0.0-SNAPSHOT.jar`; release CI resolves the exact filename from its explicit `releaseVersion`. Expected results are: all Gradle commands exit `0`; all 19 E2E methods run with `0 failed`; smoke prints `JOB_PERKS_SMOKE_PASS`; artifact assertions print `ARTIFACT_ASSERTIONS_PASS`; package tests print `package release assets tests: PASS`; docs/schema checks exit `0`; API is major version 65 and Paper is major version 69.

- [ ] **Step 3: Verify sibling regression coverage.** Run the full affected module tests rather than only the new test:

```bash
./gradlew :api:test :common:test :paper:test --console=plain --no-daemon
```

Expected result: `BUILD SUCCESSFUL`, including existing `PostgresSchemaFidelityTest`, `SchemaPresenceTest`, `SchemaPolicyTest`, `DatabaseConfigSectionsTest`, `PluginResourcesLifecycleTest`, `SkillTreeTest`, `UpgradesCommandTest`, and `BootstrapLifecycleTest`. A new test pass with a sibling regression is not completion.

- [ ] **Step 4: Verify staged scope after each atomic commit.** For each of the six implementation commits, inspect:

```bash
git show --stat --oneline HEAD
```

The commit must contain only the task’s named files and one subject line matching its atomic commit. At final handoff, run:

```bash
git status --short
git diff --check
```

Expected result: no whitespace errors; unrelated pre-existing working-tree paths remain unstaged and unmodified. If any unrelated path changed, stop claiming completion and restore only the task’s own accidental change using a surgical edit; never reset the entire worktree.

- [ ] **Step 5: Commit only if a verification-only adjustment is genuinely required.** Normally this task creates no commit. If and only if a release command reveals a missing assertion that is part of the already-approved final integration scope, return to the owning task, write its failing assertion, make one minimal correction, rerun the complete sequence, and create a seventh atomic commit with subject `test(release): close final integration verification gap`. Do not make an untested documentation or packaging tweak here.

**Final acceptance:** The six required implementation commits are atomic; no earlier component was reimplemented; PostgreSQL remains connect-only; two-plugin classloading/install checks pass; all 19 graph/persistence E2E cases pass; Paper 26.2 smoke passes; docs/catalog/config/API/database/changelog agree with actual symbols and files; API/Paper artifacts are separate and classfile/package assertions pass; CI runs the same gates; and unrelated working-tree changes are untouched.
