# Server-development skill alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align ModularJobs with `server-development-skills` pins, CI, nightly, and (in wave 2) fail-closed Google style plus the Fumadocs docs gate, without changing MySQL connect-only schema ownership or `YY.M.D.REVISION`.

**Architecture:** Wave 1 is mechanical (catalog pins, wrapper, `plugin.yml`, Hikari prep-stmt cache, Action majors, rolling nightly, LICENSE/badges) and keeps analyzers report-only so CI stays green. Wave 2 turns `./gradlew clean check` into a fail-closed Spotless + Google Checks + PMD + SpotBugs gate and wires `web/fumadocs` `npm test` to `verify-docs.mjs`.

**Tech Stack:** Gradle 9.7.1, Java 25, Paper 26.2 (`26.2.build.65-beta`), HikariCP 7.0.2, GitHub Actions checkout@v7 / setup-java@v5 / setup-gradle@v6, Spotless 8.10.0, Checkstyle 13.11.0, PMD 7.26.0, SpotBugs 4.9.7, Fumadocs 16.14.5 / Next 16.3.2.

**Spec:** `docs/superpowers/specs/2026-08-21-server-skill-alignment-design.md`

**Wave 1 status:** landed on `chore/server-skill-alignment`. Verified `./gradlew :api:test :common:test :paper:test` and `./gradlew clean check` (analyzers still report-only). Packaged `plugin.yml` has `api-version: '26.2'`. Wave 2 not started.

## Global constraints

- Do **not** switch to CalVer `YYYY.MM.DD.<GITHUB_RUN_NUMBER>`. Keep `-PreleaseVersion=` and `scripts/validate-release-version.sh`.
- Do **not** add SQLite, in-process `CREATE TABLE`, or change `SchemaPolicy`.
- Keep `paper-api` at `26.2.build.65-beta`.
- Keep modules `api` / `common` / `paper` and package `dev.mintychochip`.
- Do not add Azalea, do not delete `web/fumadocs`, do not add a second `content/docs/` at repo root.
- Atomic commits per task. Wave 1 must leave `./gradlew :api:test :common:test :paper:test` green.

## File structure (end of Wave 1)

| Path | Role |
|------|------|
| `gradle/libs.versions.toml` | run-paper 3.1.0, checkstyle 13.11.0, spotbugs-tool 4.9.7, hikari 7.0.2 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 9.7.1 |
| `build.gradle.kts` | Java 25 for every subproject |
| `paper/src/main/resources/plugin.yml` | `api-version: '26.2'` |
| `paper/.../HikariConfigProvider.java` | MySQL `cachePrepStmts` / `prepStmtCacheSize` |
| `.github/workflows/ci.yml` | v7/v5/v6, JDK 25 only, `clean check`, `contents: read` |
| `.github/workflows/nightly.yml` | rolling `nightly` pre-release of `paper/build/libs/*-all.jar` |
| `LICENSE` | MIT |
| `README.md` | shields.io badges on `master` |
| `api/src/test/java/dev/mintychochip/CiTemplateHookupTest.java` | Action majors, `clean check`, nightly schedule-only |
| `paper/src/test/java/dev/mintychochip/PluginYmlProductionReadinessTest.java` | `api-version` equals `26.2` |
| `paper/src/test/java/dev/mintychochip/repository/HikariConfigProviderTest.java` | prep-stmt cache properties |

## File structure (added in Wave 2)

| Path | Role |
|------|------|
| `build.gradle.kts` | Spotless 8.10.0, `google_checks.xml`, `ignoreFailures=false`, `check` depends on analyzers + Spotless |
| `web/fumadocs/scripts/verify-docs.mjs` | copy of skill `verify-docs.mjs` |
| `web/fumadocs/package.json` | `npm test` + pinned Fumadocs/Next |
| `web/fumadocs/app/global.css` | import order `tailwindcss` → `neutral.css` → `preset.css` |
| `web/fumadocs/content/docs/index.mdx` | ladder labels |
| `web/fumadocs/content/docs/guides/getting-started.mdx` | ladder labels |

