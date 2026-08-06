package net.aincraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jlo.preferences.api.Preference;
import dev.jlo.preferences.api.PreferenceBuilder;
import dev.jlo.preferences.api.PreferenceKey;
import dev.jlo.preferences.api.PreferenceScope;
import dev.jlo.preferences.api.PreferencesService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import net.aincraft.test.MockBukkitSupport;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Drives the shipped {@link ExternalBackedPreferencesService} get/set path against real
 * {@link Preference} handles (and a stub external {@link PreferencesService#register} path).
 */
class ExternalBackedPreferencesServiceTest {

  private JavaPlugin plugin;
  private PlayerMock player;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    plugin = MockBukkit.createMockPlugin("ModularJobs");
    plugin.getConfig().set("preferences.entries-per-page", 10);
    plugin.getConfig().set("preferences.default-gui-mode", true);
    player = MockBukkitSupport.mockServer().addPlayer("pref-player");
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void setThenGetEntriesPerPageAndGuiModeViaRegisteredHandles() {
    StubExternalPreferencesService external = new StubExternalPreferencesService();
    ExternalBackedPreferencesService facade =
        ExternalBackedPreferencesService.register(plugin, external);

    assertEquals(2, external.registered.size(), "must register entries-per-page and gui-mode");
    assertEquals(
        "modularjobs",
        external.registered.get(0).key().namespace(),
        "namespace is plugin name lowercased");
    assertEquals(ExternalBackedPreferencesService.ENTRIES_PER_PAGE_NAME,
        external.registered.get(0).key().name());
    assertEquals(ExternalBackedPreferencesService.GUI_MODE_NAME,
        external.registered.get(1).key().name());

    // Defaults from config via Preference.defaultValue
    assertEquals(10, facade.getEntriesPerPage(player));
    assertTrue(facade.prefersGuiMode(player));
    assertEquals(10, facade.getDefaultEntriesPerPage());

    // Real facade set → Preference handle storage → get
    facade.setEntriesPerPage(player, 25);
    facade.setGuiMode(player, false);

    assertEquals(25, facade.getEntriesPerPage(player));
    assertFalse(facade.prefersGuiMode(player));

    // Values live on the registered handles (Preferences-backed store), not a local map
    assertEquals(25, facade.entriesPerPagePreference().get(player));
    assertEquals(Boolean.FALSE, facade.guiModePreference().get(player));
  }

  @Test
  void setClampsEntriesPerPageToSliderBounds() {
    StubExternalPreferencesService external = new StubExternalPreferencesService();
    ExternalBackedPreferencesService facade =
        ExternalBackedPreferencesService.register(plugin, external);

    facade.setEntriesPerPage(player, 0);
    assertEquals(1, facade.getEntriesPerPage(player));

    facade.setEntriesPerPage(player, 999);
    assertEquals(50, facade.getEntriesPerPage(player));
  }

  @Test
  void registerUsesConfigDefaultsForHandles() {
    plugin.getConfig().set("preferences.entries-per-page", 7);
    plugin.getConfig().set("preferences.default-gui-mode", false);

    StubExternalPreferencesService external = new StubExternalPreferencesService();
    ExternalBackedPreferencesService facade =
        ExternalBackedPreferencesService.register(plugin, external);

    assertEquals(7, facade.getDefaultEntriesPerPage());
    assertEquals(7, facade.getEntriesPerPage(player));
    assertFalse(facade.prefersGuiMode(player));
    assertEquals(7, facade.entriesPerPagePreference().defaultValue());
    assertEquals(Boolean.FALSE, facade.guiModePreference().defaultValue());
  }

  @Test
  void handleBackedConstructorRoundTripWithoutExternalService() {
    MemoryPreference<Integer> entries =
        new MemoryPreference<>(
            new PreferenceKey("modularjobs", "entries-per-page"),
            PreferenceScope.PLAYER,
            Integer.class,
            Component.text("entries"),
            Component.empty(),
            10);
    MemoryPreference<Boolean> gui =
        new MemoryPreference<>(
            new PreferenceKey("modularjobs", "gui-mode"),
            PreferenceScope.PLAYER,
            Boolean.class,
            Component.text("gui"),
            Component.empty(),
            true);

    ExternalBackedPreferencesService facade =
        new ExternalBackedPreferencesService(plugin, null, entries, gui, 10);

    facade.setEntriesPerPage(player, 15);
    facade.setGuiMode(player, false);

    assertEquals(15, facade.getEntriesPerPage(player));
    assertFalse(facade.prefersGuiMode(player));
    assertEquals(15, entries.get(player));
    assertEquals(Boolean.FALSE, gui.get(player));
  }

  /**
   * Stub of the public Preferences API surface: implements {@link PreferencesService#register}
   * by building a real {@link PreferenceBuilder} (same path the Preferences plugin uses) and
   * returning an in-memory {@link Preference} handle.
   */
  static final class StubExternalPreferencesService implements PreferencesService {
    final List<Preference<?>> registered = new ArrayList<>();

    @Override
    public <T> Preference<T> register(
        Plugin owner, Class<T> type, Consumer<PreferenceBuilder<T>> configure) {
      PreferenceBuilder<T> builder =
          new PreferenceBuilder<>(owner.getName().toLowerCase(Locale.ROOT), type);
      configure.accept(builder);
      builder.validate();
      MemoryPreference<T> pref =
          new MemoryPreference<>(
              builder.key(),
              builder.scope(),
              builder.type(),
              builder.label(),
              builder.description(),
              builder.defaultValue());
      registered.add(pref);
      return pref;
    }

    @Override
    public Collection<? extends Preference<?>> all() {
      return List.copyOf(registered);
    }

    @Override
    public void unregisterPlugin(Plugin plugin) {
      registered.clear();
    }
  }

  /** Minimal in-memory {@link Preference} implementing the public API get/set contract. */
  static final class MemoryPreference<T> implements Preference<T> {
    private final PreferenceKey key;
    private final PreferenceScope scope;
    private final Class<T> type;
    private final Component label;
    private final Component description;
    private final T defaultValue;
    private final Map<UUID, T> values = new HashMap<>();

    MemoryPreference(
        PreferenceKey key,
        PreferenceScope scope,
        Class<T> type,
        Component label,
        Component description,
        T defaultValue) {
      this.key = Objects.requireNonNull(key);
      this.scope = Objects.requireNonNull(scope);
      this.type = Objects.requireNonNull(type);
      this.label = Objects.requireNonNull(label);
      this.description = Objects.requireNonNull(description);
      this.defaultValue = Objects.requireNonNull(defaultValue);
    }

    @Override
    public PreferenceKey key() {
      return key;
    }

    @Override
    public PreferenceScope scope() {
      return scope;
    }

    @Override
    public Class<T> type() {
      return type;
    }

    @Override
    public Component label() {
      return label;
    }

    @Override
    public Component description() {
      return description;
    }

    @Override
    public T defaultValue() {
      return defaultValue;
    }

    @Override
    public T get(Player player) {
      return values.getOrDefault(player.getUniqueId(), defaultValue);
    }

    @Override
    public T getGlobal() {
      return defaultValue;
    }

    @Override
    public void set(Player player, T value) {
      values.put(player.getUniqueId(), value);
    }

    @Override
    public void setGlobal(T value) {}

    @Override
    public void setGlobal(Player editor, T value) {}

    @Override
    public void reset(Player player) {
      values.remove(player.getUniqueId());
    }

    @Override
    public void resetGlobal() {}
  }
}
