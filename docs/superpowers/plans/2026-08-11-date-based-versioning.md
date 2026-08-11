# Date-Based Release Versioning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the unpublished `2.0.0` release metadata with the date-based version `26.8.11.1`, and make future releases use `vYY.M.D.REVISION` tags with matching validated artifacts.

**Architecture:** The root Gradle project version remains the canonical version. Paper resource processing expands that value into `plugin.yml`; a shared shell validator enforces the same version contract in local packaging and GitHub Actions; the release workflow derives every title and asset name from the validated tag.

**Tech Stack:** Gradle Kotlin DSL, Java 21/25, Paper plugin YAML, Bash, GitHub Actions, Maven publication, SHA-256 release checksums.

## Global Constraints

- Current version is exactly `26.8.11.1`.
- Release format is exactly `YY.M.D.REVISION`; Git tags add the `v` prefix.
- `YY` is exactly two decimal digits and maps to `20YY` for Gregorian calendar validation.
- `M` and `D` are one or two digits with no leading zero; `M` is `1..12`; `D` must be valid for the selected month and year, including leap-year February.
- `REVISION` is a positive decimal integer with no leading zero; the first release on a date uses `.1`, and same-day releases increment it.
- Builds record the version explicitly; they never derive it from the system clock.
- Published `v1.1.0` and `v1.2.0` remain unchanged.
- The unpublished local `v2.0.0` tag is neither released nor deleted by this work.
- Do not push tags, create a remote release, or modify unrelated existing work.
- Preserve the existing unstaged `UpgradeTreeLoader.java` change; it is outside this feature.
- Each implementation task ends with one atomic commit and a green task-specific check.

## File Map

- Create `scripts/validate-release-version.sh` — shared `YY.M.D.REVISION` parser and Gregorian date validator.
- Modify `scripts/package-release-assets.sh` — call the shared validator and keep descriptor/asset checks.
- Modify `scripts/test-package-release-assets.sh` — cover valid, malformed, range, leap-year, revision, and mismatch cases.
- Modify `build.gradle.kts` — set the canonical project version to `26.8.11.1`.
- Modify `paper/build.gradle.kts` — expand the canonical version into the packaged plugin descriptor.
- Modify `paper/src/main/resources/plugin.yml` — replace the duplicate literal with the resource token.
- Modify `.github/workflows/ci.yml` — accept validated date-version tags and derive release metadata dynamically.
- Modify `README.md`, `CHANGELOG.md`, and `docs/living-specs/professions.md` — update active current-release references.
- Do not modify dated implementation plans/design records that intentionally preserve historical `2.0.0` decisions.

---

### Task 1: Centralize release-version validation

**Files:**
- Create: `scripts/validate-release-version.sh`
- Modify: `scripts/package-release-assets.sh:9-17`
- Modify: `scripts/test-package-release-assets.sh:9-65`

**Interfaces:**
- Consumes: one positional argument, `YY.M.D.REVISION`.
- Produces: exit status `0` for a valid version, exit status `2` with an actionable stderr message for invalid usage or values.
- Package integration: `package-release-assets.sh` invokes `scripts/validate-release-version.sh "$VERSION"` before reading inputs or creating output files.

- [ ] **Step 1: Add failing packaging cases**

Change the fixture and expected output version in
`scripts/test-package-release-assets.sh` from `2.0.0` to `26.8.11.1`. Add these
invalid-version cases before the non-empty-directory check:

```bash
for invalid_version in \
    26.8.11 \
    26.13.11.1 \
    26.4.31.1 \
    26.2.29.1 \
    26.8.11.0 \
    26.08.11.1 \
    26.8.11.01; do
    if bash "$PACKAGER" "$invalid_version" "$VALID_JAR" "$SQL" \
        "$WORK_DIR/invalid-$invalid_version"; then
        echo "expected invalid release version to fail: $invalid_version" >&2
        exit 1
    fi
done
```
Before the output assertions, add a direct leap-year acceptance check for the shared validator:

```bash
if ! bash "$ROOT/scripts/validate-release-version.sh" 24.2.29.1 >/dev/null; then
    echo "expected Gregorian leap-day version to be accepted" >&2
    exit 1
fi
```

This check is intentionally red before the validator exists and proves that
February 29 is accepted for a leap year after implementation.

Update the valid output set to:

```python
expected = {
    "modularjobs-paper-26.8.11.1.jar",
    "modularjobs-postgres-26.8.11.1.sql",
    "SHA256SUMS",
}
```

