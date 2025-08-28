package net.aincraft.commands.top;

import java.math.BigDecimal;
import net.aincraft.JobProgression;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.NotNull;

final class LevelComponent implements ComponentLike {

  private final int level;
  private final BigDecimal experience;

  LevelComponent(int level, BigDecimal experience) {
    this.level = level;
    this.experience = experience;
  }

  static LevelComponent of(JobProgression progression) {
    return new LevelComponent(progression.level(), progression.experience());
  }

  @Override
  public @NotNull Component asComponent() {
    return Component.text(level)
        .hoverEvent(
            HoverEvent.showText(Component.text("Experience: " + experience.toPlainString())));
  }
}
