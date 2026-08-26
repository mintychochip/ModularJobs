package dev.mintychochip.payable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.preferences.api.Preference;
import dev.mintychochip.preferences.api.PreferenceKey;
import dev.mintychochip.preferences.api.PreferenceScope;
import dev.mintychochip.test.MockBukkitSupport;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.text.Component;
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

    @Override
    public Color getGlobal() {
      return null;
    }

    @Override
    public Color defaultValue() {
      return null;
    }

    @Override
    public PreferenceKey key() {
      throw new UnsupportedOperationException();
    }

    @Override
    public PreferenceScope scope() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Class<Color> type() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Component label() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Component description() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void set(Player player, Color value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setGlobal(Color value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setGlobal(Player player, Color value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void reset(Player player) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void resetGlobal() {
      throw new UnsupportedOperationException();
    }
  }
}
