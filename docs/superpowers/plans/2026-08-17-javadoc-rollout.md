# JavaDoc Rollout Implementation Plan

## Goal

Bring all Java declarations in `api`, `common`, and `paper` into compliance with the repository's MissingJavadoc Checkstyle rules while preserving behavior.

## Steps

1. **Baseline and inventory**
   - Run module Checkstyle tasks and record current MissingJavadoc diagnostics.
   - Group diagnostics by module/package and declaration kind.

2. **Document `api`**
   - Add type and member Javadocs to undocumented public contracts first.
   - Add package-private/private method comments required by Checkstyle.
   - Preserve existing API wording and links.
   - Run `:api:check` and `:api:javadoc`.

3. **Document `common`**
   - Document DTO types, constructors, accessors, and conversion semantics.
   - Run `:common:check` and `:common:javadoc`.

4. **Document paper domain**
   - Cover domain implementations, records, repository contracts, persistence adapters, and service lifecycle.
   - Document SQL/connection ownership and cache/write-back invariants where relevant.
   - Run `:paper:check` and focused paper tests.

5. **Document paper runtime integrations**
   - Cover bootstrap, payment, boost, listeners, events, commands, GUI, hooks, placeholders, and utility classes.
   - Document Paper thread/lifecycle assumptions and event side effects.
   - Run `:paper:check` and focused paper tests.

6. **Review and verify**
   - Search Checkstyle XML reports for remaining missing-Javadoc diagnostics.
   - Run `./gradlew :api:check :common:check :paper:check`.
   - Run `./gradlew :api:javadoc :common:javadoc`.
   - Run the repository's Java test task.
   - Confirm no Rust or TypeScript files changed.

## Editing rules

- Documentation-only changes; no API or behavior refactors.
- Use narrow edits and preserve formatting.
- Do not add comments that merely repeat identifiers.
- Do not suppress Checkstyle rules.
- Do not add generated files or modify build output.
