package net.aincraft.hooks.preferences;

import java.util.concurrent.ExecutionException;
import net.aincraft.Preference;
import net.aincraft.PreferenceService;
import net.aincraft.payable.ExperienceBarColorProvider;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.entity.Player;

public class PreferenceExperienceBarColorProviderImpl implements ExperienceBarColorProvider {

  private final PreferenceService preferenceService;
  private final Preference.Key<BossBar.Color> preferenceKey;

  public PreferenceExperienceBarColorProviderImpl(PreferenceService preferenceService,
      Preference.Key<Color> preferenceKey) {
    this.preferenceService = preferenceService;
    this.preferenceKey = preferenceKey;
  }

  @Override
  public Color getColor(Player player) {
    try {
      return preferenceService.getPreference(player.getUniqueId(), preferenceKey);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
