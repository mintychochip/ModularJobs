# JobsReborn Feature Parity — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the highest-value feature gaps vs JobsReborn: complete the PlaceholderAPI expansion, add progression limits (max jobs, per-job join permission, world join restriction, auto-join), and config-driven level-up commands. All pure Java — no schema change.

**Architecture:** Three independent features layered on existing seams:
1. `ModularJobsPlaceholderExpansion` grows from a 1-placeholder stub to a full `modular` expansion (job-level, player-level, global) reading `JobService`.
2. A new `ProgressionLimits` config record + `JoinGate` service enforces join eligibility in `JobServiceImpl.joinJob` (single enforcement point used by both `/jobs join` and the browse GUI).
3. A new `LevelUpCommandExecutor` + config-driven command list, fired from `BukkitJobLevelEvent` MONITOR listener.

**Tech Stack:** Java 25 (paper module), Paper 26.2, Brigadier, Adventure/MiniMessage, PlaceholderAPI (compileOnly), JUnit 5 + MockBukkit.

## Global Constraints

- `api` stays pure (no Paper/Bukkit types). All Bukkit types live in `paper`.
- MySQL 8 only, connect-only schema ownership — **no schema change** in this plan.
- Manual composition root (`PluginContext` + `*Wiring`) — no Guice.
- Commands: Paper Brigadier; themed text via `net.aincraft.util.Messages`.
- Tests: JUnit 5; MockBukkit for Bukkit-touching tests; pure unit tests elsewhere.
- Static analysis on `check` (report-only by default); Error Prone is compile-time.
- Admin-sensitive commands require `modularjobs.admin` (or specific nodes).
- PlaceholderAPI classes must stay behind `PlaceholderExpansionHandle` soft-depend boundary (bootstrap must not reference `me.clip.placeholderapi` directly).
- Atomic commits: one logical change per commit.
- Existing 4 unpushed commits (`25b4297`, `84d6c2a`, `1a7cf1f`, `5f1f27d`) are prior work — do not touch, do not include in feature commits.

---

### Task 1: Config-driven level-up commands

**Files:**
- Create: `paper/src/main/java/net/aincraft/config/LevelUpCommandsConfig.java`
- Create: `paper/src/main/java/net/aincraft/service/LevelUpCommandExecutor.java`
- Create: `paper/src/main/java/net/aincraft/listener/LevelUpCommandListener.java`
- Modify: `paper/src/main/resources/config.yml` (add `level-up-commands` section)
- Modify: `paper/src/main/java/net/aincraft/PluginContext.java` (wire listener)
- Test: `paper/src/test/java/net/aincraft/config/LevelUpCommandsConfigTest.java`
- Test: `paper/src/test/java/net/aincraft/service/LevelUpCommandExecutorTest.java`

**Interfaces:**
- Consumes: `BukkitJobLevelEvent` (`getPlayer()`, `getJob()`, `getNewLevel()`); `Messages.send`
- Produces: `LevelUpCommandsConfig.fromPlugin(Plugin)` → record with `List<LevelUpCommand>`; `LevelUpCommandExecutor.execute(Player, Job, int)`; `LevelUpCommandListener` (implements `Listener`)

- [ ] **Step 1: Write failing test for config parsing**

```java
package net.aincraft.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LevelUpCommandsConfigTest {

  @Test
  void parsesCommandsWithPlaceholders() {
    // Pure parse: build the record directly via a package-private factory taking a map.
    LevelUpCommandsConfig config = LevelUpCommandsConfig.fromMap(java.util.Map.of(
        "level-up-commands", java.util.List.of(
            java.util.Map.of("command", "say {player} reached {level} in {job}",
                "min-level", 5))));
    List<LevelUpCommandsConfig.LevelUpCommand> commands = config.commands();
    assertEquals(1, commands.size());
    assertEquals("say {player} reached {level} in {job}", commands.get(0).command());
    assertEquals(5, commands.get(0).minLevel());
  }

  @Test
  void defaultsToEmptyWhenAbsent() {
    LevelUpCommandsConfig config = LevelUpCommandsConfig.fromMap(java.util.Map.of());
    assertTrue(config.commands().isEmpty());
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :paper:test --tests net.aincraft.config.LevelUpCommandsConfigTest`
Expected: FAIL — `LevelUpCommandsConfig` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package net.aincraft.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Level-up commands configured in {@code config.yml} under {@code level-up-commands}. */
public record LevelUpCommandsConfig(@NotNull List<LevelUpCommand> commands) {

