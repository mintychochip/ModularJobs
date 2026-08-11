# REST-backed web editor session cutover Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Historical note (2026-08-10):** Retained as an implementation record; current distribution defaults the external editor to disabled with empty URLs.

**Goal:** Replace Paper's Bytebin editor workflow with the PostgreSQL-backed Rust REST session API while preserving `/jobs editor` and `/jobs applyedits <code>`.

**Architecture:** The Paper plugin and `web/rest-api` connect to the same operator-provisioned PostgreSQL database; neither launches PostgreSQL or runs DDL. Paper owns a small Caffeine mapping from REST session code to token/player only for the apply-command handoff, while REST/PostgreSQL owns the editor payload. Paper applies fetched payloads through the existing task repository so its save/update and delete/invalidate cache behavior remains authoritative.

**Tech Stack:** Java 25, Paper 26.2, Java `HttpClient`, Gson, Caffeine, JUnit 5, Rust/Axum/SQLx REST API, React/Vite session editor, Astro bridge, PostgreSQL.

## Global Constraints

- PostgreSQL is the only supported database.
- The plugin and REST API connect to PostgreSQL; schema provisioning stays out-of-band through `scripts/apply-postgres-schema.sh`.
- The plugin MUST NOT start a PostgreSQL process, create tables, or own database files.
- REST server-minted session tokens are authoritative; client-supplied payload tokens are ignored on create.
- Session URLs put `code` in the query and `token` in the URL fragment.
- `/jobs applyedits <code>` MUST NOT expose the token as a command argument.
- Paper-local session mappings are ephemeral and ownership-scoped to the exporting player.
- Existing `RelationalJobTaskRepositoryImpl.save/delete` cache update/invalidation behavior is reused, not bypassed.
- Production/session payloads use the Rust REST API, not Bytebin.
- Every production change and its behavior tests belong in one atomic commit.

---

## File map

| File | Responsibility in this plan |
|---|---|
| `paper/src/main/java/net/aincraft/editor/EditorConfig.java` | REST URL, web URL, create secret, and local handoff lifetime loaded from `config.yml`. |
| `paper/src/main/java/net/aincraft/editor/EditorSession.java` | Code-keyed local handoff record containing token, owner, and expiry. |
| `paper/src/main/java/net/aincraft/editor/EditorSessionStore.java` | Caffeine code→session mapping with owner/expiry checks. |
| `paper/src/main/java/net/aincraft/editor/RestSessionClient.java` | Java HTTP client for REST create/fetch operations and status-aware errors. |
| `paper/src/main/java/net/aincraft/editor/EditorService.java` | Export through REST, import through REST, and repository application. |
| `paper/src/main/java/net/aincraft/editor/ExportResult.java` | Session-code naming instead of Bytebin naming. |
| `paper/src/main/java/net/aincraft/PluginContext.java` | Construct REST editor dependencies from plugin configuration. |
| `paper/src/main/java/net/aincraft/editor/BytebinClient.java` | Delete obsolete Paper Bytebin transport. |
| `paper/src/main/resources/config.yml` | Document REST editor settings and external REST dependency. |
| `paper/src/test/java/net/aincraft/editor/EditorSessionStoreTest.java` | Test ownership, code lookup, expiry, and removal. |
| `paper/src/test/java/net/aincraft/editor/EditorServiceTest.java` | Test the pure editor URL contract. |
| `web/src/pages/editor/session.astro` | Preserve the token fragment when bridging to the React editor. |
| `web/session-editor/src/sessionCredentials.test.ts` | Lock the hash-token contract and query-token migration behavior. |
| `docs/database-schema.md` | Explicitly document one external PostgreSQL instance shared by Paper and REST. |
| `web/rest-api/README.md` | Document Paper as a REST client and the required shared database/configuration. |

---

### Task 1: Define REST editor configuration and local handoff model

