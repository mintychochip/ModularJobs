# Repository-wide JavaDoc Rollout

## Scope

Add meaningful Javadocs to all Java declarations in `api`, `common`, and `paper` required by the repository's Checkstyle contract. Rust and TypeScript are explicitly out of scope because the request is for Javadocs and those languages do not use Javadoc.

The target includes all classes, interfaces, enums, records, annotations, methods, and constructors. Checkstyle permits missing method documentation only for declarations annotated with `@Override` or `@Test`; those existing exemptions remain. Fields are documented when they are public API, constants, serialized state, or carry non-obvious invariants.

## Conventions

- Describe observable behavior and ownership rather than restating names.
- Preserve existing terminology and link related types with `{@link}`.
- Use `@param`, `@return`, `@throws`, and `@deprecated` in the order required by Checkstyle.
- Document nullability, lifecycle, thread-safety, persistence, side effects, and failure behavior when relevant.
- Keep comments concise and avoid speculative claims.
- Do not change runtime behavior, signatures, visibility, or serialization formats.

## Batching

1. Public API (`api`).
2. Shared DTOs (`common`).
3. Paper domain, models, repositories, and services.
4. Paper payment, boosts, events, commands, GUI, hooks, bootstrap, and remaining integrations.
5. Cross-module review and documentation verification.

Each batch must compile before the next batch begins. Edits should remain reviewable and should not introduce generated or blanket boilerplate comments.

## Acceptance criteria

- Checkstyle reports no missing Javadoc violations for the targeted Java declarations.
- `api` and `common` Javadoc tasks complete without unresolved repository references; existing project policy may retain `isFailOnError = false`.
- Java compilation and module tests remain passing.
- No Rust or TypeScript files are modified for this effort.
- Documentation accurately reflects current implementation behavior.

## Verification commands

```text
./gradlew :api:check :common:check :paper:check
./gradlew :api:javadoc :common:javadoc
```

Review generated reports and search for remaining missing-Javadoc diagnostics after each batch.
