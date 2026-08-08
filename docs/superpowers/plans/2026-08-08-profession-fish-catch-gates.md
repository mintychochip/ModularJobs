# Profession-Gated Fish Catch Gates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add opt-in profession-level gates for vanilla fish catches, preventing configured catches below the required level before ModularJobs pays for them.

**Architecture:** Keep the public gate record and read-only service in the Paper-free `api` module. In `paper`, parse `fish-catch-gates` into an immutable store and enforce it with a `PlayerFishEvent` listener at `NORMAL`, before the existing `MONITOR` payment listener. Use the stable event boundary rather than NMS/datapack loot-pool changes: an ineligible already-generated catch is cancelled, not rerolled, collected, paid, or progressed.

**Tech Stack:** Java 25, Gradle Kotlin DSL, Paper API 26.2 / Minecraft 1.21.11, MockBukkit 26.2, JUnit 5, Adventure/MiniMessage messages, manual `PluginContext` composition.

## Global Constraints

- Gate only the four vanilla fish items: `cod`, `salmon`, `tropical_fish`, and `pufferfish`; junk and treasure remain ungated.
- Config is opt-in under `fish-catch-gates`; no wildcard/category fallback, database state, reload command, GUI, or editor surface.
- Resolve aliases through `ProfessionCatalog`; `fisherman` must be stored as canonical `fishing`.
- Missing profession level means level 0 and blocks a configured catch.
- Use `PlayerFishEvent.State.CAUGHT_FISH`; below-level catches are cancelled at `EventPriority.NORMAL` with `ignoreCancelled = true`.
- The existing `JobPaymentListener` remains at `MONITOR`; cancelled catches must not invoke `ActionTypes.FISH` payment.
- Add `modularjobs.bypassfishcatch` with `default: op`.
- Invalid YAML entries warn and skip; invalid configuration must not fail plugin startup.
- Keep `api` free of Bukkit/Paper imports.
- Follow TDD: each production change is preceded by a failing behavior test, and the failure is observed before implementation.

## File Map

### Create

- `api/src/main/java/net/aincraft/profession/FishCatchGate.java` — immutable normalized gate record.
- `api/src/main/java/net/aincraft/service/FishCatchGateService.java` — read-only lookup contract.
- `api/src/test/java/net/aincraft/profession/FishCatchGateTest.java` — record and service contract tests.
- `paper/src/main/java/net/aincraft/profession/YamlFishCatchGateLoader.java` — YAML validation and canonicalization.
- `paper/src/main/java/net/aincraft/profession/FishCatchGateStore.java` — immutable item-key lookup.
- `paper/src/main/java/net/aincraft/profession/FishCatchGateListener.java` — event enforcement.
- `paper/src/test/java/net/aincraft/profession/YamlFishCatchGateLoaderTest.java` — loader/store behavior tests.
- `paper/src/test/java/net/aincraft/profession/FishCatchGateListenerTest.java` — event behavior tests.

### Modify

- `paper/src/main/java/net/aincraft/PluginContext.java` — load and register fish gate components.
- `paper/src/main/resources/config.yml` — commented opt-in fish gate examples.
- `paper/src/main/resources/plugin.yml` — bypass permission declaration.
- `README.md` — operator configuration and behavior.
- `CHANGELOG.md` — Unreleased added entry.
- `docs/living-specs/professions.md` — current profession-gate scope and invariants.

## Task 1: Add Paper-Free Fish Gate Contracts

**Files:**
- Create: `api/src/main/java/net/aincraft/profession/FishCatchGate.java`
- Create: `api/src/main/java/net/aincraft/service/FishCatchGateService.java`
- Create: `api/src/test/java/net/aincraft/profession/FishCatchGateTest.java`

**Interfaces:**
- Produces `FishCatchGate(String itemKey, String professionId, int minLevel)`.
- Produces `FishCatchGateService.gates()` and `FishCatchGateService.gateFor(String itemKey)`.
- Later tasks consume `FishCatchGate.itemKey()`, `.professionId()`, `.minLevel()`, and the lookup contract.

- [ ] **Step 1: Write the failing API test**

Create `FishCatchGateTest` with these two tests and a fixed service implementation:

```java
package net.aincraft.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.aincraft.service.FishCatchGateService;
import org.junit.jupiter.api.Test;

class FishCatchGateTest {

  private record FixedService(List<FishCatchGate> gates) implements FishCatchGateService {
    @Override
    public List<FishCatchGate> gates() {
      return gates;
    }

    @Override
    public Optional<FishCatchGate> gateFor(String itemKey) {
      return gates.stream()
          .filter(gate -> gate.itemKey().equalsIgnoreCase(itemKey))
          .findFirst();
    }
  }

  @Test
  void recordNormalizesCase() {
    FishCatchGate gate = new FishCatchGate("Tropical_Fish", "Fisherman", 20);

    assertEquals("tropical_fish", gate.itemKey());
    assertEquals("fisherman", gate.professionId());
    assertEquals(20, gate.minLevel());
  }

  @Test
  void serviceFindsGateCaseInsensitively() {
    FishCatchGateService service = new FixedService(
        List.of(new FishCatchGate("cod", "fishing", 1)));

    assertTrue(service.gateFor("COD").isPresent());
    assertEquals(1, service.gateFor("cod").orElseThrow().minLevel());
    assertTrue(service.gateFor("salmon").isEmpty());
  }
}
```

- [ ] **Step 2: Run the API test and verify the expected red failure**

Run:

```bash
./gradlew :api:test --tests net.aincraft.profession.FishCatchGateTest
```

Expected: compilation failure because `FishCatchGate`, `FishCatchGateService`, and their methods do not exist yet. If the test fails for a syntax or dependency error instead, correct the test before writing production code.

- [ ] **Step 3: Implement the minimal API contracts**

Create the record with the same normalization policy as `BlockBreakGate`, using `Locale.ROOT`:

```java
package net.aincraft.profession;

import java.util.Locale;
import org.jetbrains.annotations.NotNull;

public record FishCatchGate(
    @NotNull String itemKey,
    @NotNull String professionId,
    int minLevel
) {

  public FishCatchGate {
    itemKey = itemKey.toLowerCase(Locale.ROOT);
    professionId = professionId.toLowerCase(Locale.ROOT);
  }
}
```

Create the Paper-free service:

```java
package net.aincraft.service;

import java.util.List;
import java.util.Optional;
import net.aincraft.profession.FishCatchGate;
import org.jetbrains.annotations.NotNull;

public interface FishCatchGateService {

  @NotNull
  List<FishCatchGate> gates();

  @NotNull
  Optional<FishCatchGate> gateFor(@NotNull String itemKey);
}
```

- [ ] **Step 4: Run the API test and verify green**

Run the same `:api:test --tests net.aincraft.profession.FishCatchGateTest` command. Expected: both tests pass with no compilation warnings.

- [ ] **Step 5: Commit the contract unit**

```bash
git add api/src/main/java/net/aincraft/profession/FishCatchGate.java \
  api/src/main/java/net/aincraft/service/FishCatchGateService.java \
  api/src/test/java/net/aincraft/profession/FishCatchGateTest.java
git commit -m "feat: add fish catch gate contracts"
```

## Task 2: Parse and Store Fish Catch Gates

**Files:**
- Create: `paper/src/main/java/net/aincraft/profession/YamlFishCatchGateLoader.java`
- Create: `paper/src/main/java/net/aincraft/profession/FishCatchGateStore.java`
- Create: `paper/src/test/java/net/aincraft/profession/YamlFishCatchGateLoaderTest.java`

**Interfaces:**
- Consumes `FishCatchGate` and `FishCatchGateService` from Task 1.
- Produces `YamlFishCatchGateLoader(Logger).load(ConfigurationSection)`.
- Produces `FishCatchGateStore(List<FishCatchGate>)`, `isEmpty()`, `gates()`, and `gateFor(String)`.
- Later tasks consume `FishCatchGateStore.gateFor` and construct it from loader output.

- [ ] **Step 1: Write failing loader/store tests**

Use MockBukkit lifecycle exactly as the existing `YamlBlockBreakGateLoaderTest`. Cover valid entries and every invalid class in one deterministic test suite:

```java
package net.aincraft.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Logger;
import net.aincraft.test.MockBukkitSupport;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class YamlFishCatchGateLoaderTest {

  private YamlFishCatchGateLoader loader;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    loader = new YamlFishCatchGateLoader(Logger.getLogger("test"));
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void parsesFishEntriesAndCanonicalizesProfessionAlias() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("fish-catch-gates.cod.profession", "fisherman");
    config.set("fish-catch-gates.cod.level", 1);
    config.set("fish-catch-gates.tropical_fish.profession", "fishing");
    config.set("fish-catch-gates.tropical_fish.level", 20);

    List<FishCatchGate> gates = loader.load(config);

    assertEquals(2, gates.size());
    FishCatchGate cod = gates.stream()
        .filter(gate -> gate.itemKey().equals("cod"))
        .findFirst().orElseThrow();
    assertEquals("fishing", cod.professionId());
    assertEquals(1, cod.minLevel());
  }

  @Test
  void skipsUnknownNonFishProfessionAndInvalidLevelEntries() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("fish-catch-gates.not_a_real_material.profession", "fishing");
    config.set("fish-catch-gates.not_a_real_material.level", 1);
    config.set("fish-catch-gates.stone.profession", "fishing");
    config.set("fish-catch-gates.stone.level", 1);
    config.set("fish-catch-gates.cod.profession", "not_a_profession");
    config.set("fish-catch-gates.cod.level", 1);
    config.set("fish-catch-gates.salmon.profession", "fishing");
    config.set("fish-catch-gates.salmon.level", "high");
    config.set("fish-catch-gates.pufferfish.profession", "fishing");
    config.set("fish-catch-gates.pufferfish.level", 0);

    assertTrue(loader.load(config).isEmpty());
  }

  @Test
  void missingSectionYieldsEmptyList() {
    assertTrue(loader.load(new YamlConfiguration()).isEmpty());
  }

  @Test
  void storeLooksUpCaseInsensitively() {
    FishCatchGateStore store = new FishCatchGateStore(
        List.of(new FishCatchGate("cod", "fishing", 1)));

    assertTrue(store.gateFor("COD").isPresent());
    assertTrue(store.gateFor("salmon").isEmpty());
    assertFalse(store.isEmpty());
    assertTrue(new FishCatchGateStore(List.of()).isEmpty());
  }
}
```

- [ ] **Step 2: Run the loader test and verify the expected red failure**

Run:

```bash
./gradlew :paper:test --tests net.aincraft.profession.YamlFishCatchGateLoaderTest
```

Expected: compilation failure because the loader and store do not exist. Correct test/setup errors before implementing.

- [ ] **Step 3: Implement the minimal immutable store**

Use the existing block-gate store pattern, changing the key type to `itemKey`:

```java
public final class FishCatchGateStore implements FishCatchGateService {

  private final Map<String, FishCatchGate> byItem;

  public FishCatchGateStore(@NotNull List<FishCatchGate> gates) {
    this.byItem = gates.stream().collect(Collectors.toUnmodifiableMap(
        gate -> gate.itemKey().toLowerCase(Locale.ROOT), gate -> gate));
  }

  public boolean isEmpty() {
    return byItem.isEmpty();
  }

  @Override
  public @NotNull List<FishCatchGate> gates() {
    return List.copyOf(byItem.values());
  }

  @Override
  public @NotNull Optional<FishCatchGate> gateFor(@NotNull String itemKey) {
    return Optional.ofNullable(byItem.get(itemKey.toLowerCase(Locale.ROOT)));
  }
}
```

- [ ] **Step 4: Implement loader validation and canonicalization**

Use section `fish-catch-gates`. For each child configuration section:

1. `Material.matchMaterial(materialKey)` must resolve.
2. The material must be one of `Material.COD`, `Material.SALMON`, `Material.TROPICAL_FISH`, or `Material.PUFFERFISH`; reject `STONE` and all junk/treasure materials.
3. `ProfessionCatalog.resolve(entry.getString("profession"))` must be present; store `.id().toLowerCase(Locale.ROOT)`.
4. `entry.isInt("level")` must be true and `entry.getInt("level") > 0`.
5. Return `new FishCatchGate(materialKey.toLowerCase(Locale.ROOT), canonical, level)`.