**Files:**
- Modify: `paper/src/main/java/net/aincraft/editor/EditorConfig.java`
- Modify: `paper/src/main/java/net/aincraft/editor/EditorSession.java`
- Modify: `paper/src/main/java/net/aincraft/editor/EditorSessionStore.java`
- Create: `paper/src/test/java/net/aincraft/editor/EditorSessionStoreTest.java`

**Interfaces:**
- `EditorConfig.fromPlugin(JavaPlugin plugin)` returns the settings used by all Paper editor components.
- `EditorSession` exposes `sessionCode()`, `token()`, `playerId()`, `createdAt()`, `expiresAt()`, and `isExpired(Instant now)`.
- `EditorSessionStore.store(EditorSession session)`, `get(String sessionCode)`, `getOwned(String sessionCode, UUID playerId)`, and `remove(String sessionCode)` are the only local handoff operations.

- [ ] **Step 1: Write failing store tests**

Create a pure JUnit test fixture with two UUIDs and a session whose code is `code-a`, token is `token-a`, and expiry is one hour in the future. Assert code lookup, owner match, wrong-owner rejection, removal, and an already-expired session. The behavioral assertions are:

```java
assertEquals(session, store.get("code-a").orElseThrow());
assertEquals(session, store.getOwned("code-a", owner).orElseThrow());
assertTrue(store.getOwned("code-a", other).isEmpty());
store.remove("code-a");
assertTrue(store.get("code-a").isEmpty());
assertTrue(store.getOwned("expired", owner).isEmpty());
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```bash
./gradlew :paper:test --tests net.aincraft.editor.EditorSessionStoreTest
```

Expected: compilation/test failure because the code-keyed store API and test fixture do not exist yet.

- [ ] **Step 3: Implement REST-oriented configuration and store**

Change `EditorConfig` from Bytebin fields to REST fields and load them from the plugin's normal Bukkit configuration:

```java
public record EditorConfig(
    boolean enabled,
    String sessionApiUrl,
    String webEditorUrl,
    String sessionCreateSecret,
    int sessionTtlMinutes
) {
  public static final String DEFAULT_SESSION_API_URL = "http://127.0.0.1:18787";
  public static final String DEFAULT_WEB_EDITOR_URL = "";
  public static final int DEFAULT_SESSION_TTL = 24 * 60;

  public static EditorConfig defaults() {
    return new EditorConfig(
        true,
        DEFAULT_SESSION_API_URL,
        DEFAULT_WEB_EDITOR_URL,
        "",
        DEFAULT_SESSION_TTL);
  }

  public static EditorConfig fromPlugin(JavaPlugin plugin) {
    FileConfiguration config = plugin.getConfig();
    EditorConfig defaults = defaults();
    return new EditorConfig(
        config.getBoolean("editor.enabled", defaults.enabled()),
        config.getString("editor.session-api-url", defaults.sessionApiUrl()),
        config.getString("editor.web-editor-url", defaults.webEditorUrl()),
        config.getString("editor.session-create-secret", defaults.sessionCreateSecret()),
        Math.max(1, config.getInt("editor.session-ttl-minutes", defaults.sessionTtlMinutes())));
  }
}
```

Change the handoff record to be code-keyed and expiry-aware:

```java
public record EditorSession(
    String sessionCode,
    String token,
    UUID playerId,
    Instant createdAt,
    Instant expiresAt
) {
  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }
}
```

Make `EditorSessionStore` index by `session.sessionCode()`, use `expireAfterWrite(Duration.ofMinutes(config.sessionTtlMinutes()))`, and have `getOwned` remove and reject expired entries before comparing the player UUID.

- [ ] **Step 4: Run the focused test and verify it passes**

Run:

```bash
./gradlew :paper:test --tests net.aincraft.editor.EditorSessionStoreTest
```

Expected: PASS for all store behavior tests. If Paper compilation is still blocked by the unavailable Craftux dependency, run the test after that dependency is available and record the external blocker rather than changing dependency resolution.

- [ ] **Step 5: Commit the configuration/store unit**

```bash
git add paper/src/main/java/net/aincraft/editor/EditorConfig.java \
  paper/src/main/java/net/aincraft/editor/EditorSession.java \
  paper/src/main/java/net/aincraft/editor/EditorSessionStore.java \
  paper/src/test/java/net/aincraft/editor/EditorSessionStoreTest.java