---

### Task 1: Fail tests for Wave 1 contracts

**Files:**
- Modify: `paper/src/test/java/dev/mintychochip/PluginYmlProductionReadinessTest.java`
- Create: `paper/src/test/java/dev/mintychochip/repository/HikariConfigProviderTest.java`
- Modify: `api/src/test/java/dev/mintychochip/CiTemplateHookupTest.java`

- [x] **Step 1: Tighten plugin.yml api-version assertion**

In `PluginYmlProductionReadinessTest`, replace the `1.13` check with:

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
```

and:

```java
      String api = yml.getString("api-version");
      assertEquals("26.2", api, "api-version must be Paper 26.2, got " + api);
```

- [ ] **Step 2: Add Hikari prep-stmt cache test**

Create `paper/src/test/java/dev/mintychochip/repository/HikariConfigProviderTest.java`:

```java
package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zaxxer.hikari.HikariConfig;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

class HikariConfigProviderTest {

  @Test
  void mysqlConfigEnablesPrepStmtCache() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("jdbc-url", "jdbc:mysql://127.0.0.1:3306/modularjobs");
    section.set("username", "modularjobs");
    section.set("password", "secret");
    section.set("maximum-pool-size", 10);

    HikariConfig config = new HikariConfigProvider(section, DatabaseType.MYSQL).create();

    assertEquals("true", config.getDataSourceProperties().getProperty("cachePrepStmts"));
    assertEquals("250", config.getDataSourceProperties().getProperty("prepStmtCacheSize"));
  }
}
```

- [ ] **Step 3: Extend CI hookup tests**

Add these methods to `CiTemplateHookupTest` (keep existing tests):

```java
  @Test
  void ciUsesPinnedActionMajorsAndCleanCheck() throws IOException {
    String text = Files.readString(Path.of(requiredProperty("ci.workflow")));
    assertTrue(text.contains("actions/checkout@v7"), "checkout must be @v7");
    assertTrue(text.contains("actions/setup-java@v5"), "setup-java must be @v5");
    assertTrue(text.contains("gradle/actions/setup-gradle@v6"), "setup-gradle must be @v6");
    assertTrue(text.contains("./gradlew clean check"), "java gate must be clean check");
    assertFalse(
        text.contains("java-version: \"21\""),
        "CI must not install JDK 21 after the Java 25-only cutover");
  }

  @Test
  void nightlyIsScheduleOrManualAndPublishesShadowJar() throws IOException {
    Path nightly =
        Path.of(requiredProperty("project.root")).resolve(".github/workflows/nightly.yml");
    assertTrue(Files.isRegularFile(nightly), "missing " + nightly);
    String text = Files.readString(nightly);
    assertTrue(text.contains("cron: '0 4 * * *'"), "nightly must schedule at 04:00 UTC");
    assertTrue(text.contains("workflow_dispatch"), "nightly must be manually dispatchable");
    assertTrue(text.contains("gh release create nightly"), "must replace rolling nightly tag");
    assertTrue(text.contains("paper/build/libs/*-all.jar"), "must publish paper shadow jar");
    assertTrue(isScheduleOrManualOnly(text), "nightly must not run on push/PR/tag");
  }
```

- [ ] **Step 4: Run tests and confirm they fail for the right reasons**

```bash
./gradlew :paper:test --tests 'dev.mintychochip.PluginYmlProductionReadinessTest' --tests 'dev.mintychochip.repository.HikariConfigProviderTest'
./gradlew :api:test --tests 'dev.mintychochip.CiTemplateHookupTest'
```

Expected: plugin.yml test fails (`1.21` != `26.2`); Hikari test fails (property null); CI tests fail (v4 SHA / missing nightly / no `clean check`).

- [ ] **Step 5: Commit**

```bash
git add paper/src/test/java/dev/mintychochip/PluginYmlProductionReadinessTest.java \
  paper/src/test/java/dev/mintychochip/repository/HikariConfigProviderTest.java \
  api/src/test/java/dev/mintychochip/CiTemplateHookupTest.java