Log warnings prefixed with `fish-catch-gates:` for every rejected entry and return an empty list when the section is absent. Do not throw for malformed configuration.

- [ ] **Step 5: Run loader tests and verify green**

Run the same `:paper:test --tests net.aincraft.profession.YamlFishCatchGateLoaderTest` command. Expected: all four tests pass.

- [ ] **Step 6: Commit the parsing unit**

```bash
git add paper/src/main/java/net/aincraft/profession/YamlFishCatchGateLoader.java \
  paper/src/main/java/net/aincraft/profession/FishCatchGateStore.java \
  paper/src/test/java/net/aincraft/profession/YamlFishCatchGateLoaderTest.java
git commit -m "feat: load fish catch gate configuration"
```

## Task 3: Enforce Fish Catch Eligibility

**Files:**
- Create: `paper/src/main/java/net/aincraft/profession/FishCatchGateListener.java`
- Create: `paper/src/test/java/net/aincraft/profession/FishCatchGateListenerTest.java`

**Interfaces:**
- Consumes `FishCatchGateStore` and `ProfessionService`.
- Produces `FishCatchGateListener(Store, ProfessionService)` and a Bukkit handler for `PlayerFishEvent`.
- Later wiring registers this listener; the existing `JobPaymentListener.onFish` observes the result at `MONITOR`.

- [ ] **Step 1: Write failing listener tests**

Create a `StubProfessionService` matching the existing block-break listener test: return a mutable `Map<String, OptionalInt>` from `level`, return `ProfessionCatalog.resolve` from `resolve`, and return empty experience/tracks plus `true` from `ensureTrack`. In `setUp`, start MockBukkit, create the store with `salmon -> fishing -> 10`, construct the listener, and add a player.

Construct real event objects using MockBukkit classes, not proxies:

```java
private PlayerFishEvent fishEvent(Material material, PlayerFishEvent.State state) {
  ServerMock server = MockBukkitSupport.mockServer();
  FishHookMock hook = new FishHookMock(server, UUID.randomUUID());
  Entity caught = state == PlayerFishEvent.State.CAUGHT_FISH
      ? new ItemEntityMock(server, UUID.randomUUID(), new ItemStack(material))
      : null;
  return new PlayerFishEvent(player, caught, hook, state);
}
```

Add focused tests:

```java
@Test
void belowRequiredLevelCancelsCatch() {
  professions.levels().put("fishing", OptionalInt.of(9));
  PlayerFishEvent event = fishEvent(Material.SALMON, PlayerFishEvent.State.CAUGHT_FISH);
  listener.onFish(event);
  assertTrue(event.isCancelled());
}

@Test
void atRequiredLevelAllowsCatch() {
  professions.levels().put("fishing", OptionalInt.of(10));
  PlayerFishEvent event = fishEvent(Material.SALMON, PlayerFishEvent.State.CAUGHT_FISH);
  listener.onFish(event);
  assertFalse(event.isCancelled());
}

@Test
void unjoinedProfessionCancelsCatch() {
  PlayerFishEvent event = fishEvent(Material.SALMON, PlayerFishEvent.State.CAUGHT_FISH);
  listener.onFish(event);
  assertTrue(event.isCancelled());
}

@Test
void unconfiguredFishAllowsCatch() {
  PlayerFishEvent event = fishEvent(Material.COD, PlayerFishEvent.State.CAUGHT_FISH);
  listener.onFish(event);
  assertFalse(event.isCancelled());
}

@Test
void nonCatchStateAllowsEvent() {
  PlayerFishEvent event = fishEvent(null, PlayerFishEvent.State.BITE);
  listener.onFish(event);
  assertFalse(event.isCancelled());
}
```

Also add a bypass test using a registered `PluginMock` attachment and permission `modularjobs.bypassfishcatch`, as in `BlockBreakGateListenerTest`; it must leave a below-level salmon event uncancelled.

- [ ] **Step 2: Run listener tests and verify the expected red failure**

Run:

```bash
./gradlew :paper:test --tests net.aincraft.profession.FishCatchGateListenerTest
```

