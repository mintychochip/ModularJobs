package net.aincraft.commands.top;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.aincraft.JobProgression;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

final class LevelComponent implements ComponentLike {

  private final JobProgression progression;

  LevelComponent(JobProgression progression) {
    this.progression = progression;
  }

  static LevelComponent of(JobProgression progression) {
    return new LevelComponent(progression);
  }

  @Override
  public @NotNull Component asComponent() {
    int level = progression.level();
    BigDecimal experience = progression.experience();
    int maxLevel = progression.job().maxLevel();

    Component hover;
    if (level >= maxLevel) {
      // Player is at max level
      hover = Component.text()
          .append(Component.text("Level: ", NamedTextColor.GRAY))
          .append(Component.text(level + " (MAX)", NamedTextColor.GOLD))
          .appendNewline()
          .append(Component.text("Experience: ", NamedTextColor.GRAY))
          .append(Component.text(experience.toPlainString(), NamedTextColor.WHITE))
          .build();
    } else {
      // Calculate progress to next level
      BigDecimal currentLevelXP = progression.experienceForLevel(level);
      BigDecimal nextLevelXP = progression.experienceForLevel(level + 1);
      BigDecimal progressXP = experience.subtract(currentLevelXP);
      BigDecimal neededXP = nextLevelXP.subtract(currentLevelXP);

      // Calculate percentage
      double percentage = progressXP.divide(neededXP, 4, RoundingMode.HALF_UP)
          .multiply(BigDecimal.valueOf(100)).doubleValue();

      hover = Component.text()
          .append(Component.text("Level: ", NamedTextColor.GRAY))
          .append(Component.text(level, NamedTextColor.YELLOW))
          .appendNewline()
          .append(Component.text("Progress: ", NamedTextColor.GRAY))
          .append(Component.text(String.format("%.1f%%", percentage), NamedTextColor.GREEN))
          .appendNewline()
          .append(Component.text("XP: ", NamedTextColor.GRAY))
          .append(Component.text(progressXP.toPlainString(), NamedTextColor.WHITE))
          .append(Component.text(" / ", NamedTextColor.GRAY))
          .append(Component.text(neededXP.toPlainString(), NamedTextColor.WHITE))
          .build();
    }

    return Component.text(level).hoverEvent(HoverEvent.showText(hover));
  }
}