Update the descriptor mismatch invocation and the non-empty-output invocation
to use `26.8.11.1`. Keep the mismatch fixture at `1.2.0` so the test proves the
embedded descriptor must equal the requested date version.

- [ ] **Step 2: Run the packaging test and verify the old contract fails**

Run:

```bash
bash scripts/test-package-release-assets.sh
```

Expected: `FAIL`, because the current packager still accepts only three-part
semantic versions and the fixture now requests `26.8.11.1`.

- [ ] **Step 3: Implement the shared validator**

Create `scripts/validate-release-version.sh` with this behavior:

```bash
#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "usage: $0 YY.M.D.REVISION" >&2
    exit 2
fi

VERSION=$1
PATTERN='^([0-9]{2})\.([1-9][0-9]?)\.([1-9][0-9]?)\.([1-9][0-9]*)$'
if [[ ! "$VERSION" =~ $PATTERN ]]; then
    echo "version must be YY.M.D.REVISION with positive unpadded M, D, and REVISION: $VERSION" >&2
    exit 2
fi

YEAR_SUFFIX=${BASH_REMATCH[1]}
MONTH=${BASH_REMATCH[2]}
DAY=${BASH_REMATCH[3]}
REVISION=${BASH_REMATCH[4]}
YEAR=$((2000 + 10#$YEAR_SUFFIX))
MONTH_NUMBER=$((10#$MONTH))
DAY_NUMBER=$((10#$DAY))
REVISION_NUMBER=$((10#$REVISION))

if (( MONTH_NUMBER > 12 )); then
    echo "version month must be between 1 and 12: $VERSION" >&2
    exit 2
fi

case "$MONTH_NUMBER" in
    2)
        if (( YEAR % 400 == 0 || (YEAR % 4 == 0 && YEAR % 100 != 0) )); then
            MAX_DAY=29
        else
            MAX_DAY=28
        fi
        ;;
    4|6|9|11) MAX_DAY=30 ;;
    *) MAX_DAY=31 ;;
esac

if (( DAY_NUMBER > MAX_DAY )); then
    echo "version day is invalid for its month and year: $VERSION" >&2
    exit 2
fi

if (( REVISION_NUMBER < 1 )); then
    echo "version revision must be at least 1: $VERSION" >&2
    exit 2
fi

printf 'valid release version: %s\n' "$VERSION"
```

Mark the new file executable with `chmod +x scripts/validate-release-version.sh`.
The regex rejects missing components, leading-zero M/D/REVISION components, and
zero revision; the range checks reject impossible calendar dates. The leap-year
rule is Gregorian and maps `YY` into `20YY`.

- [ ] **Step 4: Make the packager use the shared validator**

In `scripts/package-release-assets.sh`, define the repository root after the
argument check:

```bash
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
```

Replace the existing three-component semantic-version block with:

```bash
"$ROOT/scripts/validate-release-version.sh" "$VERSION"
```

Leave input-file validation, empty-output validation, descriptor verification,
copying, and checksum generation unchanged.

- [ ] **Step 5: Run the focused packaging test**

Run:

```bash
bash scripts/test-package-release-assets.sh
```

Expected: `package release assets tests: PASS`, including the valid
`26.8.11.1` package, all invalid-version failures, descriptor mismatch failure,
and checksum verification.

- [ ] **Step 6: Commit the validator and packaging contract**

```bash
git add scripts/validate-release-version.sh \
  scripts/package-release-assets.sh \
  scripts/test-package-release-assets.sh
git diff --cached --check
git commit -m "build: validate date-based release versions"
```

---

### Task 2: Wire the canonical version into Gradle and Paper metadata

**Files:**
- Modify: `build.gradle.kts:14-15`
- Modify: `paper/build.gradle.kts` inside the existing `tasks` block
- Modify: `paper/src/main/resources/plugin.yml:1-3`

**Interfaces:**
- Consumes: root `project.version` set to `26.8.11.1`.
- Produces: `paper/build/libs/paper-26.8.11.1-all.jar` with exactly one
  `version: '26.8.11.1'` line in its embedded `plugin.yml`.

- [ ] **Step 1: Set the canonical root version**

Replace the root declaration:

```kotlin
version = "2.0.0"
```

with:

```kotlin
version = "26.8.11.1"
```

- [ ] **Step 2: Replace the duplicated plugin version with a resource token**

Change the top of `paper/src/main/resources/plugin.yml` to:

```yaml
name: ModularJobs
version: '${projectVersion}'
main: net.aincraft.ModularJobsBootstrap
```

Inside the existing `tasks` block in `paper/build.gradle.kts`, add this
resource-processing configuration before the existing `shadowJar` block:

