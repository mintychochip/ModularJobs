# Date-Based Release Versioning Design

**Date:** 2026-08-11

**Status:** Approved design; specification review pending.

## Goal

Replace the unpublished `2.0.0` release plan with date-based ModularJobs
versions while preserving the existing published release history. The first
release under the new scheme is `26.8.11.1`.

## Scope

### Included

- Define and enforce the release version format `YY.M.D.REVISION`.
- Use `26.8.11.1` as the current project, plugin, API, common, and Paper
  artifact version.
- Make the root Gradle version the canonical build version.
- Generate the packaged `plugin.yml` version from the Gradle project version.
- Update release packaging and GitHub Actions to derive asset names and
  validation from the pushed tag.
- Update active release documentation, changelog, and living-spec references.
- Add validation for malformed date versions and metadata/tag mismatches.

### Preserved

- Published tags and releases `v1.1.0` and `v1.2.0` remain unchanged.
- The unpublished local `v2.0.0` tag is not released or deleted by this work.
- The placeholder expansion's internal compatibility version (`1.1`) is
  unrelated to the project release version and remains unchanged.

### Deferred

- Automatic version generation from the system clock.
- Rewriting or deleting existing Git tags.
- Pushing `v26.8.11.1` or creating a remote GitHub release without an explicit
  release action.

## Version Contract

The canonical release version is four numeric components:

```text
YY.M.D.REVISION
```

For the current release:

```text
26.8.11.1
```

Rules:

- `YY` is the two-digit calendar year.
- `M` is the calendar month without required zero padding.
- `D` is the calendar day without required zero padding.
- `REVISION` starts at `1` for the first release on a date and increments for
  additional releases on that same date.
- `REVISION` resets to `1` on the next calendar date.
- Components are separated by dots and contain no leading `v` in build
  metadata. Git release tags use the `v` prefix, for example
  `v26.8.11.1`.
- The version is recorded explicitly in source before a release; builds do not
  read the system clock to select a version.

Artifact names for the current release are:

```text
paper/build/libs/paper-26.8.11.1-all.jar
modularjobs-paper-26.8.11.1.jar
modularjobs-postgres-26.8.11.1.sql
```

The Maven coordinates for the public modules use the same version:

```text
org.aincraft:modularjobs-api:26.8.11.1
org.aincraft:modularjobs-common:26.8.11.1
```

## Architecture

### Canonical Gradle version

The root `build.gradle.kts` remains the single source of the project version.
Subprojects inherit it through the existing `rootProject.version` assignment.
Gradle's project version continues to control module artifact names and Maven
publication metadata.

### Packaged plugin metadata

`paper/src/main/resources/plugin.yml` will use a Gradle resource token rather
than a second manually maintained version. The Paper resource-processing task
will expand the token from `project.version`. This makes the descriptor embedded
in the shadow JAR agree with the Gradle artifact and Maven versions by
construction.

### Release packaging

`scripts/package-release-assets.sh` will accept `YY.M.D.REVISION` versions,
validate the component shape and calendar ranges, verify the embedded
`plugin.yml` version, and produce versioned Paper and PostgreSQL assets plus
`SHA256SUMS`.

The packaging test will cover:

- a valid four-component date version;
- the expected versioned output names and checksums;
- descriptor/version mismatch;
- malformed and out-of-range versions; and
- non-empty output directories.

### GitHub Actions release job

The release workflow will trigger for version-prefixed tags, derive
`VERSION=${GITHUB_REF_NAME#v}`, and reject tags that do not match the date
version contract. It will retain the existing exact-tag, exact-commit, unused
release, API publication, package checksum, and re-download verification
checks.

The release job will no longer hardcode `v2.0.0` or `2.0.0`. It will use the
validated tag-derived version for the release title and all asset paths. A
future release requires updating the canonical source version, committing it,
creating a matching `vYY.M.D.REVISION` tag, and pushing that tag.

## Documentation Migration

Active documentation will describe `26.8.11.1` as the current release and
will use the date-based format for future releases. The current changelog entry
will be relabeled from the unpublished `2.0.0` heading to `26.8.11.1`; wording
that uses `2.0.0` solely as a release label will be made release-neutral where
needed. Dated implementation plans and approved design records retain their
original `2.0.0` references as historical records.

## Error Handling

- Packaging fails before copying assets when the supplied version is malformed
  or has invalid month/day/revision ranges.
- Packaging fails when the shadow JAR's embedded descriptor does not contain the
  supplied version exactly once.
- The release job fails when the pushed tag is not a valid date-version tag, the
  checkout does not match the tag commit, or a release already exists.
- No fallback version is generated from the current clock.

## Verification

- Run the packaging script tests with the four-component version contract.
- Run API, common, and Paper unit tests.
- Run Paper checks and build the shadow JAR.
- Inspect the shadow JAR for `plugin.yml` version `26.8.11.1` and the expected
  `paper-26.8.11.1-all.jar` name.
- Verify the release workflow contains no hardcoded `2.0.0` release trigger,
  package argument, asset name, or title.
- Verify published historical release tags remain untouched and do not claim a
  new remote release before a tag push and successful workflow execution.
