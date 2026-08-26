# Per-Player XP Boss Bar Color Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let each player choose their own XP boss bar color through the external `aincraft-org/preferences` plugin's native dialog GUI, falling back to green when that plugin is absent.

**Architecture:** `compileOnly` dependency on `dev.mintychochip:preferences-api:0.2.0` (provided at runtime by the Preferences plugin, soft-depend). A `PreferencesIntegration` wiring class loads the Bukkit `PreferencesService` at enable and registers a player-scoped `Preference<BossBar.Color>` named `experience-bar-color` (enumerated codec → native option-picker dialog). `ExperienceBarColorProvider` takes the optional preference and returns the player's value, or `BossBar.Color.GREEN` when unavailable. `ExperienceBarFormatterImpl` already calls `colorProvider.getColor(player)` on every render.

**Tech Stack:** Java 25, Paper 26.2, Gradle Kotlin DSL, Adventure boss bar, JUnit 5 + MockBukkit.

## Global Constraints

- Dependency coordinate: `dev.mintychochip:preferences-api:0.2.0`, `compileOnly`, never shaded.
- `plugin.yml` gains `Preferences` under `softdepend` (do not add to `depend`).
- Preference name: `experience-bar-color`, player-scoped, enumerated over `BossBar.Color`.
- Fallback color when service/preference/player absent: `BossBar.Color.GREEN` — identical to today.
- Only import `dev.mintychochip.preferences.api.*` — never `common.internal.*`.
- Preference key components match `[a-z0-9_-]+` (no dots); namespace is the plugin name lowercased (`modularjobs`).
- CI: pinned checkout of `aincraft-org/preferences` at `c18236c1fa844eb0ae26824e524ae4605a9b41df`, publish `:preferences-api:publishToMavenLocal -PbuildVersion=0.2.0` with isolated `-Dmaven.repo.local`.
- Keep `Preferences` out of `depend`; local `PreferencesServiceImpl` stays the always-available fallback for `entries-per-page`/`gui-mode`.

---
### Task 1: Add the preferences-api dependency and plugin.yml soft-depend

**Files:**
- Modify: `paper/build.gradle.kts`
- Modify: `paper/src/main/resources/plugin.yml`
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- Produces: library catalog entry `libs.preferences.api` → `dev.mintychochip:preferences-api:0.2.0`; `paper` depends `compileOnly(libs.preferences.api)`.

- [ ] **Step 1: Add the library catalog entry**

In `gradle/libs.versions.toml`, under `[versions]` add:

```toml
# External Preferences plugin API (aincraft-org/preferences; soft-depend, provided at runtime)
preferences-api = "0.2.0"
```

Under `[libraries]` add:

```toml
preferences-api = { module = "dev.mintychochip:preferences-api", version.ref = "preferences-api" }
```

- [ ] **Step 2: Add the compileOnly dependency**

In `paper/build.gradle.kts`, in the `dependencies` block (near the other `compileOnly` soft-depend entries like `libs.mint.api`), add:

```kotlin
    // External Preferences API (soft-depend; Preferences plugin ships it at runtime)
    compileOnly(libs.preferences.api)
```

- [ ] **Step 3: Add Preferences to plugin.yml softdepend**

In `paper/src/main/resources/plugin.yml`, under `softdepend:` add `Preferences` (keep alphabetical order):

```yaml
softdepend:
  - PlaceholderAPI
  - Mint
  - mcMMO
  - Bolt
  - LWC
  - Choco
  - Preferences
```

- [ ] **Step 4: Verify the dependency resolves**

Ensure the sibling repo is published to local Maven:

```bash
cd /home/jlo/dev/preferences && ./gradlew :preferences-api:publishToMavenLocal -PbuildVersion=0.2.0 --console=plain --no-daemon
cd /home/jlo/dev/modularjobs && ./gradlew :paper:compileJava --console=plain --no-daemon
```

Expected: `BUILD SUCCESSFUL`; `dev.mintychochip:preferences-api:0.2.0` resolves from `mavenLocal()`.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml paper/build.gradle.kts paper/src/main/resources/plugin.yml
git commit -m "build: add preferences-api compileOnly dependency and soft-depend"
```

---
### Task 2: Wire ExperienceBarColorProvider to the preference

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/payable/ExperienceBarColorProvider.java`
- Modify: `paper/src/main/java/dev/mintychochip/payable/PayableWiring.java`
- Test: `paper/src/test/java/dev/mintychochip/payable/ExperienceBarColorProviderTest.java`