git commit -m "test: encode server-skill wave 1 contracts"
```

---

### Task 2: Toolchain pins and Java 25

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (subproject `javaVersion` branch)
- Modify: `gradle/wrapper/gradle-wrapper.properties` (via wrapper task)
- Modify: `gradlew`, `gradlew.bat` if the wrapper task rewrites them

- [ ] **Step 1: Bump catalog pins**

In `gradle/libs.versions.toml`:

```toml
run-paper = "3.1.0"
spotbugs-tool = "4.9.7"
checkstyle = "13.11.0"
hikari = "7.0.2"
```

Leave `paper = "26.2.build.65-beta"`, `pmd = "7.26.0"`, `spotbugs-gradle = "6.5.10"`.

- [ ] **Step 2: Java 25 for every module**

In `build.gradle.kts` replace:

```kotlin
    val moduleName = name
    val javaVersion = if (moduleName == "paper") 25 else 21
```

with:

```kotlin
    val moduleName = name
    val javaVersion = 25
```

- [ ] **Step 3: Regenerate Gradle wrapper 9.7.1**

```bash
./gradlew wrapper --gradle-version 9.7.1 --distribution-type bin
```

Confirm `gradle/wrapper/gradle-wrapper.properties` contains:

```
distributionUrl=https\://services.gradle.org/distributions/gradle-9.7.1-bin.zip
```

- [ ] **Step 4: Compile to prove pins resolve**

```bash
./gradlew :api:compileJava :common:compileJava :paper:compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts gradle/wrapper gradle/wrapper/gradle-wrapper.jar gradlew gradlew.bat
git commit -m "chore: pin Gradle 9.7.1, Java 25, run-paper 3.1.0, analyzers, Hikari 7.0.2"
```

---

### Task 3: plugin.yml api-version 26.2

**Files:**
- Modify: `paper/src/main/resources/plugin.yml`

- [ ] **Step 1: Change api-version**

Replace `api-version: '1.21'` with:

```yaml
api-version: '26.2'
```

Keep the quotes. Do not change `version: '${version}'`.

- [ ] **Step 2: Re-run plugin.yml test**

```bash
./gradlew :paper:test --tests 'dev.mintychochip.PluginYmlProductionReadinessTest'
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add paper/src/main/resources/plugin.yml
git commit -m "fix: declare plugin.yml api-version 26.2"
```

---

### Task 4: Hikari MySQL prep-stmt cache

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/repository/HikariConfigProvider.java`

- [ ] **Step 1: Set data source properties after credentials**

After `hikariConfig.setPassword(password);` add:

```java
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
```

Do not add SQLite branches, `PRAGMA`, or DDL.

- [ ] **Step 2: Re-run Hikari test**

```bash
./gradlew :paper:test --tests 'dev.mintychochip.repository.HikariConfigProviderTest' --tests 'dev.mintychochip.repository.SqlStatementsHikariRepositoryTest'
```

Expected: PASS (`SqlStatementsHikariRepositoryTest` skips if MySQL is down).

- [ ] **Step 3: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/repository/HikariConfigProvider.java
git commit -m "fix: enable Hikari MySQL prepared-statement cache"
```

---

### Task 5: CI Action majors, JDK 25, clean check

**Files:**
- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1: Workflow permissions**

Replace the top-level permissions block with:

```yaml
permissions:
  contents: read
```

Do not keep workflow-level `contents: write` or `packages: write`. No job in this file publishes packages.

- [ ] **Step 2: Replace every checkout / setup-java / setup-gradle pin**

Use these exact tags (not SHAs):

```yaml
        uses: actions/checkout@v7
```

```yaml
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "25"
```

```yaml
        uses: gradle/actions/setup-gradle@v6