```kotlin
processResources {
    filesMatching("plugin.yml") {
        expand("projectVersion" to project.version.toString())
    }
}
```

Only `plugin.yml` is expanded, so unrelated resource text is not interpreted as
Gradle template syntax.

- [ ] **Step 3: Build the resource and verify the descriptor**

Run:

```bash
./gradlew :paper:processResources
unzip -p paper/build/resources/main/plugin.yml | sed -n '1,3p'
```

Expected output includes:

```text
name: ModularJobs
version: '26.8.11.1'
main: net.aincraft.ModularJobsBootstrap
```

- [ ] **Step 4: Build the Paper shadow JAR and verify its name**

Run:

```bash
./gradlew :paper:shadowJar
unzip -p paper/build/libs/paper-26.8.11.1-all.jar plugin.yml | sed -n '1,3p'
test -f paper/build/libs/paper-26.8.11.1-all.jar
```

Expected: the file exists and the embedded descriptor contains
`version: '26.8.11.1'`.

- [ ] **Step 5: Commit canonical version metadata**

```bash
git add build.gradle.kts paper/build.gradle.kts paper/src/main/resources/plugin.yml
git diff --cached --check
git commit -m "build: adopt date-based release version"
```

---

### Task 3: Make the release workflow tag-driven

**Files:**
- Modify: `.github/workflows/ci.yml:200-299`

**Interfaces:**
- Consumes: a pushed tag named `vYY.M.D.REVISION` and the shared validator from
  `scripts/validate-release-version.sh`.
- Produces: a GitHub release whose title and assets use the exact tag-derived
  version, after exact-tag, exact-commit, unused-release, package, and checksum
  checks pass.

- [ ] **Step 1: Make the release job run for version-prefixed tags**

Replace the release job name and condition with:

```yaml
release:
  name: Publish immutable ModularJobs date-version assets
  if: startsWith(github.ref, 'refs/tags/v')
```

Keep the workflow's existing `tags: ['v*']` push trigger and all job
 dependencies unchanged.

- [ ] **Step 2: Validate the tag and the checked-out canonical version before publication**

Move the existing `Set up JDK 21` step so it appears immediately before the
`Require exact tag and unused release` step. Keep its current contents:

```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: "21"
```

Replace `EXPECTED_TAG` in the `Require exact tag and unused release` step with
this shell block before any API publication:

```bash
VERSION="${GITHUB_REF_NAME#v}"
./scripts/validate-release-version.sh "$VERSION"

PROJECT_VERSION="$(./gradlew -q properties --console=plain | sed -n 's/^version: //p')"
if [[ "$PROJECT_VERSION" != "$VERSION" ]]; then
    echo "::error::Gradle project version $PROJECT_VERSION does not match tag version $VERSION"
    exit 1
fi

./gradlew :paper:processResources --console=plain --no-daemon
PLUGIN_VERSION="$(sed -n "s/^version: '\\(.*\\)'$/\\1/p" paper/build/resources/main/plugin.yml)"
if [[ "$PLUGIN_VERSION" != "$VERSION" ]]; then
    echo "::error::plugin.yml version $PLUGIN_VERSION does not match tag version $VERSION"
    exit 1
fi

echo "VERSION=$VERSION" >> "$GITHUB_ENV"
```

Remove the hardcoded `test "$GITHUB_REF_NAME" = "$EXPECTED_TAG"` check. Keep:

```bash
test "$(git rev-parse HEAD)" = "$GITHUB_SHA"
test "$(git rev-list -n 1 "$GITHUB_REF_NAME")" = "$GITHUB_SHA"
if gh release view "$GITHUB_REF_NAME" >/dev/null 2>&1; then
    echo "::error::release already exists for $GITHUB_REF_NAME"
    exit 1
fi
```

The validator, Gradle project-version check, and generated plugin descriptor
check all run before `Publish ModularJobs API artifacts`. A mismatched tag
therefore cannot publish API coordinates for the wrong version before the
release package is built.

- [ ] **Step 3: Replace every release-job `2.0.0` literal**

Change the package invocation to:

```bash
./scripts/package-release-assets.sh \
  "$VERSION" \
  "${shadow_jars[0]}" \
  paper/src/main/resources/sql/postgres.sql \
  dist
```

Change the publish command to:

```bash
gh release create "$GITHUB_REF_NAME" \
  "dist/modularjobs-paper-$VERSION.jar" \
  "dist/modularjobs-postgres-$VERSION.sql" \
  dist/SHA256SUMS \
  --verify-tag \
  --title "ModularJobs $VERSION" \
  --generate-notes
```