**Interfaces:**
- Consumes: `dev.mintychochip.preferences.api.Preference<BossBar.Color>` (from Task 3's `PreferencesIntegration`).
- Produces: `ExperienceBarColorProvider` constructor `ExperienceBarColorProvider(@Nullable Preference<BossBar.Color> preference)`; method `Color getColor(Player player)` returns the player's stored value or `BossBar.Color.GREEN`.

- [ ] **Step 1: Write the failing test**

Create `paper/src/test/java/dev/mintychochip/payable/ExperienceBarColorProviderTest.java`:

```java
package dev.mintychochip.payable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.preferences.api.Preference;
import dev.mintychochip.test.MockBukkitSupport;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class ExperienceBarColorProviderTest {

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void returnsPlayerStoredColorWhenPreferencePresent() {
    MockBukkitSupport.mockServer();
    Player player = MockBukkitSupport.mockServer().addPlayer("color-player");
    Preference<Color> pref = new StubPreference(Color.RED);
    ExperienceBarColorProvider provider = new ExperienceBarColorProvider(pref);
    assertEquals(Color.RED, provider.getColor(player));
  }

  @Test
  void returnsGreenWhenPreferenceAbsent() {
    MockBukkitSupport.mockServer();
    Player player = MockBukkitSupport.mockServer().addPlayer("color-player");
    ExperienceBarColorProvider provider = new ExperienceBarColorProvider(null);
    assertEquals(Color.GREEN, provider.getColor(player));
  }

  @Test
  void returnsGreenWhenPreferenceReturnsNull() {
    MockBukkitSupport.mockServer();
    Player player = MockBukkitSupport.mockServer().addPlayer("color-player");
    ExperienceBarColorProvider provider = new ExperienceBarColorProvider(new StubPreference(null));
    assertEquals(Color.GREEN, provider.getColor(player));
  }

  /** Minimal Preference<Color> stub — only get(Player) is exercised. */
  private static final class StubPreference implements Preference<Color> {
    private final Color value;

    StubPreference(Color value) {
      this.value = value;
    }

    @Override
    public Color get(Player player) {
      return value;
    }
  }
}
```

Note: `Preference` has more methods than `get(Player)`; the stub must implement the interface. If the API's `Preference<T>` is not a functional interface, add the remaining methods as `throw new UnsupportedOperationException()` stubs (the test only calls `get`). The exact interface surface is in Task 3's `PreferencesIntegration` — implement all abstract methods.

- [ ] **Step 2: Run test to verify it fails (does not compile)**

Run: `./gradlew :paper:test --tests dev.mintychochip.payable.ExperienceBarColorProviderTest --console=plain --no-daemon`
Expected: COMPILATION ERROR — `ExperienceBarColorProvider` has no constructor taking `Preference<Color>`.

- [ ] **Step 3: Implement the provider**

Replace the body of `paper/src/main/java/dev/mintychochip/payable/ExperienceBarColorProvider.java`:

```java
package dev.mintychochip.payable;

import dev.mintychochip.preferences.api.Preference;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the XP boss bar color from the optional per-player Preferences-backed preference.
 * Falls back to green when the preference or its service is unavailable.
 */
@NullMarked
final class ExperienceBarColorProvider {

  private final @Nullable Preference<Color> preference;

  ExperienceBarColorProvider(@Nullable Preference<Color> preference) {
    this.preference = preference;
  }

  Color getColor(Player player) {
    if (preference == null) {
      return Color.GREEN;
    }
    Color value = preference.get(player);
    return value != null ? value : Color.GREEN;
  }
}
```

- [ ] **Step 4: Wire the provider in PayableWiring**

In `paper/src/main/java/dev/mintychochip/payable/PayableWiring.java`, change the `create` signature to accept the optional preference and pass it to the provider:

```java
  public static PayableWiring create(
      Plugin plugin,
      JobService jobService,
      Registry<PayableType> payableTypeRegistry,
      CraftuxSurfaces surfaces,
      @Nullable dev.mintychochip.preferences.api.Preference<net.kyori.adventure.bossbar.BossBar.Color>
          experienceBarColorPreference) {
    ExperienceBarColorProvider colorProvider =
        new ExperienceBarColorProvider(experienceBarColorPreference);
```

Add the import for `org.jspecify.annotations.Nullable` if not present. The `create` caller is updated in Task 4.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :paper:test --tests dev.mintychochip.payable.ExperienceBarColorProviderTest --console=plain --no-daemon`
Expected: `BUILD SUCCESSFUL`, 3 tests pass.

- [ ] **Step 6: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/payable/ExperienceBarColorProvider.java paper/src/main/java/dev/mintychochip/payable/PayableWiring.java paper/src/test/java/dev/mintychochip/payable/ExperienceBarColorProviderTest.java
git commit -m "feat: back XP bar color provider with optional preference"
```

---
### Task 3: Add PreferencesIntegration wiring

**Files:**
- Create: `paper/src/main/java/dev/mintychochip/service/PreferencesIntegration.java`
- Test: `paper/src/test/java/dev/mintychochip/service/PreferencesIntegrationTest.java`

**Interfaces:**
- Consumes: `dev.mintychochip.preferences.api.PreferencesService` (Bukkit service), `Preference`, `PreferenceCodec`, `PreferenceKey`, `PreferenceScope`.
- Produces: `PreferencesIntegration.Wiring` record with fields `@Nullable Preference<BossBar.Color> experienceBarColor` and `@Nullable Runnable onDisable`; static `Wiring wire(JavaPlugin plugin)`.

- [ ] **Step 1: Write the failing test**

Create `paper/src/test/java/dev/mintychochip/service/PreferencesIntegrationTest.java`:

```java
package dev.mintychochip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.mintychochip.preferences.api.Preference;
import dev.mintychochip.preferences.api.PreferenceKey;
import dev.mintychochip.preferences.api.PreferenceScope;
import dev.mintychochip.preferences.api.PreferencesService;
import dev.mintychochip.test.MockBukkitSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class PreferencesIntegrationTest {

  private JavaPlugin plugin;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    plugin = MockBukkit.createMockPlugin("ModularJobs");
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void wireRegistersPlayerScopedColorPreferenceWhenServicePresent() {
    StubPreferencesService external = new StubPreferencesService();
    Bukkit.getServicesManager()
        .register(PreferencesService.class, external, plugin, ServicePriority.Normal);

    PreferencesIntegration.Wiring wiring = PreferencesIntegration.wire(plugin);

    assertNotNull(wiring.experienceBarColor(), "must register the color preference");
    assertEquals(1, external.registered.size(), "must register exactly one preference");
    PreferenceKey key = external.registered.get(0).key();
    assertEquals("modularjobs", key.namespace(), "namespace is plugin name lowercased");
    assertEquals("experience-bar-color", key.name());
    assertEquals(PreferenceScope.PLAYER, external.registered.get(0).scope());
    assertEquals(Color.class, external.registered.get(0).type());
    assertNotNull(wiring.onDisable(), "must expose unregister cleanup");
  }

  @Test
  void wireYieldsNullPreferenceWhenServiceAbsent() {
    PreferencesIntegration.Wiring wiring = PreferencesIntegration.wire(plugin);
    assertNull(wiring.experienceBarColor(), "no service -> no preference");
    assertNull(wiring.onDisable(), "no service -> no cleanup");
  }

  /** Minimal stub recording registrations; never actually persists. */
  private static final class StubPreferencesService implements PreferencesService {
    final List<Preference<?>> registered = new ArrayList<>();

    @Override
    public <T> Preference<T> register(
        org.bukkit.plugin.Plugin owner, Class<T> type, Consumer<PreferenceBuilder<T>> configure) {
      PreferenceBuilder<T> builder = new PreferenceBuilder<>("modularjobs", type);
      configure.accept(builder);
      builder.validate();
      Preference<T> handle = new StubPreference<>(builder);
      registered.add(handle);
      return handle;
    }

    @Override
    public Collection<? extends Preference<?>> all() {
      return registered;
    }

    @Override
    public void unregisterPlugin(org.bukkit.plugin.Plugin plugin) {
      registered.clear();
    }
  }

  /** Minimal Preference<T> handle backed by a builder. */
  private static final class StubPreference<T> implements Preference<T> {
    private final PreferenceBuilder<T> builder;

    StubPreference(PreferenceBuilder<T> builder) {
      this.builder = builder;
    }

    @Override
    public PreferenceKey key() {
      return builder.key();
    }

    @Override
    public PreferenceScope scope() {
      return builder.scope();
    }

    @Override
    public Class<T> type() {
      return builder.type();
    }

    @Override
    public net.kyori.adventure.text.Component label() {
      return builder.label();
    }

    @Override
    public net.kyori.adventure.text.Component description() {
      return builder.description();
    }

    @Override
    public T defaultValue() {
      return builder.defaultValue();
    }

    @Override
    public T get(org.bukkit.entity.Player player) {
      return builder.defaultValue();
    }

    @Override
    public T getGlobal() {
      return builder.defaultValue();
    }

    @Override
    public void set(org.bukkit.entity.Player player, T value) {}

    @Override
    public void setGlobal(T value) {}

    @Override
    public void setGlobal(org.bukkit.entity.Player editor, T value) {}

    @Override
    public void reset(org.bukkit.entity.Player player) {}

    @Override
    public void resetGlobal() {}
  }
}
```

Note: `PreferenceBuilder` is public with a public constructor `PreferenceBuilder(String namespace, Class<T> type)`; `key()`, `scope()`, `label()`, `description()`, `type()`, `defaultValue()` are public accessors. `Preference<T>` methods are per the API source (key, scope, type, label, description, defaultValue, get, getGlobal, set×2, reset, resetGlobal).

- [ ] **Step 2: Run test to verify it fails (does not compile)**

Run: `./gradlew :paper:test --tests dev.mintychochip.service.PreferencesIntegrationTest --console=plain --no-daemon`
Expected: COMPILATION ERROR — `PreferencesIntegration` not found.

- [ ] **Step 3: Implement PreferencesIntegration**

Create `paper/src/main/java/dev/mintychochip/service/PreferencesIntegration.java`:

```java
package dev.mintychochip.service;

import dev.mintychochip.preferences.api.Preference;
import dev.mintychochip.preferences.api.PreferenceCodec;
import dev.mintychochip.preferences.api.PreferencesService;
import java.util.Objects;
import java.util.logging.Level;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Soft integration with the external Preferences plugin.
 *
 * <p>When the Preferences Bukkit service is registered, ModularJobs registers its per-player
 * XP boss bar color preference there. When the plugin or service is absent, yields a null
 * preference so {@link dev.mintychochip.payable.ExperienceBarColorProvider} falls back to green.
 */
@NullMarked
public final class PreferencesIntegration {

  static final String EXPERIENCE_BAR_COLOR_NAME = "experience-bar-color";

  private PreferencesIntegration() {}

  /** Result of wiring: optional preference handle plus optional disable-path cleanup. */
  public record Wiring(
      @Nullable Preference<Color> experienceBarColor, @Nullable Runnable onDisable) {
    // Both fields are intentionally nullable: the absent-service paths construct Wiring(null, null).
  }

  /**
   * Loads the external Preferences service and registers ModularJobs' XP bar color preference.
   * Returns a {@link Wiring} with null fields when the service is unavailable.
   */
  public static Wiring wire(JavaPlugin plugin) {
    Objects.requireNonNull(plugin, "plugin");

    Plugin preferencesPlugin = Bukkit.getPluginManager().getPlugin("Preferences");
    if (preferencesPlugin == null || !preferencesPlugin.isEnabled()) {
      plugin.getSLF4JLogger().info(
          "Preferences plugin not present; XP boss bar color stays default green");
      return new Wiring(null, null);
    }

    PreferencesService external = Bukkit.getServicesManager().load(PreferencesService.class);
    if (external == null) {
      plugin.getSLF4JLogger().info(
          "Preferences plugin enabled but PreferencesService is not registered; "
              + "XP boss bar color stays default green");
      return new Wiring(null, null);
    }

    try {
      Preference<Color> color =
          external.register(
              plugin,
              Color.class,
              b ->
                  b.playerScoped(EXPERIENCE_BAR_COLOR_NAME)
                      .label(Component.text("XP bar color"))
                      .description(Component.text("Color of your job experience boss bar"))
                      .codec(PreferenceCodec.enumerated(Color.class, c -> Component.text(c.name())))
                      .defaultValue(Color.GREEN));
      plugin.getSLF4JLogger().info(
          "Registered ModularJobs XP boss bar color preference with Preferences plugin");
      return new Wiring(color, () -> external.unregisterPlugin(plugin));
    } catch (RuntimeException e) {
      plugin.getLogger().log(
          Level.WARNING, "Failed to register XP bar color preference; falling back to green", e);
      return new Wiring(null, null);
    }
  }
}
```

Note: `PreferenceCodec.enumerated(Class<E>, Function<E, Component>)` requires the enum type and a display mapper. `Color` is `net.kyori.adventure.bossbar.BossBar.Color`. The namespace is derived by the Preferences plugin from the owning plugin name (`modularjobs`).

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :paper:test --tests dev.mintychochip.service.PreferencesIntegrationTest --console=plain --no-daemon`
Expected: `BUILD SUCCESSFUL`, 2 tests pass.

- [ ] **Step 5: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/service/PreferencesIntegration.java paper/src/test/java/dev/mintychochip/service/PreferencesIntegrationTest.java
git commit -m "feat: register XP bar color preference via Preferences plugin"
```

---
### Task 4: Wire PreferencesIntegration into PluginContext

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/PluginContext.java`
- Modify: `paper/src/main/java/dev/mintychochip/payable/PayableWiring.java` (caller update)

**Interfaces:**
- Consumes: `PreferencesIntegration.wire(plugin)` → `Wiring(experienceBarColor, onDisable)`; `PayableWiring.create(..., @Nullable Preference<Color>)`.
- Produces: `PluginContext.createInto` wires the integration before `PayableWiring.create`, registers `onDisable` on `resources`, and passes the color preference into the payable wiring.

- [ ] **Step 1: Wire the integration into PluginContext**

In `paper/src/main/java/dev/mintychochip/PluginContext.java`, in `createInto`, before the `PayableWiring.create(...)` call (which currently sits after `CraftuxSurfaces.create()`), add:

```java
    // Soft-depend: register the XP bar color preference with the external Preferences plugin
    // when present; falls back to green when absent.
    final PreferencesIntegration.Wiring preferencesWiring = PreferencesIntegration.wire(plugin);
    if (preferencesWiring.onDisable() != null) {
      resources.onFlush(preferencesWiring.onDisable());
    }
```

Then change the `PayableWiring.create(...)` call to pass the color preference:

```java
    final PayableWiring payables =
        PayableWiring.create(
            plugin,
            domain.jobService,
            payableTypeRegistry,
            craftuxSurfaces,
            preferencesWiring.experienceBarColor());
```

Add the import `dev.mintychochip.service.PreferencesIntegration` (same package as `PreferencesServiceImpl`, already imported).

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :paper:compileJava --console=plain --no-daemon`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run the full paper test suite**

Run: `./gradlew :paper:test --console=plain --no-daemon`
Expected: `BUILD SUCCESSFUL`, all tests pass (including `ExperienceBarColorProviderTest`, `PreferencesIntegrationTest`, `BootstrapLifecycleTest`).

- [ ] **Step 4: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/PluginContext.java paper/src/main/java/dev/mintychochip/payable/PayableWiring.java
git commit -m "feat: wire XP bar color preference into plugin context"
```

---
### Task 5: Update CI for the pinned preferences checkout

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/nightly.yml`

**Interfaces:**
- Consumes: pinned SHA `c18236c1fa844eb0ae26824e524ae4605a9b41df`; publish command `:preferences-api:publishToMavenLocal -PbuildVersion=0.2.0`.

- [ ] **Step 1: Update the ci.yml Preferences checkout + publish step**

In `.github/workflows/ci.yml`, replace the existing "Checkout Preferences API dependency" + "Publish Preferences API to isolated Maven local" steps:

```yaml
      - name: Checkout Preferences API dependency
        uses: actions/checkout@v7
        with:
          repository: aincraft-org/preferences
          ref: c18236c1fa844eb0ae26824e524ae4605a9b41df
          path: preferences
          fetch-depth: 1

      - name: Publish Preferences API to isolated Maven local
        working-directory: preferences
        run: |
          chmod +x gradlew
          ./gradlew :preferences-api:publishToMavenLocal \
            -PbuildVersion=0.2.0 \
            -Dmaven.repo.local=${{ runner.temp }}/m2 \
            --console=plain --no-daemon
```

- [ ] **Step 2: Update the nightly.yml Preferences checkout + publish step**

Apply the same change to `.github/workflows/nightly.yml` (same repo/ref/publish command).

- [ ] **Step 3: Verify no stale references remain**

Search for `mintychochip/Preferences`, `:api:publishToMavenLocal` (in the Preferences context), and `dev.jlo` across `.github/`:

Run: `grep -rn "mintychochip/Preferences\|:api:publishToMavenLocal\|dev.jlo" .github/`
Expected: no matches (the `:api:` module belongs to the old repo; the new repo's module is `:preferences-api`).

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/ci.yml .github/workflows/nightly.yml
git commit -m "ci: pin aincraft-org/preferences checkout and publish preferences-api 0.2.0"
```

---
### Task 6: Update living spec and README

**Files:**
- Modify: `docs/living-specs/payables-economy.md`
- Modify: `docs/living-specs/modularjobs.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: the shipped feature state (XP bar color preference, optional Preferences soft-depend).

- [ ] **Step 1: Update payables-economy living spec**

In `docs/living-specs/payables-economy.md`, update the "Current notes" to reflect that the XP boss bar color is now a per-player preference backed by the external Preferences plugin when present:

```markdown
The XP boss bar color is a per-player preference registered with the external
Preferences plugin when present; without it the bar stays default green.
```

Also add a "Current" checkbox:

```markdown
- [x] Per-player XP boss bar color preference (external Preferences plugin, green fallback)
```

- [ ] **Step 2: Update modularjobs living spec**

In `docs/living-specs/modularjobs.md`, update the "Current notes" paragraph (which currently says "Preferences is local-only") to note the optional external wiring for the XP bar color:

```markdown
The general Paper distribution hardening is landed: Mint is an optional
reflective economy adapter with a blackhole default, the editor is opt-in, and
Craftux remains the explicitly deferred UI dependency. The external Preferences
plugin is an optional soft-depend that powers the per-player XP boss bar color;
without it the bar stays green and the local preferences service handles
entries-per-page/gui-mode.
```

- [ ] **Step 3: Update README**

In `README.md`, find the section that says "The external Preferences plugin is not required." (near line 167). Update it to note the optional XP bar color integration:

```markdown
The external Preferences plugin is not required. When present, it provides a
per-player XP boss bar color preference via /preferences; without it the bar
stays default green.
```

- [ ] **Step 4: Commit**

```bash
git add docs/living-specs/payables-economy.md docs/living-specs/modularjobs.md README.md
git commit -m "docs: document optional XP bar color preference integration"
```

---
### Task 7: Full verification

**Files:**
- None (verification only).

- [ ] **Step 1: Run the full check**

Run: `./gradlew clean check --console=plain --no-daemon`
Expected: `BUILD SUCCESSFUL` — compile, Error Prone, unit tests, static analysis all pass.

- [ ] **Step 2: Build the shadow jar**

Run: `./gradlew :paper:shadowJar --console=plain --no-daemon`
Expected: `BUILD SUCCESSFUL`; `paper/build/libs/paper-all.jar` produced.

- [ ] **Step 3: Verify the jar contains no preferences-api classes**

Run: `unzip -l paper/build/libs/paper-all.jar | grep -i "preferences" | head`
Expected: no `dev/mintychochip/preferences/` classes (the API is `compileOnly`, not shaded).

- [ ] **Step 4: Verify plugin.yml softdepend**

Run: `unzip -p paper/build/libs/paper-all.jar plugin.yml | grep -A8 softdepend`
Expected: `Preferences` listed under `softdepend`.

- [ ] **Step 5: Confirm the full suite is green**

Run: `./gradlew :api:test :common:test :paper:test --console=plain --no-daemon`
Expected: `BUILD SUCCESSFUL`, all tests pass.

---

## Self-Review

**Spec coverage:**
- Dependency `compileOnly` + `plugin.yml` softdepend → Task 1.
- Pinned CI checkout + `-PbuildVersion=0.2.0` publish → Task 5.
- Registration (player-scoped `experience-bar-color`, enumerated codec) → Task 3.
- Provider seam + green fallback → Task 2.
- PluginContext wiring + disable cleanup → Task 4.
- Tests (provider + wiring) → Tasks 2 & 3.
- Living spec + README → Task 6.
- Full verification → Task 7.

**Placeholder scan:** No TBD/TODO; every code step shows complete code. The `Preference<T>` stub in Task 3 and `StubPreference` in Task 2 note that all interface methods must be implemented (the API surface is fully enumerated from source).

**Type consistency:** `Preference<BossBar.Color>` / `Preference<Color>` used consistently across Tasks 2–4. `PreferencesIntegration.Wiring(experienceBarColor, onDisable)` matches Task 4's consumption. `PayableWiring.create` signature change (added `@Nullable Preference<Color>` param) is reflected in both Task 2 (definition) and Task 4 (caller).