git commit -m "feat: model REST editor session handoff"
```

---

### Task 2: Add the Paper REST session client

**Files:**
- Create: `paper/src/main/java/net/aincraft/editor/RestSessionClient.java`
- Create: `paper/src/test/java/net/aincraft/editor/RestSessionClientTest.java`

**Interfaces:**

```java
public final class RestSessionClient {
  public RestSessionClient(EditorConfig config, Gson gson);
  public CompletableFuture<CreatedSession> create(EditorPayload payload);
  public CompletableFuture<EditorPayload> fetchPayload(String sessionCode, String token);

  public record CreatedSession(
      String sessionCode,
      String token,
      Instant expiresAt
  ) {}

  public static final class RestSessionException extends RuntimeException {
    public int statusCode();
    public boolean expired();
  }
}
```

- [ ] **Step 1: Write failing HTTP contract tests**

Use `com.sun.net.httpserver.HttpServer` on an ephemeral local port. The create handler must assert `POST /api/v1/sessions`, `Content-Type: application/json`, and `X-Create-Secret: test-secret`, then return:

```json
{"code":"abc123","token":"server-token","expiresAt":"2030-01-01T00:00:00Z"}
```

The fetch handler must assert `GET /api/v1/sessions/abc123/payload`, `Authorization: Bearer server-token`, and `X-Session-Token: server-token`, then return a valid camelCase `EditorPayload`. Add a 410 handler and assert `RestSessionException.expired()` is true. Add a 401 handler and assert `expired()` is false.

- [ ] **Step 2: Run the focused client test and verify it fails**

```bash
./gradlew :paper:test --tests net.aincraft.editor.RestSessionClientTest
```

Expected: compilation failure because `RestSessionClient` does not exist.

- [ ] **Step 3: Implement the HTTP client**

Use a single `HttpClient` with a 30-second connect timeout and UTF-8 JSON bodies. Normalize the configured base URL by removing trailing slashes. Build endpoint URIs with `URI.create(base + "/api/v1/sessions")` and `URLEncoder`/`URLDecoder`-safe path encoding for session codes.

Create requests with:

```java
HttpRequest.newBuilder(uri)
    .timeout(Duration.ofSeconds(30))
    .header("Content-Type", "application/json")
    .header("X-Create-Secret", config.sessionCreateSecret())
    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), UTF_8))
    .build();
```

Only add `X-Create-Secret` when the configured value is nonblank. Accept HTTP 201 for create and HTTP 200 for fetch. For every other response, throw `RestSessionException` containing the status and server error body; classify HTTP 410 as expired. Parse `expiresAt` with `OffsetDateTime.parse(value).toInstant()` so both `Z` and offset RFC3339 forms are accepted. Reject empty response code/token fields with a client exception.

- [ ] **Step 4: Run the focused client test and verify it passes**

```bash
./gradlew :paper:test --tests net.aincraft.editor.RestSessionClientTest
```

Expected: PASS for create headers/body, fetch headers/path, 401 classification, and 410 classification.

- [ ] **Step 5: Commit the REST client unit**

```bash
git add paper/src/main/java/net/aincraft/editor/RestSessionClient.java \
  paper/src/test/java/net/aincraft/editor/RestSessionClientTest.java
