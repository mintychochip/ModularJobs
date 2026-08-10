# Profession-Gated Block Breaking Implementation Plan

**Date:** 2026-08-07
**Status:** Superseded by `2026-08-10-modularjobs-azoth-integration.md`; historical ModularJobs-owned gate plan.
**Goal:** Block breaking is restricted by profession level via per-material config (`block-break-gates` in `config.yml`): a player below the required profession level cannot break the block (event cancelled + themed message), with an op-default bypass permission.

**Architecture:** A new `api`-level `BlockBreakGate` record (material key as `String`, profession id, min level) + `BlockBreakGateService`. In `paper`, a `YamlBlockBreakGateLoader` parses the config section into a cached `BlockBreakGateStore`, and a `BlockBreakGateListener` at `EventPriority.NORMAL` (before the `MONITOR` pay listener) looks up the gate for the broken material, checks `ProfessionService.level(player, professionId)`, and cancels + messages when below. Wired into `PluginContext` so the listener joins `created.listeners`.

**Tech Stack:** Java 21/25, PaperMC API (Bukkit `Material`, `BlockBreakEvent`, `Player`), MockBukkit 26.2 for listeners, JUnit 5 (Jupiter), Gradle Kotlin DSL, project's own `config.yml`/`YamlConfiguration` + `Messages` MiniMessage.

## Global Constraints

- `api` module is **pure** — no Bukkit/Paper imports allowed (`api/build.gradle.kts` deps: `common`, `adventure.api`, `jetbrains.annotations`). `Material` must NOT appear in `api`; use `String materialKey`.
- Java 21 language features OK (records, pattern matching, `switch` arrows, `List.of`); project uses `@NotNull`/`@Nullable` (`org.jetbrains.annotations`) in `api`, `@NullMarked` (`org.jspecify`) in `paper`.
- Gate key: profession id from the §8.1 catalog (`ProfessionCatalog.resolve`) — resolve the profession at load time; invalid → `logger.warning`, skip entry. Same for unknown `Material.matchMaterial(key)` and `level <= 0`.
- `level` parse: `config.isInt("level")`; if not an int → warn + skip.
- Material → `org.bukkit.Material.matchMaterial(String)`; missing/`AIR` → warn + skip.
- Empty/missing section → empty store; listener short-circuits; no behavior change.
- Bypass: `player.hasPermission("modularjobs.bypassblockbreak")`.
- Message: `Messages.send(player, "<error>... <primary>... <primary>... <secondary>...")` format — exact template in Task 3.
- Event priority: `EventPriority.NORMAL`, `ignoreCancelled = true`.
- An unjoined profession (`ProfessionService.level(...)` returns `OptionalInt.empty()`) is treated as level 0 → block.
- Tests: JUnit 5 (`org.junit.jupiter`), MockBukkit via `net.aincraft.test.MockBukkitSupport`; `@AfterEach MockBukkitSupport.unmockServer()`. No checkstyle/spotbugs gates per plan; project formatter/checkstyle runs once in Task 4.
- No shared mutable global state: store is immutable after load; listener stateless.
- Docs/changelog updated in Task 4 (`docs/database-schema.md` NOT touched — schema unchanged; update `CHANGELOG.md` and `README.md` only if they enumerate features — see Task 4).

---

### Task 1: api — `BlockBreakGate` record + `BlockBreakGateService`

**Files:**
- Create: `api/src/main/java/net/aincraft/profession/BlockBreakGate.java`
- Create: `api/src/main/java/net/aincraft/service/BlockBreakGateService.java`
- Test: `api/src/test/java/net/aincraft/profession/BlockBreakGateTest.java`

**Interfaces:**
- Produces:
  - `record BlockBreakGate(@NotNull String materialKey, @NotNull String professionId, int minLevel)` — compact constructor lowercases `materialKey` and `professionId`; `minLevel` stored as-is.
  - `interface BlockBreakGateService { @NotNull List<BlockBreakGate> gates(); @NotNull Optional<BlockBreakGate> gateFor(@NotNull String materialKey); }` — `gateFor` matches `materialKey.equalsIgnoreCase(...)`.