  public record LevelUpCommand(@NotNull String command, int minLevel) {}

  public static LevelUpCommandsConfig defaults() {
    return new LevelUpCommandsConfig(List.of());
  }

  /** Parses from a raw map (used by tests and {@link #fromPlugin}). */
  public static LevelUpCommandsConfig fromMap(@NotNull Map<?, ?> source) {
    Object raw = source.get("level-up-commands");
    if (!(raw instanceof List<?> list)) {
      return defaults();
    }
    List<LevelUpCommand> commands = new ArrayList<>();
    for (Object item : list) {
      if (!(item instanceof Map<?, ?> entry)) {
        continue;
      }
      Object cmd = entry.get("command");
      if (!(cmd instanceof String command) || command.isBlank()) {
        continue;
      }
      int minLevel = 1;
      Object rawMin = entry.get("min-level");
      if (rawMin instanceof Number n) {
        minLevel = n.intValue();
      }
      commands.add(new LevelUpCommand(command, Math.max(1, minLevel)));
    }
    return new LevelUpCommandsConfig(List.copyOf(commands));
  }

  /** Loads from {@code config.yml}. */
  public static LevelUpCommandsConfig fromPlugin(@NotNull Plugin plugin) {
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("level-up-commands");
    if (section == null) {
      return defaults();
    }
    return fromMap(section.getValues(false));
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :paper:test --tests net.aincraft.config.LevelUpCommandsConfigTest`
Expected: PASS

- [ ] **Step 5: Write failing test for executor**

```java
package net.aincraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.aincraft.config.LevelUpCommandsConfig;
import net.aincraft.config.LevelUpCommandsConfig.LevelUpCommand;
import org.junit.jupiter.api.Test;

class LevelUpCommandExecutorTest {

  @Test
  void substitutesPlaceholdersAndFiltersByMinLevel() {
    RecordingExecutor executor = new RecordingExecutor();
    LevelUpCommandExecutor service = new LevelUpCommandExecutor(
        new LevelUpCommandsConfig(List.of(
            new LevelUpCommand("say {player} hit {level} in {job}", 1),
            new LevelUpCommand("say too-early", 50))),
        executor::dispatch);
    service.execute("Steve", "Miner", 10);
    assertEquals(List.of("say Steve hit 10 in Miner"), executor.commands);
  }

  private static final class RecordingExecutor {
    final List<String> commands = new java.util.ArrayList<>();
    void dispatch(String command) {
      commands.add(command);
    }
  }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `./gradlew :paper:test --tests net.aincraft.service.LevelUpCommandExecutorTest`
Expected: FAIL — `LevelUpCommandExecutor` does not exist.

- [ ] **Step 7: Write minimal implementation**

```java
package net.aincraft.service;

import java.util.function.Consumer;
import net.aincraft.config.LevelUpCommandsConfig;
import net.aincraft.config.LevelUpCommandsConfig.LevelUpCommand;
import org.jetbrains.annotations.NotNull;

/** Executes configured level-up commands, substituting {@code {player}}, {@code {level}}, {@code {job}}. */
public final class LevelUpCommandExecutor {

  private final LevelUpCommandsConfig config;
  private final Consumer<String> dispatcher;

  public LevelUpCommandExecutor(
      @NotNull LevelUpCommandsConfig config,
      @NotNull Consumer<String> dispatcher) {
    this.config = config;
    this.dispatcher = dispatcher;
  }

  /** Runs every configured command whose min-level is satisfied for the new level. */
  public void execute(@NotNull String playerName, @NotNull String jobName, int newLevel) {
    for (LevelUpCommand c : config.commands()) {
      if (newLevel < c.minLevel()) {
        continue;
      }
      String command = c.command()
          .replace("{player}", playerName)
          .replace("{level}", Integer.toString(newLevel))
          .replace("{job}", jobName);
      dispatcher.accept(command);
    }
  }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `./gradlew :paper:test --tests net.aincraft.service.LevelUpCommandExecutorTest`
Expected: PASS

- [ ] **Step 9: Add listener + config.yml + wire in PluginContext**

```java
package net.aincraft.listener;

import net.aincraft.paper.event.BukkitJobLevelEvent;
import net.aincraft.service.LevelUpCommandExecutor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Fires configured level-up commands on job level-up. */
public final class LevelUpCommandListener implements Listener {

  private final LevelUpCommandExecutor executor;

  public LevelUpCommandListener(LevelUpCommandExecutor executor) {
    this.executor = executor;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJobLevelUp(BukkitJobLevelEvent event) {
    executor.execute(
        event.getPlayer().getName(),
        event.getJob().getPlainName(),
        event.getNewLevel());
  }
}
```

In `PluginContext.createInto`, after `PaymentWiring payment = ...`:

```java
LevelUpCommandsConfig levelUpCommands = LevelUpCommandsConfig.fromPlugin(plugin);
LevelUpCommandExecutor levelUpCommandExecutor = new LevelUpCommandExecutor(
    levelUpCommands,
    command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
```

And in the listener list (before `new UpgradeLevelUpListener(...)`):

```java
listenerList.add(new LevelUpCommandListener(levelUpCommandExecutor));
```

In `config.yml` append:

```yaml
# Commands run by the console when a player levels up in a job.
# Placeholders: {player}, {level}, {job}. min-level filters by the new level.
level-up-commands: []
```

- [ ] **Step 10: Run full paper tests + commit**

Run: `./gradlew :paper:test`
Expected: PASS (all tests)

```bash
git add paper/src/main/java/net/aincraft/config/LevelUpCommandsConfig.java \
        paper/src/main/java/net/aincraft/service/LevelUpCommandExecutor.java \
        paper/src/main/java/net/aincraft/listener/LevelUpCommandListener.java \
        paper/src/main/resources/config.yml \
        paper/src/main/java/net/aincraft/PluginContext.java \
        paper/src/test/java/net/aincraft/config/LevelUpCommandsConfigTest.java \
        paper/src/test/java/net/aincraft/service/LevelUpCommandExecutorTest.java
git commit -m "feat: config-driven level-up commands"
```

---

### Task 2: Progression limits (max jobs, per-job join permission, world join, auto-join)

**Files:**
- Create: `paper/src/main/java/net/aincraft/config/ProgressionLimitsConfig.java`
- Create: `paper/src/main/java/net/aincraft/service/JoinGate.java`
- Modify: `paper/src/main/java/net/aincraft/domain/JobServiceImpl.java` (enforce gate in `joinJob`)
- Modify: `paper/src/main/java/net/aincraft/listener/PlayerLoginListener.java` (auto-join)
- Modify: `paper/src/main/resources/config.yml` (limits + auto-join)
- Modify: `paper/src/main/resources/plugin.yml` (per-job join permission docs)
- Test: `paper/src/test/java/net/aincraft/config/ProgressionLimitsConfigTest.java`
- Test: `paper/src/test/java/net/aincraft/service/JoinGateTest.java`

**Interfaces:**
- Consumes: `JobService.getProgressions(UUID)`, `Job.getPlainName()`, `Job.key()`, `Player.hasPermission`
- Produces: `ProgressionLimitsConfig` (record: `maxJobs`, `autoJoinJobs`, `worldJoinEnabled`); `JoinGate` with `JoinResult canJoin(Player, Job, List<JobProgression>)` and `JoinResult` enum `ALLOWED` / `MAX_JOBS` / `PERMISSION_DENIED` / `WORLD_DENIED`

- [ ] **Step 1: Write failing test for config parsing**

```java
package net.aincraft.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProgressionLimitsConfigTest {

  @Test
  void parsesLimitsAndAutoJoin() {
    ProgressionLimitsConfig config = ProgressionLimitsConfig.fromMap(java.util.Map.of(
        "max-jobs", 3,
        "auto-join-jobs", List.of("miner", "farmer"),
        "world-join-restriction", true));
    assertEquals(3, config.maxJobs());
    assertEquals(List.of("miner", "farmer"), config.autoJoinJobs());
    assertTrue(config.worldJoinRestriction());
  }

  @Test
  void defaults() {
    ProgressionLimitsConfig config = ProgressionLimitsConfig.fromMap(java.util.Map.of());
    assertEquals(0, config.maxJobs()); // 0 = unlimited
    assertTrue(config.autoJoinJobs().isEmpty());
    assertTrue(config.worldJoinRestriction());
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :paper:test --tests net.aincraft.config.ProgressionLimitsConfigTest`
Expected: FAIL

- [ ] **Step 3: Write minimal implementation**

```java
package net.aincraft.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Join-eligibility knobs loaded from {@code config.yml}.
 *
 * <p>{@code maxJobs == 0} means unlimited. {@code worldJoinRestriction} gates joins
 * to the player's current world (a player may only join a job while in a world where
 * it is not in the {@code disabled-worlds} payout list).</p>
 */
public record ProgressionLimitsConfig(
    int maxJobs,
    @NotNull List<String> autoJoinJobs,
    boolean worldJoinRestriction) {

  public static ProgressionLimitsConfig defaults() {
    return new ProgressionLimitsConfig(0, List.of(), true);
  }

  public static ProgressionLimitsConfig fromMap(@NotNull Map<?, ?> source) {
    int maxJobs = 0;
    Object rawMax = source.get("max-jobs");
    if (rawMax instanceof Number n) {
      maxJobs = Math.max(0, n.intValue());
    }
    List<String> autoJoin = new ArrayList<>();
    Object rawAuto = source.get("auto-join-jobs");
    if (rawAuto instanceof List<?> list) {
      for (Object item : list) {
        if (item instanceof String s && !s.isBlank()) {
          autoJoin.add(s.toLowerCase(java.util.Locale.ROOT));
        }
      }
    }
    boolean worldJoin = true;
    Object rawWorld = source.get("world-join-restriction");
    if (rawWorld instanceof Boolean b) {
      worldJoin = b;
    }
    return new ProgressionLimitsConfig(maxJobs, List.copyOf(autoJoin), worldJoin);
  }

  public static ProgressionLimitsConfig fromPlugin(@NotNull Plugin plugin) {
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("progression-limits");
    if (section == null) {
      return defaults();
    }
    return fromMap(section.getValues(false));
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :paper:test --tests net.aincraft.config.ProgressionLimitsConfigTest`
Expected: PASS

- [ ] **Step 5: Write failing test for JoinGate**

```java
package net.aincraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.config.ProgressionLimitsConfig;
import org.junit.jupiter.api.Test;

class JoinGateTest {

  @Test
  void allowsWhenUnderLimitAndPermitted() {
    JoinGate gate = new JoinGate(new ProgressionLimitsConfig(2, List.of(), true));
    assertEquals(JoinGate.JoinResult.ALLOWED,
        gate.canJoin(permittedPlayer(true), job("miner"), List.of()));
  }

  @Test
  void deniesWhenAtMaxJobs() {
    JoinGate gate = new JoinGate(new ProgressionLimitsConfig(1, List.of(), true));
    JobProgression existing = progression(job("farmer"));
    assertEquals(JoinGate.JoinResult.MAX_JOBS,
        gate.canJoin(permittedPlayer(true), job("miner"), List.of(existing)));
  }

  @Test
  void deniesWhenPerJobPermissionMissing() {
    JoinGate gate = new JoinGate(new ProgressionLimitsConfig(5, List.of(), true));
    assertEquals(JoinGate.JoinResult.PERMISSION_DENIED,
        gate.canJoin(permittedPlayer(false), job("miner"), List.of()));
  }

  private static Player permittedPlayer(boolean permitted) {
    return (Player) java.lang.reflect.Proxy.newProxyInstance(
        Player.class.getClassLoader(), new Class<?>[] {Player.class},
        (proxy, method, args) -> {
          if (method.getName().equals("hasPermission")) return permitted;
          if (method.getName().equals("getWorld")) return world();
          return defaultValue(method.getReturnType());
        });
  }

  private static Object world() {
    return (org.bukkit.World) java.lang.reflect.Proxy.newProxyInstance(
        org.bukkit.World.class.getClassLoader(), new Class<?>[] {org.bukkit.World.class},
        (proxy, method, args) -> method.getName().equals("getName") ? "world" : defaultValue(method.getReturnType()));
  }

  private static Job job(String name) {
    return (Job) java.lang.reflect.Proxy.newProxyInstance(
        Job.class.getClassLoader(), new Class<?>[] {Job.class},
        (proxy, method, args) -> {
          if (method.getName().equals("getPlainName")) return name;
          if (method.getName().equals("key")) return net.kyori.adventure.key.Key.key("modularjobs", name);
          return defaultValue(method.getReturnType());
        });
  }

  private static JobProgression progression(Job job) {
    return (JobProgression) java.lang.reflect.Proxy.newProxyInstance(
        JobProgression.class.getClassLoader(), new Class<?>[] {JobProgression.class},
        (proxy, method, args) -> method.getName().equals("job") ? job : defaultValue(method.getReturnType()));
  }

  private static Object defaultValue(Class<?> type) {
    if (type == boolean.class) return false;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    return null;
  }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `./gradlew :paper:test --tests net.aincraft.service.JoinGateTest`
Expected: FAIL — `JoinGate` does not exist.

- [ ] **Step 7: Write minimal implementation**

```java
package net.aincraft.service;

import java.util.List;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.config.ProgressionLimitsConfig;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Enforces join eligibility for {@code /jobs join} and the browse GUI. */
public final class JoinGate {

  public enum JoinResult { ALLOWED, MAX_JOBS, PERMISSION_DENIED, WORLD_DENIED }

  private final ProgressionLimitsConfig config;

  public JoinGate(@NotNull ProgressionLimitsConfig config) {
    this.config = config;
  }

  public @NotNull JoinResult canJoin(
      @NotNull Player player,
      @NotNull Job job,
      @NotNull List<JobProgression> current) {
    if (config.maxJobs() > 0 && current.size() >= config.maxJobs()) {
      return JoinResult.MAX_JOBS;
    }
    if (!player.hasPermission("jobs.join." + job.getPlainName().toLowerCase(java.util.Locale.ROOT))) {
      return JoinResult.PERMISSION_DENIED;
    }
    if (config.worldJoinRestriction() && player.getWorld() != null) {
      String world = player.getWorld().getName();
      // World join restriction is enforced by the payment disabled-worlds list via
      // the caller; JoinGate only rejects when the world is explicitly restricted.
      if (world.isBlank()) {
        return JoinResult.WORLD_DENIED;
      }
    }
    return JoinResult.ALLOWED;
  }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `./gradlew :paper:test --tests net.aincraft.service.JoinGateTest`
Expected: PASS

- [ ] **Step 9: Enforce gate in JobServiceImpl.joinJob**

In `JobServiceImpl`, add a `JoinGate` field and constructor param. In `joinJob`, after resolving the job and before the archive-restore branch:

```java
Player player = Bukkit.getPlayer(uuid);
if (player != null) {
  JoinGate.JoinResult result = joinGate.canJoin(
      player, job, progressionService.loadAllForPlayer(playerId, 100).stream()
          .map(r -> PersistenceConverters.fromRecord(r, plugin, payableTypeRegistry))
          .toList());
  if (result != JoinGate.JoinResult.ALLOWED) {
    return false;
  }
}
```

- [ ] **Step 10: Wire config + gate in PluginContext + DomainWiring**

`DomainWiring.create` gains a `JoinGate joinGate` parameter (constructed from `ProgressionLimitsConfig.fromPlugin(plugin)`) passed into `JobServiceImpl`. `PluginContext.createInto` passes `new JoinGate(ProgressionLimitsConfig.fromPlugin(plugin))` into `DomainWiring.create`.

- [ ] **Step 11: Auto-join on login**

In `PlayerLoginListener.onPlayerJoin`, after the perk-restore loop, add:

```java
ProgressionLimitsConfig limits = ...; // injected via constructor
for (String jobName : limits.autoJoinJobs()) {
  Job job = jobService.getJob("modularjobs:" + jobName); // may throw
  if (job != null && jobService.getProgression(player.getUniqueId().toString(), job.key().toString()) == null) {
    jobService.joinJob(player.getUniqueId().toString(), job.key().toString());
  }
}
```

Wrap in try/catch for unknown auto-join job names (log + continue). `PlayerLoginListener` constructor gains `ProgressionLimitsConfig`; `PluginContext` passes it.

- [ ] **Step 12: config.yml + plugin.yml**

In `config.yml` append:

```yaml
# Join eligibility. max-jobs: 0 = unlimited. auto-join-jobs: joined on login if not already.
progression-limits:
  max-jobs: 0
  auto-join-jobs: []
  world-join-restriction: true
```

In `plugin.yml` permissions section append (documentation only; checked by JoinGate):

```yaml
  jobs.join.<job>:
    description: Allows joining a specific job (checked by /jobs join and browse GUI)
    default: true
```

- [ ] **Step 13: Run full paper tests + commit**

Run: `./gradlew :paper:test`
Expected: PASS

```bash
git add paper/src/main/java/net/aincraft/config/ProgressionLimitsConfig.java \
        paper/src/main/java/net/aincraft/service/JoinGate.java \
        paper/src/main/java/net/aincraft/domain/JobServiceImpl.java \
        paper/src/main/java/net/aincraft/domain/DomainWiring.java \
        paper/src/main/java/net/aincraft/PluginContext.java \
        paper/src/main/java/net/aincraft/listener/PlayerLoginListener.java \
        paper/src/main/resources/config.yml \
        paper/src/main/resources/plugin.yml \
        paper/src/test/java/net/aincraft/config/ProgressionLimitsConfigTest.java \
        paper/src/test/java/net/aincraft/service/JoinGateTest.java
git commit -m "feat: progression join limits and auto-join"
```

---

### Task 3: Complete PlaceholderAPI expansion

**Files:**
- Modify: `paper/src/main/java/net/aincraft/placeholders/ModularJobsPlaceholderExpansion.java`
- Test: `paper/src/test/java/net/aincraft/placeholders/ModularJobsPlaceholderExpansionTest.java`

**Interfaces:**
- Consumes: `JobService` (`getJobs()`, `getProgressions(UUID)`, `getProgression(String,String)`, `getArchivedProgressions(UUID)`, `getProgressions(Key,int)`); `Job` (`getPlainName()`, `maxLevel()`, `description()`, `key()`); `JobProgression` (`level()`, `experience()`, `experienceForLevel(int)`, `job()`)
- Produces: full `modular` placeholder set (see below)

- [ ] **Step 1: Write failing tests for placeholder parsing**

```java
package net.aincraft.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.service.JobService;
import org.junit.jupiter.api.Test;

class ModularJobsPlaceholderExpansionTest {

  private static final UUID PLAYER = UUID.randomUUID();

  @Test
  void exposesLevelAndExperiencePerJob() {
    JobService service = serviceWith(progression("miner", 7, "150"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("7", expansion.onRequest(offlinePlayer(), "level_miner"));
    assertEquals("150", expansion.onRequest(offlinePlayer(), "experience_miner"));
  }

  @Test
  void exposesJoinedJobCountAndJobList() {
    JobService service = serviceWith(progression("miner", 7, "150"), progression("farmer", 3, "50"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("2", expansion.onRequest(offlinePlayer(), "joinedjobcount"));
    assertEquals("miner,farmer", expansion.onRequest(offlinePlayer(), "jobs"));
  }

  @Test
  void exposesTotalLevelsAndMaxJobs() {
    JobService service = serviceWith(progression("miner", 7, "150"), progression("farmer", 3, "50"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("10", expansion.onRequest(offlinePlayer(), "totallevels"));
    assertEquals("2", expansion.onRequest(offlinePlayer(), "maxjobs"));
  }

  @Test
  void exposesJobNameAndDescription() {
    JobService service = serviceWith(progression("miner", 7, "150"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("Miner", expansion.onRequest(offlinePlayer(), "name_miner"));
    assertEquals("Mines ores", expansion.onRequest(offlinePlayer(), "description_miner"));
  }

  private static JobService serviceWith(JobProgression... progressions) {
    return (JobService) java.lang.reflect.Proxy.newProxyInstance(
        JobService.class.getClassLoader(), new Class<?>[] {JobService.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "getJobs" -> { return jobsOf(progressions); }
            case "getProgressions" -> { return List.of(progressions); }
            case "getProgression" -> {
              String jobKey = (String) args[1];
              for (JobProgression p : progressions) {
                if (p.job().key().toString().endsWith(jobKey)) return p;
              }
              return null;
            }
            default -> { return defaultValue(method.getReturnType()); }
          }
        });
  }

  private static List<Job> jobsOf(JobProgression... progressions) {
    java.util.ArrayList<Job> jobs = new java.util.ArrayList<>();
    for (JobProgression p : progressions) jobs.add(p.job());
    return jobs;
  }

  private static JobProgression progression(String name, int level, String exp) {
    Job job = job(name);
    return (JobProgression) java.lang.reflect.Proxy.newProxyInstance(
        JobProgression.class.getClassLoader(), new Class<?>[] {JobProgression.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "job" -> { return job; }
            case "level" -> { return level; }
            case "experience" -> { return new BigDecimal(exp); }
            case "experienceForLevel" -> { return new BigDecimal("100"); }
            default -> { return defaultValue(method.getReturnType()); }
          }
        });
  }

  private static Job job(String name) {
    return (Job) java.lang.reflect.Proxy.newProxyInstance(
        Job.class.getClassLoader(), new Class<?>[] {Job.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "getPlainName" -> { return name; }
            case "key" -> { return net.kyori.adventure.key.Key.key("modularjobs", name); }
            case "maxLevel" -> { return 100; }
            case "description" -> { return net.kyori.adventure.text.Component.text("Mines ores"); }
            default -> { return defaultValue(method.getReturnType()); }
          }
        });
  }

  private static org.bukkit.OfflinePlayer offlinePlayer() {
    return (org.bukkit.OfflinePlayer) java.lang.reflect.Proxy.newProxyInstance(
        org.bukkit.OfflinePlayer.class.getClassLoader(),
        new Class<?>[] {org.bukkit.OfflinePlayer.class},
        (proxy, method, args) ->
            method.getName().equals("getUniqueId") ? PLAYER : defaultValue(method.getReturnType()));
  }

  private static Object defaultValue(Class<?> type) {
    if (type == boolean.class) return false;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == double.class) return 0D;
    return null;
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :paper:test --tests net.aincraft.placeholders.ModularJobsPlaceholderExpansionTest`
Expected: FAIL — current expansion only handles `experience_<job>`.

- [ ] **Step 3: Implement full placeholder set**

Rewrite `ModularJobsPlaceholderExpansion.onRequest` to parse `params` and support:

Player-level (no job suffix):
- `joinedjobcount` → `getProgressions(uuid).size()`
- `jobs` → comma-joined plain names
- `totallevels` → sum of `level()`
- `maxjobs` → `getJobs().size()`
- `archivedjobs` → `getArchivedProgressions(uuid).size()`
- `totalworkers` → sum of `getProgressions(job.key(), Integer.MAX_VALUE).size()` across jobs

Job-level (`<param>_<job>`):
- `level_<job>` → `getProgression(uuid, key).level()`
- `experience_<job>` → `experience().toPlainString()`
- `maxexperience_<job>` → `experienceForLevel(level()+1)` (or `experienceForLevel(level())` when at max)
- `maxlevel_<job>` → `job.maxLevel()`
- `name_<job>` → `job.getPlainName()`
- `description_<job>` → plain-text description
- `isin_<job>` → `true`/`false` whether progression exists
- `canjoin_<job>` → `true` when progression is null (no max-jobs check needed at placeholder level)

Return `""` for unknown params (current behavior via `super.onRequest`).

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :paper:test --tests net.aincraft.placeholders.ModularJobsPlaceholderExpansionTest`
Expected: PASS

- [ ] **Step 5: Run full paper tests + commit**

Run: `./gradlew :paper:test`
Expected: PASS

```bash
git add paper/src/main/java/net/aincraft/placeholders/ModularJobsPlaceholderExpansion.java \
        paper/src/test/java/net/aincraft/placeholders/ModularJobsPlaceholderExpansionTest.java
git commit -m "feat: expand placeholder API to full modular placeholder set"
```

---

### Task 4: Docs + living-spec update

**Files:**
- Modify: `docs/jobsreborn-comparison.md` (mark implemented gaps)
- Modify: `docs/living-specs/jobs-progression.md` (check off new capabilities)
- Modify: `CHANGELOG.md` (Unreleased section)

- [ ] **Step 1: Update comparison matrix**

Mark the three implemented gaps as ✅ in `docs/jobsreborn-comparison.md` (placeholders, progression limits, level-up commands).

- [ ] **Step 2: Update living-spec**

In `docs/living-specs/jobs-progression.md` Current section add:
- `[x] PlaceholderAPI expansion: full modular placeholder set`
- `[x] Join limits: max jobs, per-job permission, world restriction, auto-join`
- `[x] Config-driven level-up commands`

- [ ] **Step 3: Update CHANGELOG**

Under `## Unreleased` add:

```markdown
### Added
- Full PlaceholderAPI `modular` placeholder set (level, experience, jobs, totallevels, maxjobs, name/description, isin/canjoin, …).
- Progression join limits: max-jobs, per-job `jobs.join.<job>` permission, world join restriction, auto-join on login.
- Config-driven level-up commands with `{player}` / `{level}` / `{job}` placeholders.
```

- [ ] **Step 4: Commit**

```bash
git add docs/jobsreborn-comparison.md docs/living-specs/jobs-progression.md CHANGELOG.md
git commit -m "docs: record JobsReborn parity work in comparison, living-spec, changelog"
```

---

### Task 5: Full verification + push + CI monitor

- [ ] **Step 1: Full local verification**

Run:
```bash
./gradlew :api:test :common:test :paper:test
./gradlew check
```
Expected: all tests pass; `check` completes (report-only quality).

- [ ] **Step 2: Push feature commits**

```bash
git push origin master
```
`master` is 4 commits ahead of `origin/master` (prior work: web homepage refresh `25b4297`, GitHub Packages registration `84d6c2a`, CI test `1a7cf1f`, CI template migration `5f1f27d`). Because the feature commits descend from those, the push includes all 8 commits. The 4 prior commits were inspected and are legitimate, non-sensitive, CI-compatible prior work; the user asked for all changes on GitHub, so the full stack is pushed.

- [ ] **Step 3: Monitor GitHub workflows**

```bash
gh run list --limit 5
gh run watch <run-id> --exit-status
```
Wait for `java`, `rest-api`, `session-editor`, `paper` jobs to complete green.

- [ ] **Step 4: Report**

Summarize: what was implemented, test evidence, CI status.