git commit -m "feat: add Paper REST editor session client"
```

---

### Task 3: Replace Bytebin in the Paper editor service

**Files:**
- Modify: `paper/src/main/java/net/aincraft/editor/EditorService.java`
- Modify: `paper/src/main/java/net/aincraft/editor/ExportResult.java`
- Modify: `paper/src/main/java/net/aincraft/PluginContext.java`
- Delete: `paper/src/main/java/net/aincraft/editor/BytebinClient.java`
- Create: `paper/src/test/java/net/aincraft/editor/EditorServiceTest.java`

**Interfaces:**
- `EditorService` constructor consumes `RestSessionClient`, `EditorSessionStore`, `EditorConfig`, and `Gson`; it no longer consumes `BytebinClient`.
- `EditorService.exportTasks(...)` returns an `ExportResult` whose first field is `sessionCode`.
- `EditorService.importTasks(String sessionCode, UUID playerId)` keeps the existing command-facing signature.

- [ ] **Step 1: Write failing service-level URL tests**

Add `paper/src/test/java/net/aincraft/editor/EditorServiceTest.java` with a pure URL-contract test:

```java
@Test
void editorUrlKeepsTokenAfterFragment() {
  String url = EditorService.editorUrl(
      "https://editor.example/editor", "code/1", "secret-token");
  assertEquals(
      "https://editor.example/editor/session?code=code%2F1#token=secret-token",
      url);
  assertTrue(url.indexOf("#token=") > url.indexOf("code="));
}
```

Keep ownership behavior covered by `EditorSessionStoreTest`; the service test must not require Bukkit, a database, or a mock subclass of the final REST client. Do not test private implementation details or Bukkit messaging.

- [ ] **Step 2: Run the focused service test and verify it fails**

```bash
./gradlew :paper:test --tests net.aincraft.editor.EditorServiceTest
```

Expected: compilation/test failure because the service still depends on Bytebin and has no REST URL boundary.

- [ ] **Step 3: Implement REST-backed export**

Keep the existing payload-building logic but replace the Bytebin continuation with a typed draft:

```java
private record ExportDraft(UUID playerId, EditorPayload payload) {}

return CompletableFuture.supplyAsync(() -> {
  UUID playerId = exportingPlayerId;
  List<Job> jobs = jobKey == null
      ? jobService.getJobs()
      : List.of(getJobOrThrow(jobKey));
  EditorPayload payload = buildPayload(playerId, jobs);
  return new ExportDraft(playerId, payload);
}).thenCompose(draft -> restClient.create(draft.payload()))
    .thenApply(created -> {
      EditorSession session = new EditorSession(
          created.sessionCode(),
          created.token(),
          draft.playerId(),
          Instant.now(),
          created.expiresAt());
      sessionStore.store(session);
      String url = editorUrl(config.webEditorUrl(), created.sessionCode(), created.token());
      return new ExportResult(created.sessionCode(), url, created.token());
    });
```

The existing `buildJobData`, registered-type, and metadata logic remains in `buildPayload`; `EditorService` must pass the payload through unchanged except for the server-side token rewrite performed by REST. Use the REST response token for the local mapping and URL. Do not treat the payload metadata token as the authentication source; the API overwrites it during create.

Build the URL with UTF-8 percent encoding and a fragment:

```java
static String editorUrl(String base, String code, String token) {
  String normalized = base.replaceFirst("/+$", "");
  return normalized + "/session?code=" + encode(code) + "#token=" + encode(token);
}
```

Use UTF-8 percent encoding for both values. The trailing-slash expression removes one or more terminal slashes without changing an intentional path such as `/editor`.

- [ ] **Step 4: Implement REST-backed import and ownership checks**

At the start of `importTasks`, resolve the code through `sessionStore.getOwned(sessionCode, playerId)`. If absent, return an `ImportResult` with zero changes and the message `Editor session is missing, expired, or belongs to another player; run /jobs editor again.` without calling REST.

Fetch with `restClient.fetchPayload(session.sessionCode(), session.token())`. Retain the existing task conversion, save, and delete loops. Catch `RestSessionClient.RestSessionException` and report `Session expired; run /jobs editor again.` for `expired()`, otherwise report its server message. Catch other exceptions with the existing generic import error. Remove the session mapping only when the import returns without errors; retain it for a failed/partial retry.

- [ ] **Step 5: Update construction and remove Bytebin**

In `PluginContext`, replace:

```java
EditorConfig editorConfig = EditorConfig.defaults();
EditorSessionStore sessionStore = new EditorSessionStore(editorConfig);
BytebinClient bytebinClient = new BytebinClient(editorConfig, gson);
```

with:

```java
EditorConfig editorConfig = EditorConfig.fromPlugin(plugin);
EditorSessionStore sessionStore = new EditorSessionStore(editorConfig);
RestSessionClient restSessionClient = new RestSessionClient(editorConfig, gson);
```

Pass `restSessionClient` to `EditorService`. Delete `BytebinClient.java`, update `ExportResult` documentation/field names from `bytebinCode` to `sessionCode`, and remove Bytebin exception handling/imports from `EditorService`.

- [ ] **Step 6: Run focused tests and verify they pass**

```bash
./gradlew :paper:test --tests net.aincraft.editor.EditorServiceTest \
  --tests net.aincraft.editor.EditorSessionStoreTest \
  --tests net.aincraft.editor.RestSessionClientTest