```

Apply to `api-build`, `java`, `rest-api` (`checkout` only), and `session-editor` (`checkout` only). Sibling-repo checkouts (craftux, mint, databag, conditions, Preferences) also use `actions/checkout@v7`.

- [ ] **Step 3: api-build and java jobs use JDK 25 only**

Delete the `Set up JDK 21` steps. `api-build` must set up JDK 25 (with `cache: gradle` on the java job as today).

- [ ] **Step 4: Java job runs clean check**

Replace the Check step with:

```yaml
      - name: Check (compile + Error Prone + unit tests + static analysis)
        # quality.fail not set in wave 1 → Checkstyle/PMD/SpotBugs report-only
        run: ./gradlew clean check -Dmaven.repo.local=${{ runner.temp }}/m2 --console=plain --no-daemon
```

Keep MySQL service, schema apply, sibling publishes, shadow jar, and report uploads.

Leave `actions/upload-artifact` on its existing SHA. Leave `dtolnay/rust-toolchain` and `actions/setup-node` pins unchanged.

- [ ] **Step 5: Re-run CI hookup tests that do not need nightly.yml yet**

```bash
./gradlew :api:test --tests 'dev.mintychochip.CiTemplateHookupTest.consumerWorkflowBuildsAndUploadsApiArtifact' --tests 'dev.mintychochip.CiTemplateHookupTest.ciUsesPinnedActionMajorsAndCleanCheck' --tests 'dev.mintychochip.CiTemplateHookupTest.leftoverLocalJobsDoNotPublishOnPushPrOrTag'
```

Expected: the two existing tests plus `ciUsesPinnedActionMajorsAndCleanCheck` PASS. `nightlyIsScheduleOrManualAndPublishesShadowJar` still fails until Task 6.

- [ ] **Step 6: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: pin Actions v7/v5/v6, JDK 25, and clean check"
```

---

### Task 6: Rolling nightly release

**Files:**
- Create: `.github/workflows/nightly.yml`

- [ ] **Step 1: Write nightly.yml**

Create `.github/workflows/nightly.yml` with this content. Copy sibling checkout refs from current `ci.yml` (craftux `402bea8ce21847df632a10b00b563665db205de9`, mint `873fd3e359d3f3ac5b22fc84bc9aaae2a6adabdd`, Preferences `c78eac8c81104ba0890fc98d5f49090d90d0dee7`, databag `373f7d0991e121afbcccf32015105010cafab2be`, conditions `7d30d892b66b7ab440edd881105b1c4ad4ab4c0e`). If `ci.yml` pins moved, copy whatever `ci.yml` currently uses — do not invent new SHAs.

