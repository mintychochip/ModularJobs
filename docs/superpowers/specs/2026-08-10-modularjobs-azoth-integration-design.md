# ModularJobs 2.0.0 and Azoth integration design

**Date:** 2026-08-10  
**Status:** Approved for implementation  
**Historical note (2026-08-10):** This approved sibling-integration design is retained for history; it is not current standalone ModularJobs setup guidance.
**Repositories:** `modularjobs`, sibling `azoth`

## Goal

Release ModularJobs 2.0.0 as the progression provider and integrate it into Azoth. Azoth owns all world-interaction level gates, while ModularJobs owns profession identity, progression, and payment data. Implement the remaining gathering interactions in Azoth without duplicating or migrating Azoth's combat-level rules.

## Ownership

- **ModularJobs:** public `ProfessionService`, profession aliases/resolution, player profession levels and experience, progression task definitions, payment listeners, and Java artifacts.
- **Azoth:** gathering gate configuration, interaction predicates, denial messages, bypass permissions, listeners, and enforcement. Azoth obtains the authoritative level from `ProfessionService`.
- **Combat level:** remains owned by Azoth. Existing `CombatLevelService` behavior is unchanged; no profession checks are converted to combat checks.

A cancelled gathering event must not reach payment listeners. Gate listeners therefore run before payment and payment listeners continue to ignore cancelled events.

## Release and dependency contract

- ModularJobs version, plugin descriptor, published `api`/`common` artifacts, release assets, and documentation use `2.0.0`.
- `org.aincraft:modularjobs-api:2.0.0` is Java 21-compatible and depends on `org.aincraft:modularjobs-common:2.0.0`.
- Paper implementation remains on its existing Java 25 toolchain.
- Azoth uses the ModularJobs API as `compileOnly`; it does not shade or relocate ModularJobs. Its Paper descriptor declares a required server dependency with `load: BEFORE` and `join-classpath: true`.
- ModularJobs always registers its core `ProfessionService` Bukkit service. Optional auxiliary services retain their existing configuration gate.

## Gate scope

Azoth enforces these configured interactions:

1. Block breaking: mining, woodcutting, farming, and herbalism tiers.
2. Fishing: cod, salmon, tropical fish, and pufferfish tiers.
3. Log stripping with an axe.
4. Mature sweet-berry, cocoa, and cave-vine harvesting.

Predicates are explicit and conservative: only supported materials/actions are gated; invalid configuration fails closed during loading; cancelled events are ignored; bypass permissions skip enforcement. Gate denial cancels the event and sends the configured level/profession/action message. Interaction events also deny the Bukkit use result.

The existing gathering task rows, including Herbalism rows required by the configured tiers, remain in ModularJobs so successful interactions can progress and pay correctly. No database schema change is introduced.

## Configuration migration

Move the gathering gate sections and their current tier values from the ModularJobs runtime configuration into Azoth's configuration. ModularJobs no longer registers gathering gate listeners or owns gate configuration. Documentation states that Azoth must be installed and configured for enforcement; ModularJobs remains usable as a progression/payment provider without Azoth.

## Verification

- ModularJobs API/common compile and publish locally at 2.0.0; plugin and release package metadata agree on 2.0.0.
- ModularJobs tests cover the remaining payment/progression contracts and no longer expect ModularJobs-owned enforcement.
- Azoth tests cover each gate family at below, exact, above, missing-level, cancelled-event, and bypass boundaries where applicable.
- Build Azoth against the published ModularJobs API and run both repository test suites.
- Exercise release packaging and verify checksums and embedded plugin version.
