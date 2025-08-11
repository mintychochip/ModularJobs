package net.aincraft.api.container;

import net.aincraft.api.Bridge;
import net.aincraft.api.JobProgressionView;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface ExperienceBarFormatter {

  static ExperienceBarFormatter experienceBarFormatter() {
    return Bridge.bridge().experienceBarFormatter();
  }

  BossBar format(@NotNull BossBar bossBar, @NotNull FormattingContext context);

  void setColor(@NotNull Color color);

  void setOverlay(@NotNull Overlay overlay);

  void setNameFormatter(@NotNull NameFormatter formatter);

  @FunctionalInterface
  interface NameFormatter {

    @NotNull
    Component format(@NotNull FormattingContext context);
  }

  record FormattingContext(JobProgressionView progression, Payable payable, Player player) {

  }
}