```yaml
name: Nightly release

on:
  schedule:
    - cron: '0 4 * * *' # 04:00 UTC every day
  workflow_dispatch:

concurrency:
  group: nightly
  cancel-in-progress: true

permissions:
  contents: write

jobs:
  nightly:
    runs-on: ubuntu-latest
    timeout-minutes: 45
    services:
      mysql:
        image: mysql:8.4
        env:
          MYSQL_USER: test
          MYSQL_PASSWORD: test
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: modularjobs
        ports:
          - 13306:3306
        options: >-
          --health-cmd "mysqladmin ping -h 127.0.0.1 -u root -proot"
          --health-interval 5s
          --health-timeout 5s
          --health-retries 10
    env:
      GITHUB_ACTOR: ${{ github.actor }}
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      MODULARJOBS_TEST_MYSQL_URL: jdbc:mysql://localhost:13306/modularjobs
      MODULARJOBS_TEST_MYSQL_USER: test
      MODULARJOBS_TEST_MYSQL_PASSWORD: test
    steps:
      - name: Checkout
        uses: actions/checkout@v7

      - name: Set up JDK 25
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "25"
          cache: gradle

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Checkout craftux dependency (pinned release)
        uses: actions/checkout@v7
        with:
          repository: aincraft-org/craftux
          ref: 402bea8ce21847df632a10b00b563665db205de9
          path: craftux
          fetch-depth: 1

      - name: Link craftux local Maven repository
        run: ln -s "$GITHUB_WORKSPACE/craftux" "$GITHUB_WORKSPACE/../craftux"

      - name: Set up Rust for craftux views
        uses: dtolnay/rust-toolchain@4360b52568e2003a75bf9bc1d59f33a8e3fc893c

      - name: Publish craftux local Maven artifacts
        working-directory: craftux
        run: |
          chmod +x gradlew
          ./gradlew \
            :api:publishMavenPublicationToLocalBuildRepository \
            :common:publishMavenPublicationToLocalBuildRepository \
            :paper:publishMavenPublicationToLocalBuildRepository \
            --console=plain --no-daemon

      - name: Require Mint read-only deploy key
        env:
          MINT_DEPLOY_KEY: ${{ secrets.MINT_DEPLOY_KEY }}
        run: |
          if [[ -z "$MINT_DEPLOY_KEY" ]]; then
            echo "::error::ModularJobs needs the repository-scoped read-only MINT_DEPLOY_KEY for aincraft-org/mint."
            exit 1
          fi

      - name: Checkout mint dependency (private, pinned source)
        uses: actions/checkout@v7
        with:
          repository: aincraft-org/mint
          ref: 873fd3e359d3f3ac5b22fc84bc9aaae2a6adabdd
          path: mint
          fetch-depth: 1
          ssh-key: ${{ secrets.MINT_DEPLOY_KEY }}
          persist-credentials: false

      - name: Link mint local Maven repository
        run: ln -s "$GITHUB_WORKSPACE/mint" "$GITHUB_WORKSPACE/../mint"

      - name: Publish mint API to local Maven repository
        working-directory: mint
        run: |
          chmod +x gradlew
          ./gradlew \
            :api:publishMavenPublicationToLocalBuildRepository \
            --console=plain --no-daemon

      - name: Checkout Preferences API dependency
        uses: actions/checkout@v7
        with:
          repository: mintychochip/Preferences
          ref: c78eac8c81104ba0890fc98d5f49090d90d0dee7
          path: preferences
          fetch-depth: 1

      - name: Publish Preferences API to isolated Maven local
        working-directory: preferences
        run: |
          chmod +x gradlew
          ./gradlew :api:publishToMavenLocal \
            -Dmaven.repo.local=${{ runner.temp }}/m2 \
            --console=plain --no-daemon

      - name: Checkout databag dependency
        uses: actions/checkout@v7
        with:
          repository: mintychochip/databag
          ref: 373f7d0991e121afbcccf32015105010cafab2be
          path: databag
          fetch-depth: 1

      - name: Link databag local Maven repository
        run: ln -s "$GITHUB_WORKSPACE/databag" "$GITHUB_WORKSPACE/../databag"

      - name: Publish databag local Maven artifacts
        working-directory: databag
        run: |
          chmod +x gradlew
          ./gradlew publishAllPublicationsToLocalBuildRepoRepository --console=plain --no-daemon

      - name: Checkout conditions dependency
        uses: actions/checkout@v7
        with:
          repository: aincraft-org/conditions
          ref: 7d30d892b66b7ab440edd881105b1c4ad4ab4c0e
          path: conditions
          fetch-depth: 1

      - name: Link conditions local Maven repository
        run: ln -s "$GITHUB_WORKSPACE/conditions" "$GITHUB_WORKSPACE/../conditions"

      - name: Publish conditions local Maven artifacts
        working-directory: conditions
        run: |
          chmod +x gradlew
          ./gradlew publishAllPublicationsToLocalBuildRepoRepository --console=plain --no-daemon

      - name: Verify required local dependency artifacts
        run: |
          test -f ../craftux/build/maven-repo/dev/craftux/craftux-paper/1.0.2/craftux-paper-1.0.2.pom
          test -n "$(find ../databag/build/maven-repo/dev/databag/databag/0.0.0-SNAPSHOT -name 'databag-*.pom' -print -quit)"
          test -n "$(find ../conditions/build/maven-repo/dev/conditions/api/0.0.0-SNAPSHOT -name 'api-*.pom' -print -quit)"

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Apply MySQL schema (for MySQL-backed unit tests)
        run: |
          ./scripts/apply-mysql-schema.sh \
            "mysql://test:test@127.0.0.1:13306/modularjobs"

      - name: Check
        run: ./gradlew clean check -Dmaven.repo.local=${{ runner.temp }}/m2 --console=plain --no-daemon

      - name: Shadow jar
        run: ./gradlew :paper:shadowJar -Dmaven.repo.local=${{ runner.temp }}/m2 --console=plain --no-daemon

      - name: Replace rolling nightly release
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          gh release delete nightly --cleanup-tag --yes || true
          gh release create nightly paper/build/libs/*-all.jar \
            --prerelease \
            --title "Nightly $(date -u +%F)" \
            --notes "Automated nightly build from ${GITHUB_SHA}."
```