```

Expected: all focused Paper editor tests pass when the Paper dependency is resolvable. `EditorCommand` and `ApplyEditsCommand` should compile unchanged because their public service method signatures remain stable.

- [ ] **Step 7: Commit the Paper cutover**

```bash
git add paper/src/main/java/net/aincraft/editor/EditorService.java \
  paper/src/main/java/net/aincraft/editor/ExportResult.java \
  paper/src/main/java/net/aincraft/PluginContext.java \
  paper/src/main/java/net/aincraft/editor/BytebinClient.java \
  paper/src/test/java/net/aincraft/editor/EditorServiceTest.java
git commit -m "feat: route Paper editor sessions through REST"
```

---

### Task 4: Align web bridge, configuration, and operator documentation

**Files:**
- Modify: `paper/src/main/resources/config.yml`
- Modify: `web/src/pages/editor/session.astro`
- Modify: `web/session-editor/src/sessionCredentials.test.ts`
- Modify: `docs/database-schema.md`
- Modify: `web/rest-api/README.md`

**Interfaces:**
- The Astro bridge forwards `?code=...#token=...` to the React app without moving the token into the query string.
- `sessionEditorPath(code, token)` remains the canonical client-side URL helper.

- [ ] **Step 1: Write failing credential/bridge tests where executable**

Extend `sessionCredentials.test.ts` with a URL containing a code query and hash token and assert the token is read from the hash. Add an assertion that the token never appears in the query portion returned by `sessionEditorPath`. Keep the existing legacy query-token test because the React client must migrate old links safely.

- [ ] **Step 2: Run the focused web tests and verify the new assertion fails**

```bash
cd web/session-editor
npm test -- --run src/sessionCredentials.test.ts
```

Expected: the new bridge/fragment assertion fails before the bridge/config changes.

- [ ] **Step 3: Add the editor configuration block**

Append this documented section to `paper/src/main/resources/config.yml`:

```yaml
# Secure web editor. PostgreSQL and the REST API are external services.
# The plugin never launches PostgreSQL or provisions schema.
editor:
  enabled: true
  session-api-url: http://127.0.0.1:18787
  web-editor-url: ""
  # Must match REST SESSION_CREATE_SECRET when the API requires one.
  session-create-secret: ""
  session-ttl-minutes: 1440
```

- [ ] **Step 4: Preserve hash tokens through Astro**

In `session.astro`, parse `window.location.hash` in addition to `window.location.search`, choose the hash token first, and construct the React target with the code query plus `#token=<encoded-token>`. Do not append the token to the target query string. The iframe and “open editor” link must receive the same target.

- [ ] **Step 5: Align operator docs**

Document all of the following in `docs/database-schema.md` and `web/rest-api/README.md`:

