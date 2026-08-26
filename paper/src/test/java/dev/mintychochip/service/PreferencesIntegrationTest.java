package dev.mintychochip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.mintychochip.preferences.api.Preference;
import dev.mintychochip.preferences.api.PreferenceBuilder;
import dev.mintychochip.preferences.api.PreferenceKey;
import dev.mintychochip.preferences.api.PreferenceScope;
import dev.mintychochip.preferences.api.PreferencesService;
import dev.mintychochip.test.MockBukkitSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
