package net.aincraft.container;

import java.math.BigDecimal;
import net.aincraft.JobProgressionView;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface ExperiencePayableHandler extends PayableHandler {

  void setExperienceBarController(ExperienceBarController controller);

  void setExperienceBarFormatter(ExperienceBarFormatter formatter);

  interface ExperienceBarFormatter {

    BossBar format(@NotNull BossBar bossBar,
        @NotNull ExperienceBarContext context);

    void setColor(@NotNull BossBar.Color color);

    void setOverlay(@NotNull BossBar.Overlay overlay);
  }

  interface ExperienceBarController {

    void display(ExperienceBarContext context, ExperienceBarFormatter formatter);
  }

  record ExperienceBarContext(JobProgressionView progression, Player player, BigDecimal amount) {}
}