Expected: compilation failure because `FishCatchGateListener` does not exist. If MockBukkit construction fails, fix the test to use the public `FishHookMock`/`ItemEntityMock` constructors before writing production code.

- [ ] **Step 3: Implement the minimal event listener**

Implement the following behavior:

```java
public final class FishCatchGateListener implements Listener {

  public static final String BYPASS_PERMISSION = "modularjobs.bypassfishcatch";

  private final FishCatchGateStore store;
  private final ProfessionService professionService;

  public FishCatchGateListener(
      @NotNull FishCatchGateStore store,
      @NotNull ProfessionService professionService) {
    this.store = store;
    this.professionService = professionService;
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onFish(final PlayerFishEvent event) {
    if (event.getPlayer().hasPermission(BYPASS_PERMISSION)
        || event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
      return;
    }
    if (!(event.getCaught() instanceof Item item)) {
      return;
    }
    String itemKey = item.getItemStack().getType().name().toLowerCase(Locale.ROOT);
    FishCatchGate gate = store.gateFor(itemKey).orElse(null);
    if (gate == null) {
      return;
    }
    Player player = event.getPlayer();
    OptionalInt level = professionService.level(player.getUniqueId(), gate.professionId());
    if (level.isEmpty() || level.getAsInt() < gate.minLevel()) {
      event.setCancelled(true);
      Messages.send(player, gateMessage(gate));
    }
  }

  private static String gateMessage(FishCatchGate gate) {
    return "<error>Level <primary>" + gate.minLevel()
        + " <error>" + gate.professionId() + " required to catch <secondary>"
        + gate.itemKey() + "</secondary>";
  }
}
```

- [ ] **Step 4: Run listener tests and verify green**

Run the same listener test command. Expected: all eligibility, state, unconfigured-item, and bypass tests pass.

- [ ] **Step 5: Commit the enforcement unit**

```bash
git add paper/src/main/java/net/aincraft/profession/FishCatchGateListener.java \
  paper/src/test/java/net/aincraft/profession/FishCatchGateListenerTest.java
git commit -m "feat: enforce fish catch profession levels"
```

## Task 4: Wire Configuration and Runtime Registration

**Files:**
- Modify: `paper/src/main/java/net/aincraft/PluginContext.java`
- Modify: `paper/src/main/resources/config.yml`
- Modify: `paper/src/main/resources/plugin.yml`

**Interfaces:**
- Consumes `YamlFishCatchGateLoader`, `FishCatchGateStore`, and `FishCatchGateListener` from Tasks 2–3.
- Produces startup loading from the existing `databaseConfig` configuration and listener registration in the existing `listenerList`.

- [ ] **Step 1: Add the runtime wiring and resource configuration**

In `PluginContext.createInto`, next to the existing block-gate setup, add:

```java
FishCatchGateStore fishCatchGateStore = new FishCatchGateStore(
    new YamlFishCatchGateLoader(plugin.getLogger()).load(databaseConfig));
```

Next to `listenerList.add(new BlockBreakGateListener(...))`, add:

```java
// Profession-gated fish catches (cancel below configured level)
listenerList.add(new FishCatchGateListener(fishCatchGateStore, professions.professionService));
```

Import the three fish-gate classes. Add this commented section to `config.yml` after `block-break-gates`:

```yaml
# Fish catching gates: minimum profession level required to catch a fish item.
# Fish keys are cod, salmon, tropical_fish, or pufferfish.
# Professions are catalog ids or aliases (fisherman resolves to fishing).
fish-catch-gates:
  # cod: { profession: fisherman, level: 1 }
  # salmon: { profession: fisherman, level: 10 }
  # tropical_fish: { profession: fisherman, level: 20 }
  # pufferfish: { profession: fisherman, level: 30 }
```

Add to `plugin.yml` permissions:

```yaml
  modularjobs.bypassfishcatch:
    description: Bypass profession-gated fish catching
    default: op
```

- [ ] **Step 2: Compile the affected modules**

Run:

```bash
./gradlew :api:compileJava :paper:compileJava
```

Expected: successful compilation and no Paper/API boundary violations.

- [ ] **Step 3: Commit runtime wiring**