- [ ] **Step 1: Write the failing test**

`api/src/test/java/net/aincraft/profession/BlockBreakGateTest.java`:

```java
package net.aincraft.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.aincraft.service.BlockBreakGateService;
import org.junit.jupiter.api.Test;

class BlockBreakGateTest {

  private record FixedService(List<BlockBreakGate> gates) implements BlockBreakGateService {
    @Override
    public List<BlockBreakGate> gates() {
      return gates;
    }

    @Override
    public Optional<BlockBreakGate> gateFor(String materialKey) {
      return gates.stream()
          .filter(g -> g.materialKey().equalsIgnoreCase(materialKey))
          .findFirst();
    }
  }

  @Test
  void recordNormalizesCase() {
    BlockBreakGate gate = new BlockBreakGate("Diamond_Ore", "Mining", 30);
    assertEquals("diamond_ore", gate.materialKey());
    assertEquals("mining", gate.professionId());
    assertEquals(30, gate.minLevel());
  }

  @Test
  void serviceFindsGateCaseInsensitively() {
    BlockBreakGateService svc = new FixedService(
        List.of(new BlockBreakGate("diamond_ore", "mining", 30)));
    assertTrue(svc.gateFor("DIAMOND_ORE").isPresent());
    assertEquals(30, svc.gateFor("diamond_ore").orElseThrow().minLevel());
    assertTrue(svc.gateFor("stone").isEmpty());
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :api:test --tests net.aincraft.profession.BlockBreakGateTest -v`
Expected: FAIL — compilation error: `cannot find symbol` for `BlockBreakGate` / `BlockBreakGateService`.

- [ ] **Step 3: Write the api production code**

`api/src/main/java/net/aincraft/profession/BlockBreakGate.java`:

```java
package net.aincraft.profession;

import org.jetbrains.annotations.NotNull;

/**
 * One profession→material level gate: breaking the material requires at least
 * {@code minLevel} in {@code professionId} (a §8.1 catalog id).
 *
 * @param materialKey lowercase Minecraft material key, e.g. {@code diamond_ore}
 * @param professionId canonical profession id, e.g. {@code mining}
 * @param minLevel     minimum profession level required to break
 */
public record BlockBreakGate(
    @NotNull String materialKey,
    @NotNull String professionId,
    int minLevel
) {

  public BlockBreakGate {
    materialKey = materialKey.toLowerCase();
    professionId = professionId.toLowerCase();
  }
}
```

`api/src/main/java/net/aincraft/service/BlockBreakGateService.java`:

```java
package net.aincraft.service;

import java.util.List;
import java.util.Optional;
import net.aincraft.profession.BlockBreakGate;
import org.jetbrains.annotations.NotNull;

/**
 * Read-only access to the configured profession→material block-break gates.
 */
public interface BlockBreakGateService {

  /** All configured gates, catalog-irrelevant order. */
  @NotNull
  List<BlockBreakGate> gates();

  /** Gate for a material key (case-insensitive), or empty if not gated. */
  @NotNull
  Optional<BlockBreakGate> gateFor(@NotNull String materialKey);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :api:test --tests net.aincraft.profession.BlockBreakGateTest -v`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/net/aincraft/profession/BlockBreakGate.java \
        api/src/main/java/net/aincraft/service/BlockBreakGateService.java \
        api/src/test/java/net/aincraft/profession/BlockBreakGateTest.java
