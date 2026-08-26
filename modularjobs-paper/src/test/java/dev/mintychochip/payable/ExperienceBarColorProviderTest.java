package dev.mintychochip.payable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.test.MockBukkitSupport;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ExperienceBarColorProviderTest {

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void returnsPlayerStoredColorWhenPreferencePresent() {
    MockBukkitSupport.mockServer();
    Player player = MockBukkitSupport.mockServer().addPlayer("color-player");
    ExperienceBarColorPreference pref = p -> Color.RED;
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
    ExperienceBarColorProvider provider = new ExperienceBarColorProvider(p -> null);
    assertEquals(Color.GREEN, provider.getColor(player));
  }
}