The existing re-download and checksum verification remains unchanged.

- [ ] **Step 4: Check workflow invariants locally**

Run:

```bash
python3 - <<'PY'
from pathlib import Path

text = Path('.github/workflows/ci.yml').read_text()
assert 'v2.0.0' not in text
assert '2.0.0' not in text
assert 'scripts/validate-release-version.sh' in text
assert '"$VERSION"' in text
assert 'ModularJobs $VERSION' in text
print('release workflow invariants: PASS')
PY
```

Expected: `release workflow invariants: PASS`.

- [ ] **Step 5: Commit dynamic release automation**

```bash
git add .github/workflows/ci.yml
git diff --cached --check
git commit -m "ci: publish date-versioned release assets"
```

---

### Task 4: Update active release documentation

**Files:**
- Modify: `README.md:14-19,43-47,167-169`
- Modify: `CHANGELOG.md:3-24`
- Modify: `docs/living-specs/professions.md:65`

**Interfaces:**
- Consumes: current version `26.8.11.1` and the date-version contract.
- Produces: active documentation that names the current artifact and release
  without claiming a remote release has already been published.

- [ ] **Step 1: Update README artifact and operator examples**

Use these exact current-version replacements:

```text
paper/build/libs/paper-26.8.11.1-all.jar
modularjobs-paper-26.8.11.1.jar
Plugin and project version: **26.8.11.1**
```

Add one sentence near the version section:

```markdown
Future releases use `YY.M.D.REVISION` tags, for example `v26.8.11.1`; the final component resets daily and increments for same-day releases.
```

- [ ] **Step 2: Relabel the current changelog entry without rewriting history**

Change the current heading to:

```markdown
## 26.8.11.1 — ModularJobs API and Azoth gathering gates
```

Change the artifact line to use `paper-26.8.11.1-all.jar` and
`modularjobs-paper-26.8.11.1.jar`. Change the sentence
`2.0.0 API cutover` to `date-versioned API cutover`; leave the historical
`1.1.0` section unchanged.

- [ ] **Step 3: Update the active living spec**

Change the completed professions milestone from `2.0.0` to `26.8.11.1`.
Do not modify dated plans or design records whose `2.0.0` text records the
historical implementation plan.

- [ ] **Step 4: Search active files for stale release labels**

Run:

```bash
python3 - <<'PY'
from pathlib import Path

active = [
    Path('README.md'),
    Path('CHANGELOG.md'),
    Path('docs/living-specs/professions.md'),
    Path('build.gradle.kts'),
    Path('paper/src/main/resources/plugin.yml'),
    Path('.github/workflows/ci.yml'),
]
for path in active:
    text = path.read_text()
    assert '2.0.0' not in text, f'stale release label in {path}'
print('active release references: PASS')
PY
```

Expected: `active release references: PASS`.

- [ ] **Step 5: Commit active documentation updates**

```bash
git add README.md CHANGELOG.md docs/living-specs/professions.md
git diff --cached --check
git commit -m "docs: document date-based release versioning"
```

---

### Task 5: Run complete verification

**Files:**
- Read only: all files changed by Tasks 1–4.
- Preserve: the pre-existing unstaged `paper/src/main/java/net/aincraft/upgrade/config/UpgradeTreeLoader.java` change.

- [ ] **Step 1: Run focused release packaging tests**

Run:

```bash
bash scripts/test-package-release-assets.sh
```

Expected: `package release assets tests: PASS`.

- [ ] **Step 2: Run module tests and checks**

Run:

```bash
./gradlew :api:test :common:test :paper:test :paper:check :paper:visualTest :paper:build --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Verify the actual release artifact and descriptor**

Run:

```bash
test -f paper/build/libs/paper-26.8.11.1-all.jar
unzip -p paper/build/libs/paper-26.8.11.1-all.jar plugin.yml | sed -n '1,3p'
```

Expected: the artifact exists and the descriptor reports
`version: '26.8.11.1'`.

- [ ] **Step 4: Verify local Maven publication metadata**

Run:

```bash
./gradlew :api:publishMavenPublicationToLocalBuildRepo \
  :common:publishMavenPublicationToLocalBuildRepo
```

Expected: local repository paths contain `26.8.11.1` for both
`modularjobs-api` and `modularjobs-common`.

- [ ] **Step 5: Review final worktree partition**

Run:

```bash
git status --short
git diff --stat
```

Expected: only the intentionally preserved `UpgradeTreeLoader.java` change is
unstaged; no generated runtime files or unrelated files are staged. Do not push
a tag or claim a remote release from this verification alone.