```bash
git add paper/src/main/java/net/aincraft/PluginContext.java \
  paper/src/main/resources/config.yml \
  paper/src/main/resources/plugin.yml
git commit -m "feat: wire fish catch gates into plugin startup"
```

## Task 5: Document Operator-Facing Fish Gates

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/living-specs/professions.md`

**Interfaces:**
- Documents the exact `fish-catch-gates` keys, canonical alias behavior, denial semantics, and bypass permission already implemented in Tasks 2–4.

- [ ] **Step 1: Update README operator quick start**

Add a section directly after “Block breaking gates”:


````markdown
### Fish catching gates

Restrict a fish item to a minimum profession level (`fish-catch-gates` in `config.yml`):

```yaml
fish-catch-gates:
  cod: { profession: fisherman, level: 1 }
  salmon: { profession: fisherman, level: 10 }
  tropical_fish: { profession: fisherman, level: 20 }
  pufferfish: { profession: fisherman, level: 30 }
```

Below-level or unjoined players do not collect configured fish and receive no fish job payment. Staff bypass via `modularjobs.bypassfishcatch`. Junk and treasure are unaffected.
````

Add this Unreleased changelog bullet:

```markdown
- **Profession-gated fish catching**: `fish-catch-gates` restricts configured vanilla fish by profession level (bypass: `modularjobs.bypassfishcatch`).
```

Update `docs/living-specs/professions.md` with these concrete changes:

- Extend Intent to say operators can require minimum profession levels to catch configured vanilla fish, in addition to breaking materials.
- Add to In Scope: `FishCatchGate` API model plus YAML loader/store/listener; `fish-catch-gates`; and `modularjobs.bypassfishcatch`.
- Add to Invariants: only `cod`, `salmon`, `tropical_fish`, and `pufferfish` are eligible keys; `fisherman` resolves to `fishing`; missing level blocks; `CAUGHT_FISH` enforcement runs at `NORMAL` before payment; rejected catches do not pay.
- Add Current checklist entries for fish gate API/paper implementation, configuration/permission, and loader/listener tests.
- Add an explicit out-of-scope/future note that vanilla weighted loot tables are not modified; NMS/datapack pool filtering is not part of this feature.

- [ ] **Step 3: Review documentation consistency**

Check that all operator-facing text uses `fish-catch-gates`, the four exact material keys, `fisherman` as the accepted alias, `fishing` as canonical service id, and `modularjobs.bypassfishcatch` as the bypass. Do not claim that vanilla loot weights are changed; state that an ineligible caught item is rejected before collection/payment.

- [ ] **Step 4: Commit documentation**

```bash
git add README.md CHANGELOG.md docs/living-specs/professions.md
git commit -m "docs: document profession fish catch gates"
```

## Task 6: Full Verification and Smoke Checks

**Files:**
- No source changes expected unless verification exposes a defect.

- [ ] **Step 1: Run focused API and Paper tests**

```bash
./gradlew :api:test --tests net.aincraft.profession.FishCatchGateTest
./gradlew :paper:test --tests net.aincraft.profession.YamlFishCatchGateLoaderTest
./gradlew :paper:test --tests net.aincraft.profession.FishCatchGateListenerTest
```

Expected: all focused tests pass.

- [ ] **Step 2: Run the complete supported JVM test set**

```bash
./gradlew :api:test :common:test :paper:test
```

Expected: all tests pass with no failures.

- [ ] **Step 3: Build the plugin artifact**

```bash
./gradlew :paper:build
```

Expected: successful build and `paper/build/libs/paper-all.jar` exists.

- [ ] **Step 4: Verify the final feature contract**

Inspect the final diff and confirm all of these observable facts:

- A configured salmon gate at level 10 cancels a level-9/unjoined `CAUGHT_FISH` event.
- A level-10+ player and a bypass-permission player are not cancelled.
- Cod with no configured gate is unchanged.
- `BITE`/other non-catch states and non-item caught entities are unchanged.
- The loader rejects non-fish keys and malformed levels without throwing.
- Plugin startup loads the section and registers the listener.
- The payment listener remains at `MONITOR`, so cancelled catches do not pay.
- The parent checkout’s pre-existing `build.gradle.kts` modification remains untouched; only the isolated feature worktree changes are part of this implementation.