Do not add `on.push` or `on.pull_request`. Do not use skill CalVer for the jar version.

- [ ] **Step 2: Run full CiTemplateHookupTest**

```bash
./gradlew :api:test --tests 'dev.mintychochip.CiTemplateHookupTest'
```

Expected: PASS, including `nightlyIsScheduleOrManualAndPublishesShadowJar` and `leftoverLocalJobsDoNotPublishOnPushPrOrTag`.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/nightly.yml
git commit -m "ci: add rolling nightly Paper shadow-jar pre-release"
```

---

### Task 7: LICENSE and README badges

**Files:**
- Create: `LICENSE`
- Modify: `README.md`
- Modify: `AGENTS.md` (toolchain / CI sentences only)

- [ ] **Step 1: Add MIT LICENSE**

Create `LICENSE`:

```text
MIT License

Copyright (c) 2026 ModularJobs contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

- [ ] **Step 2: README badges and CI sentence**

Insert immediately under `# ModularJobs`:

```markdown
[![Build](https://img.shields.io/github/actions/workflow/status/aincraft-org/modularjobs/ci.yml?branch=master&label=build)](https://github.com/aincraft-org/modularjobs/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/aincraft-org/modularjobs)](LICENSE)
[![Release](https://img.shields.io/github/v/release/aincraft-org/modularjobs)](https://github.com/aincraft-org/modularjobs/releases/latest)
![Platform](https://img.shields.io/badge/Paper-26.2-blue)
```

Replace the CI sentence with:

```markdown
CI: `.github/workflows/ci.yml` — Java 25 + MySQL 8 (`./gradlew clean check` + shadow jar), Rust rest-api, React session-editor. Nightly Paper jar: `.github/workflows/nightly.yml`.
```

- [ ] **Step 3: AGENTS.md toolchain lines**

Change the opening toolchain sentence from `Java 21 / 25` to `Java 25`. Change:

```
`./gradlew check` — reports under `*/build/reports/{checkstyle,pmd,spotbugs}/`.
```

to mention `./gradlew clean check` as the CI gate (Wave 1 still documents `-Pquality.fail=true` as the optional fail-on-findings switch). Mention `.github/workflows/nightly.yml`.

- [ ] **Step 4: Commit**

```bash
git add LICENSE README.md AGENTS.md
git commit -m "docs: add MIT license, README badges, and nightly CI notes"
```

---

### Task 8: Wave 1 verification

**Files:** none except living-spec checkboxes if verification passes.

- [ ] **Step 1: Run unit tests**

