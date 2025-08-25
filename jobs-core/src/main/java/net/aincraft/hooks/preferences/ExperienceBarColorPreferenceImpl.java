package net.aincraft.hooks.preferences;

import net.aincraft.Preference;
import net.aincraft.PreferenceType;
import net.aincraft.types.PreferenceTypes;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.jetbrains.annotations.NotNull;

public final class ExperienceBarColorPreferenceImpl implements Preference<Color> {

  @Override
  public PreferenceType<Color> getType() {
    return PreferenceTypes.BOSS_BAR_COLOR;
  }

  @Override
  public String getName() {
    return "experience-bar-color";
  }

  @Override
  public @NotNull Color getDefault() {
    return Color.BLUE;
  }
}