git commit -m "feat(api): block break gate model and service contract"
```

---

### Task 2: paper — `YamlBlockBreakGateLoader` + `BlockBreakGateStore`

**Files:**
- Create: `paper/src/main/java/net/aincraft/profession/YamlBlockBreakGateLoader.java`
- Create: `paper/src/main/java/net/aincraft/profession/BlockBreakGateStore.java`
- Test: `paper/src/test/java/net/aincraft/profession/YamlBlockBreakGateLoaderTest.java`

**Interfaces:**
- Consumes: `BlockBreakGate`, `BlockBreakGateService` (Task 1); `net.aincraft.config.YamlConfiguration` (exists); `ProfessionCatalog.resolve(String)` (exists); `org.bukkit.Material.matchMaterial(String)` (exists); `org.bukkit.plugin.Plugin.getLogger()`.
- Produces:
  - `final class BlockBreakGateStore implements BlockBreakGateService` — ctor `BlockBreakGateStore(List<BlockBreakGate> gates)`; stores `Map<String, BlockBreakGate>` by lowercased material key; `gateFor` delegates to map lookup; `gates()` returns immutable list. `isEmpty()` returns `gates.isEmpty()`.
  - `final class YamlBlockBreakGateLoader` — ctor `YamlBlockBreakGateLoader(Plugin plugin)`; method `List<BlockBreakGate> load(YamlConfiguration config)`; reads section `block-break-gates` (or empty list if `config.getConfigurationSection("block-break-gates") == null`); for each key: skip `configuration`-type keys (`config.isConfigurationSection(key)`); validate material/profession/level per Global Constraints; `logger.warning(...)` + skip on any invalid.

- [ ] **Step 1: Write the failing test**

`paper/src/test/java/net/aincraft/profession/YamlBlockBreakGateLoaderTest.java`:

```java
package net.aincraft.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Logger;
import net.aincraft.test.MockBukkitSupport;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class YamlBlockBreakGateLoaderTest {

  private YamlBlockBreakGateLoader loader;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    loader = new YamlBlockBreakGateLoader(Logger.getLogger("test"));
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void parsesValidGateEntries() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("block-break-gates.diamond_ore.profession", "mining");
    config.set("block-break-gates.diamond_ore.level", 30);
    config.set("block-break-gates.ancient_debris.profession", "mining");
    config.set("block-break-gates.ancient_debris.level", 40);

    List<BlockBreakGate> gates = loader.load(config);

    assertEquals(2, gates.size());
    BlockBreakGate diamond = gates.stream()
        .filter(g -> g.materialKey().equals("diamond_ore"))
        .findFirst().orElseThrow();
    assertEquals("mining", diamond.professionId());
    assertEquals(30, diamond.minLevel());
    assertTrue(Material.matchMaterial(diamond.materialKey()) != null);
  }

  @Test
  void skipsInvalidEntriesWithWarnings() {
    YamlConfiguration config = new YamlConfiguration();
    // Unknown material
    config.set("block-break-gates.not_a_real_material.profession", "mining");
    config.set("block-break-gates.not_a_real_material.level", 30);
    // Unknown profession
    config.set("block-break-gates.stone.profession", "not_a_profession");
    config.set("block-break-gates.stone.level", 5);
    // Non-int level
    config.set("block-break-gates.dirt.profession", "farming");
    config.set("block-break-gates.dirt.level", "high");
    // Zero/negative level
    config.set("block-break-gates.sand.profession", "mining");
    config.set("block-break-gates.sand.level", 0);

    List<BlockBreakGate> gates = loader.load(config);
    assertTrue(gates.isEmpty());
  }

  @Test
  void emptyOrMissingSectionYieldsEmptyList() {
    YamlConfiguration empty = new YamlConfiguration();
    assertTrue(loader.load(empty).isEmpty());
  }

  @Test
  void storeLooksUpCaseInsensitively() {
    BlockBreakGateStore store = new BlockBreakGateStore(
        List.of(new BlockBreakGate("diamond_ore", "mining", 30)));
    assertTrue(store.gateFor("DIAMOND_ORE").isPresent());
    assertTrue(store.gateFor("stone").isEmpty());
    assertFalse(store.isEmpty());
    assertTrue(new BlockBreakGateStore(List.of()).isEmpty());
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :paper:test --tests net.aincraft.profession.YamlBlockBreakGateLoaderTest -v`
Expected: FAIL — `cannot find symbol` for `YamlBlockBreakGateLoader` / `BlockBreakGateStore`.

- [ ] **Step 3: Write the loader + store**

`paper/src/main/java/net/aincraft/profession/BlockBreakGateStore.java`:

```java
package net.aincraft.profession;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.aincraft.service.BlockBreakGateService;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable in-memory gate table keyed by lowercase material key.
 */
public final class BlockBreakGateStore implements BlockBreakGateService {

  private final Map<String, BlockBreakGate> byMaterial;

  public BlockBreakGateStore(@NotNull List<BlockBreakGate> gates) {
    Map<String, BlockBreakGate> map = gates.stream()
        .collect(Collectors.toUnmodifiableMap(
            g -> g.materialKey().toLowerCase(Locale.ROOT), g -> g));
    this.byMaterial = Map.copyOf(map);
  }

  public boolean isEmpty() {
    return byMaterial.isEmpty();
  }

  @Override
  public @NotNull List<BlockBreakGate> gates() {
    return List.copyOf(byMaterial.values());
  }

  @Override
  public @NotNull Optional<BlockBreakGate> gateFor(@NotNull String materialKey) {
    return Optional.ofNullable(byMaterial.get(materialKey.toLowerCase(Locale.ROOT)));
  }
}
```

`paper/src/main/java/net/aincraft/profession/YamlBlockBreakGateLoader.java`:

```java
package net.aincraft.profession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import net.aincraft.config.YamlConfiguration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * Parses the {@code block-break-gates} section of config.yml into {@link BlockBreakGate}s.
 * Invalid entries (unknown material/profession, non-positive level) are skipped
 * with a warning; a missing section yields an empty list.
 */
public final class YamlBlockBreakGateLoader {

  private static final String SECTION = "block-break-gates";

  private final Logger logger;

  public YamlBlockBreakGateLoader(@NotNull Logger logger) {
    this.logger = logger;
  }

  public @NotNull List<BlockBreakGate> load(@NotNull YamlConfiguration config) {
    ConfigurationSection section = config.getConfigurationSection(SECTION);
    if (section == null) {
      return List.of();
    }
    List<BlockBreakGate> gates = new ArrayList<>();
    for (String materialKey : section.getKeys(false)) {
      if (section.isConfigurationSection(materialKey)) {
        parseEntry(section.getConfigurationSection(materialKey), materialKey).ifPresent(gates::add);
      }
    }
    return gates;
  }

  private java.util.Optional<BlockBreakGate> parseEntry(
      ConfigurationSection entry, String materialKey) {
    Material material = Material.matchMaterial(materialKey);
    if (material == null || material == Material.AIR) {
      logger.warning("block-break-gates: unknown material '" + materialKey + "' — skipping");
      return java.util.Optional.empty();
    }
    String professionId = entry.getString("profession");
    if (professionId == null
        || !ProfessionCatalog.resolve(professionId).isPresent()) {
      logger.warning("block-break-gates: '" + materialKey + "' has unknown profession '"
          + professionId + "' — skipping");
      return java.util.Optional.empty();
    }
    if (!entry.isInt("level")) {
      logger.warning("block-break-gates: '" + materialKey + "' level must be an int — skipping");
      return java.util.Optional.empty();
    }
    int level = entry.getInt("level");
    if (level <= 0) {
      logger.warning("block-break-gates: '" + materialKey + "' level must be > 0 — skipping");
      return java.util.Optional.empty();
    }
    String canonical = ProfessionCatalog.resolve(professionId).orElseThrow().id().toLowerCase(Locale.ROOT);
    return java.util.Optional.of(
        new BlockBreakGate(materialKey.toLowerCase(Locale.ROOT), canonical, level));
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :paper:test --tests net.aincraft.profession.YamlBlockBreakGateLoaderTest -v`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add paper/src/main/java/net/aincraft/profession/YamlBlockBreakGateLoader.java \
        paper/src/main/java/net/aincraft/profession/BlockBreakGateStore.java \
        paper/src/test/java/net/aincraft/profession/YamlBlockBreakGateLoaderTest.java
git commit -m "feat(paper): parse and store block break gates from config"
```

---

### Task 3: paper — `BlockBreakGateListener`

**Files:**
- Create: `paper/src/main/java/net/aincraft/profession/BlockBreakGateListener.java`
- Test: `paper/src/test/java/net/aincraft/profession/BlockBreakGateListenerTest.java`

**Interfaces:**
- Consumes: `BlockBreakGateStore` (Task 2), `net.aincraft.service.ProfessionService` (exists), `org.bukkit.block.Block`/`Player`/`BlockBreakEvent` (Bukkit), `Messages` (exists).
- Produces:
  - `final class BlockBreakGateListener implements Listener` — ctor `BlockBreakGateListener(BlockBreakGateStore store, ProfessionService professionService)`; public method `void onBlockBreak(BlockBreakEvent event)` annotated `@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)`.
  - `static final String BYPASS_PERMISSION = "modularjobs.bypassblockbreak"`.
  - Behavior: player `hasPermission(BYPASS_PERMISSION)` → return. `store.gateFor(block.getType().name().toLowerCase(Locale.ROOT))` empty → return. `professionService.level(player.getUniqueId(), gate.professionId())` empty or `< gate.minLevel()` → `event.setCancelled(true)` + `Messages.send(player, gateMessage(gate))`.
  - `gateMessage`: `"<error>Level <primary>" + gate.minLevel() + " <error>" + gate.professionId() + " required to break <secondary>" + gate.materialKey() + "</secondary>"`.
  - Material key: `block.getType().name().toLowerCase(Locale.ROOT)` — `Material.toString()` yields the SCREAMING_SNAKE name (`DIAMOND_ORE`), matching how `matchMaterial(key)` parses from config. No `Key`/namespaced resolution needed (config keys are un-namespaced keys).

- [ ] **Step 1: Write the failing test**

`paper/src/test/java/net/aincraft/profession/BlockBreakGateListenerTest.java`:

```java
package net.aincraft.profession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.aincraft.service.ProfessionService;
import net.aincraft.test.MockBukkitSupport;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockBreakGateListenerTest {

  private static final String BYPASS = "modularjobs.bypassblockbreak";

  private StubProfessionService professions;

  private record StubProfessionService(
      java.util.Map<String, OptionalInt> levels) implements ProfessionService {

    @Override
    public List<net.aincraft.profession.ProfessionDefinition> tracks() {
      return List.of();
    }

    @Override
    public Optional<net.aincraft.profession.ProfessionDefinition> resolve(String idOrAlias) {
      return net.aincraft.profession.ProfessionCatalog.resolve(idOrAlias);
    }

    @Override
    public OptionalInt level(UUID playerId, String professionIdOrAlias) {
      return levels.getOrDefault(professionIdOrAlias, OptionalInt.empty());
    }

    @Override
    public Optional<java.math.BigDecimal> experience(UUID playerId, String professionIdOrAlias) {
      return Optional.empty();
    }

    @Override
    public boolean ensureTrack(UUID playerId, String professionIdOrAlias) {
      return true;
    }
  }

  private BlockBreakGateListener listener;
  private Player player;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    professions = new StubProfessionService(new java.util.HashMap<>());
    BlockBreakGateStore store = new BlockBreakGateStore(List.of(
        new BlockBreakGate("diamond_ore", "mining", 30)));
    listener = new BlockBreakGateListener(store, professions);
    player = MockBukkitSupport.mockServer().addPlayer();
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void belowRequiredLevelCancelsBreak() {
    professions.levels().put("mining", OptionalInt.of(29));
    assertTrue(breakEvent().isCancelled());
  }

  @Test
  void atRequiredLevelAllowsBreak() {
    professions.levels().put("mining", OptionalInt.of(30));
    assertFalse(breakEvent().isCancelled());
  }

  @Test
  void aboveRequiredLevelAllowsBreak() {
    professions.levels().put("mining", OptionalInt.of(45));
    assertFalse(breakEvent().isCancelled());
  }

  @Test
  void unjoinedProfessionCancelsBreak() {
    // no level entry -> OptionalInt.empty -> treated as level 0
    assertTrue(breakEvent().isCancelled());
  }

  @Test
  void ungatedMaterialAllowsBreak() {
    Block block = mockBlock(Material.STONE);
    BlockBreakEvent event = new BlockBreakEvent(block, player);
    listener.onBlockBreak(event);
    assertFalse(event.isCancelled());
  }

  @Test
  void bypassPermissionAllowsBreak() {
    professions.levels().put("mining", OptionalInt.of(1));
    player.addAttachment(MockBukkitSupport.mockServer().getPluginManager().getPlugin("MockBukkit"))
        .setPermission(BYPASS, true);
    assertFalse(breakEvent().isCancelled());
  }

  private BlockBreakEvent breakEvent() {
    Block block = mockBlock(Material.DIAMOND_ORE);
    BlockBreakEvent event = new BlockBreakEvent(block, player);
    listener.onBlockBreak(event);
    return event;
  }

  private Block mockBlock(Material material) {
    var server = org.bukkit.Bukkit.getServer();
    org.bukkit.World world = server.getWorlds().isEmpty()
        ? server.createWorld(new org.bukkit.WorldCreator("world"))
        : server.getWorlds().getFirst();
    Block block = world.getBlockAt(0, 64, 0);
    block.setType(material);
    return block;
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :paper:test --tests net.aincraft.profession.BlockBreakGateListenerTest -v`
Expected: FAIL — `cannot find symbol` for `BlockBreakGateListener`.

- [ ] **Step 3: Write the listener**

`paper/src/main/java/net/aincraft/profession/BlockBreakGateListener.java`:

```java
package net.aincraft.profession;

import java.util.Locale;
import java.util.OptionalInt;
import net.aincraft.service.ProfessionService;
import net.aincraft.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Cancels breaking a material whose configured profession level requirement is unmet.
 *
 * <p>Runs at {@link EventPriority#NORMAL} so a cancelled break happens before the
 * {@code MONITOR} payment listener, keeping denied breaks out of pay/exploit logic.
 */
public final class BlockBreakGateListener implements Listener {

  public static final String BYPASS_PERMISSION = "modularjobs.bypassblockbreak";

  private final BlockBreakGateStore store;
  private final ProfessionService professionService;

  public BlockBreakGateListener(
      @NotNull BlockBreakGateStore store,
      @NotNull ProfessionService professionService) {
    this.store = store;
    this.professionService = professionService;
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onBlockBreak(final BlockBreakEvent event) {
    Player player = event.getPlayer();
    if (player.hasPermission(BYPASS_PERMISSION)) {
      return;
    }
    String materialKey = event.getBlock().getType().name().toLowerCase(Locale.ROOT);
    BlockBreakGate gate = store.gateFor(materialKey).orElse(null);
    if (gate == null) {
      return;
    }
    OptionalInt level = professionService.level(player.getUniqueId(), gate.professionId());
    if (level.isEmpty() || level.getAsInt() < gate.minLevel()) {
      event.setCancelled(true);
      Messages.send(player, gateMessage(gate));
    }
  }

  private static String gateMessage(BlockBreakGate gate) {
    return "<error>Level <primary>" + gate.minLevel()
        + " <error>" + gate.professionId()
        + " required to break <secondary>" + gate.materialKey() + "</secondary>";
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :paper:test --tests net.aincraft.profession.BlockBreakGateListenerTest -v`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add paper/src/main/java/net/aincraft/profession/BlockBreakGateListener.java \
        paper/src/test/java/net/aincraft/profession/BlockBreakGateListenerTest.java
git commit -m "feat(paper): cancel block breaks below profession level gate"
```

---

### Task 4: paper — wire into `PluginContext`, config.yml sample, plugin.yml permission

**Files:**
- Modify: `paper/src/main/java/net/aincraft/PluginContext.java` (composition root)
- Modify: `paper/src/main/resources/config.yml` (add commented sample section)
- Modify: `paper/src/main/resources/plugin.yml` (add bypass permission)
- Test: `paper/src/test/java/net/aincraft/PluginYmlProductionReadinessTest.java` — extend if the test asserts the full plugin.yml permission set; otherwise no test change. Inspect the existing test first (Step 3).

**Interfaces:**
- Consumes: `YamlBlockBreakGateLoader`, `BlockBreakGateStore`, `BlockBreakGateListener` (Tasks 2-3), `PluginContext.createInto(...)` pattern.
- Produces: `BlockBreakGateListener` added to `listenerList` inside `createInto`, constructed with `new BlockBreakGateStore(new YamlBlockBreakGateLoader(plugin.getLogger()).load(databaseConfig))`.

- [ ] **Step 1: Wire the gate into the composition root**

In `PluginContext.createInto(JavaPlugin plugin, PluginResources resources)`, immediately after the existing `ProfessionWiring professions = ProfessionWiring.create(domain.jobService);` line, add:

```java
    BlockBreakGateStore blockBreakGateStore = new BlockBreakGateStore(
        new YamlBlockBreakGateLoader(plugin.getLogger()).load(databaseConfig));
```

and, in the `List<Listener> listenerList = new ArrayList<>();` block (after the existing `listenerList.add(new UpgradePermissionRestoreListener(...))` line), add:

```java
    listenerList.add(new BlockBreakGateListener(blockBreakGateStore, professions.professionService));
```

Add the three imports at the top of the file:

```java
import net.aincraft.profession.BlockBreakGateListener;
import net.aincraft.profession.BlockBreakGateStore;
import net.aincraft.profession.YamlBlockBreakGateLoader;
```

- [ ] **Step 2: Add config.yml sample + plugin.yml permission**

In `paper/src/main/resources/config.yml`, after the `profession-apis` block, append:

```yaml
# Block breaking gates: minimum profession level required to break a material.
# Material names are Minecraft keys (diamond_ore, ancient_debris, ...).
# Professions are §8.1 catalog ids (mining, woodcutting, farming, ...).
block-break-gates:
  # diamond_ore: { profession: mining, level: 30 }
  # deepslate_diamond_ore: { profession: mining, level: 30 }
```

In `paper/src/main/resources/plugin.yml`, append under the `permissions:` map:

```yaml
  modularjobs.bypassblockbreak:
    description: Bypass profession-gated block breaking
    default: op
```

- [ ] **Step 3: Verify PluginYmlProductionReadinessTest still passes**

Run: `./gradlew :paper:test --tests net.aincraft.PluginYmlProductionReadinessTest -v`
Expected: PASS. If it fails because the test asserts an exact permission list, update the test to include `modularjobs.bypassblockbreak` in the expected set (read the test first).

- [ ] **Step 4: Run full paper + api test suites**

Run: `./gradlew :api:test :common:test :paper:test`
Expected: ALL PASS (including the 2 api + 10 paper new tests).

- [ ] **Step 5: Commit**

```bash
git add paper/src/main/java/net/aincraft/PluginContext.java \
        paper/src/main/resources/config.yml \
        paper/src/main/resources/plugin.yml \
        paper/src/test/java/net/aincraft/PluginYmlProductionReadinessTest.java
git commit -m "feat(paper): wire block break gates into plugin context"
```

---

### Task 5: docs + changelog

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `README.md` (only if it enumerates features — inspect first; if it does, add a one-line mention under the feature list)

**Interfaces:**
- Consumes: feature naming from the spec (`block-break-gates`, `modularjobs.bypassblockbreak`).

- [ ] **Step 1: Inspect CHANGELOG.md and README.md**

Run: `read CHANGELOG.md` and `read README.md`.
Check: does README.md list features/config? Does CHANGELOG.md have an Unreleased section?

- [ ] **Step 2: Add changelog entry**

In `CHANGELOG.md` top, under the latest version (or create an `## Unreleased` section if none), add:

```markdown
### Added
- Profession-gated block breaking: `block-break-gates` in config.yml restricts
  breaking a material to players at/above a configured profession level
  (bypass: `modularjobs.bypassblockbreak`).
```

- [ ] **Step 3: Add README feature mention (only if Step 1 shows a feature list)**

Add one line under the closest feature grouping in `README.md`:

```markdown
- Profession-gated block breaking (per-material level requirements in `config.yml`)
```

- [ ] **Step 4: Final verification**

Run: `./gradlew :api:test :common:test :paper:test`
Expected: ALL PASS.

- [ ] **Step 5: Commit**

```bash
git add CHANGELOG.md README.md
git commit -m "docs: changelog and README for block break gates"
```

---

## Self-Review

**Spec coverage:**
- Hard gate cancel + message → Task 3 listener.
- Check on break attempt (`BlockBreakEvent`) → Task 3.
- Per-material explicit `{profession, level}` config → Task 2 loader + Task 4 sample.
- Explicit profession id, decoupled from paying job → Task 2 (canonical id via `ProfessionCatalog.resolve`), Task 3 (gate stores profession id, checks `ProfessionService.level`).
- Message with reason → Task 3 `gateMessage`.
- Bypass permission `modularjobs.bypassblockbreak`, op default → Task 3 + Task 4 plugin.yml.
- Block all breaking outright → Task 3 (cancel).
- Bad config warn+skip; empty section = off → Task 2 (`skipsInvalidEntriesWithWarnings`, `emptyOrMissingSectionYieldsEmptyList`) + loader warnings.
- Fear of `MONITOR`-priority pay listener interaction → addressed: gate at `NORMAL` (spec §"Priority ordering"; Task 3 javadoc).
- No schema change → docs/database-schema.md untouched (explicit non-goal).
- Error handling: unjoined profession = level 0 → Task 3 `unjoinedProfessionCancelsBreak`.

**Placeholder scan:** No TBD/TODO/"similar to"/"add appropriate handling". Every step has concrete code/commands. The only conditional is Task 4 Step 3 and Task 5 Steps 1-3, which instruct to inspect first and give exact fallback code.

**Type consistency:**
- `BlockBreakGate` ctor/prod fields: `materialKey` (String), `professionId` (String), `minLevel` (int) — consistent across Tasks 1-3 and tests.
- `BlockBreakGateService.gateFor(String)` returns `Optional<BlockBreakGate>`; `BlockBreakGateStore` implements it — consistent.
- `BlockBreakGateStore` ctor takes `List<BlockBreakGate>`; `isEmpty()` exists — used in listener tests only via `gateFor`; used in wiring (Task 4) via no method requiring `isEmpty`.
- `YamlBlockBreakGateLoader.load(YamlConfiguration)` — Task 2 defines; Task 4 consumes with `databaseConfig` (which is `net.aincraft.config.YamlConfiguration`). Note: `createInto`'s `databaseConfig` is `YamlConfiguration` (the paper wrapper) — correct type.
- `BlockBreakGateListener(BLOCK_BREAK_GATE_STORE, ProfessionService)` ctor and `BYPASS_PERMISSION` constant — Task 3 defines, Task 4 consumes `professions.professionService` (exists, `ProfessionService`).
- Loader test ctor `YamlBlockBreakGateLoader(Logger.getLogger("test"))` matches the `Logger` ctor in Task 2 Step 3 — consistent (test passes a `Logger`; production passes `plugin.getLogger()`).
- Material key normalization: store lowercases; listener lowercases `Material.name()`; config keys lowercased in loader — all `Locale.ROOT`, consistent.

**Scope:** Single-feature plan sized for one implementation pass. No unrelated refactors.