```bash
./gradlew :api:test :common:test :paper:test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run clean check (report-only analyzers)**

```bash
./gradlew clean check
```

Expected: `BUILD SUCCESSFUL`. Checkstyle/PMD/SpotBugs may still warn; they must not fail the build.

- [ ] **Step 3: Confirm wrapper and packaged api-version**

```bash
grep 9.7.1 gradle/wrapper/gradle-wrapper.properties
./gradlew :paper:processResources
grep "api-version" paper/build/resources/main/plugin.yml
```

Expected: wrapper URL is 9.7.1; processed plugin.yml contains `api-version: '26.2'` (or `26.2` as a string).

- [ ] **Step 4: Flip living-spec Wave 1 checkbox**

In `docs/living-specs/modularjobs.md` mark:

```markdown
- [x] Server-skill alignment Wave 1: toolchain pins, `api-version` 26.2, CI action majors, nightly, Hikari 7.0.2 (`docs/superpowers/specs/2026-08-21-server-skill-alignment-design.md`)
```

- [ ] **Step 5: Commit living-spec progress**

```bash
git add docs/living-specs/modularjobs.md docs/superpowers/plans/2026-08-21-server-skill-alignment.md
git commit -m "docs: record wave 1 server-skill alignment as done"
```

Stop here until Wave 1 is green. Do not start Wave 2 in the same unreviewed dump if `clean check` failed.

---

### Task 9: Wave 2 — Spotless + fail-closed analyzers

**Files:**
- Modify: `build.gradle.kts`
- Modify: `gradle/libs.versions.toml` (add spotless plugin)
- Many Java files (via `spotlessApply` only — do not hand-format)

- [ ] **Step 1: Add Spotless plugin to the version catalog**

In `gradle/libs.versions.toml` `[versions]`:

```toml
spotless = "8.10.0"
```

In `[plugins]`:

```toml
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
```

- [ ] **Step 2: Apply Spotless and fail-closed analyzers in root `build.gradle.kts`**

Add to the root `plugins {}` block:

```kotlin
    alias(libs.plugins.spotless) apply false
```

Inside `subprojects {`, after `apply(plugin = "com.github.spotbugs")`:

```kotlin
    apply(plugin = "com.diffplug.spotless")
```

Replace the `qualityFail` / `qualityIgnoreFailures` block with hard failures:

```kotlin
    configure<CheckstyleExtension> {
        toolVersion = rootProject.libs.versions.checkstyle.get()
        config = resources.text.fromUri(
            "https://raw.githubusercontent.com/checkstyle/checkstyle/checkstyle-13.11.0/src/main/resources/google_checks.xml"
        )
        isIgnoreFailures = false
        maxWarnings = 0
        isShowViolations = true
    }

    configure<PmdExtension> {
        toolVersion = rootProject.libs.versions.pmd.get()
        isConsoleOutput = true
        ruleSetFiles = files(qualityConfig.file("pmd/ruleset.xml"))
        ruleSets = emptyList()
        isIgnoreFailures = false
        threads = Runtime.getRuntime().availableProcessors().coerceAtMost(4)
    }

    configure<SpotBugsExtension> {
        toolVersion.set(rootProject.libs.versions.spotbugs.tool)
        ignoreFailures.set(false)
        showStackTraces.set(true)
        showProgress.set(false)
        effort.set(Effort.MORE)
        reportLevel.set(Confidence.MEDIUM)
        excludeFilter.set(qualityConfig.file("spotbugs/exclude.xml"))
    }
```

Add Spotless:

```kotlin
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat("1.36.1")
            target("src/**/*.java")
        }
    }
```

After the existing Checkstyle/PMD/SpotBugs report `configureEach` blocks, add:

```kotlin
    tasks.named("check") {
        dependsOn(tasks.withType<Checkstyle>())
        dependsOn(tasks.withType<Pmd>())
        dependsOn(tasks.withType<SpotBugsTask>())
        dependsOn(tasks.named("spotlessCheck"))
    }