- one external PostgreSQL database is shared by Paper and REST;
- apply `paper/src/main/resources/sql/postgres.sql` before starting either process;
- Paper's `database.yml` configures the database connection;
- REST's `DATABASE_URL` must target the same database;
- `SESSION_CREATE_SECRET` must match `editor.session-create-secret` when enabled;
- the plugin does not start PostgreSQL and cannot replace operator backups, upgrades, or schema provisioning.

Remove remaining production-path Bytebin wording without deleting the explicitly marked legacy Vue demo unless a reference points users to it as the secure production editor.

- [ ] **Step 6: Run web tests/build**

```bash
cd web/session-editor
npm test -- --run
npm run build
```

Expected: all session-editor tests pass and Vite produces the production build. The existing Astro package may need its normal `npm install` dependencies before the Astro site build; do not change unrelated frontend tooling.

- [ ] **Step 7: Commit the web/config/documentation unit**

```bash
git add paper/src/main/resources/config.yml \
  web/src/pages/editor/session.astro \
  web/src/lib/sessionApi.ts \
  web/session-editor/src/sessionCredentials.test.ts \
  docs/database-schema.md \
  web/rest-api/README.md
git commit -m "docs: configure REST-backed web editor deployment"
```

---

### Task 5: Verify the complete session and apply path

**Files:**
- No planned production files; verification may add only narrowly scoped regression tests if a verified contract lacks coverage.

- [ ] **Step 1: Verify clean atomic history and working tree partition**

Run:

```bash
git status --short
git log --oneline -5
```

Expected: only intentional uncommitted test/build artifacts are ignored; migration commits are separate by concern and the existing base-worktree edits are not present in this isolated worktree.

- [ ] **Step 2: Start an external PostgreSQL service for the smoke test**

Use an operator-managed local PostgreSQL instance/container listening at `127.0.0.1:55432`; do not start it from Paper. Apply the schema with:

```bash
DATABASE_URL=postgres://test:test@127.0.0.1:55432/modularjobs \
  ./scripts/apply-postgres-schema.sh
```

If no PostgreSQL service is available, record the exact connection failure and continue with all tests that do not require the database; do not add plugin database-launch code.

- [ ] **Step 3: Run REST integration tests**

```bash
cd web/rest-api
DATABASE_URL=postgres://test:test@127.0.0.1:55432/modularjobs cargo test
```

Expected: all REST session authorization, expiry, schema, and payload replacement tests pass.

- [ ] **Step 4: Run Java module tests/build**

```bash
./gradlew :api:test :common:test :paper:test :paper:build
```

Expected: Java tests and the Paper shadow artifact pass when the configured Craftux Maven dependency is available. If dependency resolution remains blocked by the existing 401/DNS failures, preserve the focused test results and report that external blocker exactly.

- [ ] **Step 5: Exercise the session contract directly**

With the REST API running against the provisioned database:

1. POST a valid payload and record `code`, `token`, and `expiresAt`.
2. GET the payload with the returned token and verify HTTP 200.
3. PUT an edited payable amount with the same token and verify HTTP 200.
4. GET again and verify the edited amount.
5. GET with a different token and verify HTTP 401.

This proves the durable REST half before launching Paper.

- [ ] **Step 6: Smoke-test Paper export/apply**

Start Paper with `database.yml` pointing to the same PostgreSQL database and `config.yml` pointing to the running REST API. As an admin player:

1. Run `/jobs editor`.
2. Open the generated URL and confirm the browser receives code from the query and token from the fragment.
3. Edit a task payable amount and save.
4. Run `/jobs applyedits <code>` in the same Paper lifetime.
5. Verify the success counts and query the changed task through the Paper service/repository path.
6. Confirm a removed task is absent and a saved task is returned from the repository after cache access.

- [ ] **Step 7: Record final verification**

Capture exact passing test counts and any environment-only blockers in the final response. Do not claim end-to-end success unless the browser save and Paper apply steps were exercised against the same PostgreSQL database.