```

Remove `configDirectory` / local `checkstyle.xml` from the Checkstyle extension (Google Checks URL is the only config). Keep `config/checkstyle/` in git until Wave 2 is green if other docs still mention it, then delete the unused XML in the same commit if nothing references it.

- [ ] **Step 3: Apply formatting**

```bash
./gradlew spotlessApply
```

- [ ] **Step 4: Fix remaining Checkstyle / PMD / SpotBugs findings until clean check is green**

```bash
./gradlew clean check
```

Expected: `BUILD SUCCESSFUL`. Do not re-enable `ignoreFailures`. Prefer fixing code over broadening `spotbugs/exclude.xml`. Do not weaken PMD rules to skip findings.

- [ ] **Step 5: Point CI at the fail-closed gate**

In `.github/workflows/ci.yml` and `nightly.yml`, keep `./gradlew clean check` and delete comments that say quality is report-only.

- [ ] **Step 6: Commit**

```bash
git add build.gradle.kts gradle/libs.versions.toml .github/workflows/ci.yml .github/workflows/nightly.yml
git add api common paper config
git commit -m "chore: fail-closed Spotless, Google Checks, PMD, and SpotBugs"
```

---

### Task 10: Wave 2 — Fumadocs docs gate

**Files:**
- Create: `web/fumadocs/scripts/verify-docs.mjs` (copy from `.agents/skills/docs-maintenance/references/verify-docs.mjs` or `.agents/skills/server-development-skills/docs-maintenance/references/verify-docs.mjs`)
- Modify: `web/fumadocs/package.json`
- Modify: `web/fumadocs/app/global.css`
- Modify: `web/fumadocs/content/docs/index.mdx`
- Modify: `web/fumadocs/content/docs/guides/getting-started.mdx`
- Other MDX files the gate names

- [ ] **Step 1: Copy the gate and wire npm test**

```bash
cp .agents/skills/docs-maintenance/references/verify-docs.mjs web/fumadocs/scripts/verify-docs.mjs
```

In `web/fumadocs/package.json` `scripts`:

```json
    "test": "node scripts/verify-docs.mjs --ladder=guides",
    "build": "next build",
```

Pin dependencies:

```json
    "fumadocs-core": "16.14.5",
    "fumadocs-mdx": "15.3.0",
    "fumadocs-ui": "npm:@fumadocs/base-ui@16.14.5",
    "next": "16.3.2"
```

Then `npm install` in `web/fumadocs` so `package-lock.json` updates.

- [ ] **Step 2: Fix CSS import order**

`web/fumadocs/app/global.css` must start with:

```css
@import 'tailwindcss';
@import 'fumadocs-ui/css/neutral.css';
@import 'fumadocs-ui/css/preset.css';
```

- [ ] **Step 3: Add ladder labels**

In `index.mdx` and `guides/getting-started.mdx`, add visible section headings or prose that include the exact words `basics`, `Everyday`, and `Advanced` (the gate is case-sensitive for Everyday/Advanced as documented in the skill). Open with the idea, not a command table. Keep MySQL-only guidance.

- [ ] **Step 4: Run the gate and build; fix every failure it prints**

```bash
cd web/fumadocs && npm test && npm run build
```

Expected: both succeed. Do not invent plugin behaviour.

- [ ] **Step 5: Flip living-spec Wave 2 checkbox and commit**

```markdown
- [x] Server-skill alignment Wave 2: Spotless + Google Checks fail-on-`check`, Fumadocs verify-docs gate
```

```bash
git add web/fumadocs docs/living-specs/modularjobs.md
git commit -m "docs: add Fumadocs verify-docs gate and ladder labels"
```

---

## Spec coverage (self-review)

| Spec requirement | Task |
|---|---|
| Gradle 9.7.1 | 2 |
| Java 25 all modules | 2 |
| run-paper 3.1.0, Checkstyle 13.11.0, SpotBugs 4.9.7, Hikari 7.0.2 | 2 |
| Keep paper-api 26.2.build.65-beta | 2 (do not bump) |
| plugin.yml api-version 26.2 | 3 |
| Hikari cachePrepStmts | 4 |
| No SQLite / no DDL | 4 (explicit non-change) |
| CI v7/v5/v6, JDK 25, clean check, contents:read | 5 |
| nightly.yml rolling pre-release | 6 |
| Keep YY.M.D.REVISION | 6 (no CalVer) |
| LICENSE + README badges | 7 |
| Wave 1 verification | 8 |
| Spotless + google_checks + fail-closed check | 9 |
| Fumadocs gate / pins / CSS order / ladder | 10 |
| Do not delete fumadocs / add repo-root content/docs | 10 |

No CalVer, Azalea, package rename, or SchemaPolicy changes appear in any task.
