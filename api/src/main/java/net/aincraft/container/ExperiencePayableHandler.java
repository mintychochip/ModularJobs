package net.aincraft.container;

import java.math.BigDecimal;
import net.aincraft.JobProgressionView;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface ExperiencePayableHandler extends PayableHandler {

  interface ExperienceBarFormatter {

    BossBar format(@NotNull BossBar bossBar,
        @NotNull ExperienceBarContext context);

    void setOverlay(@NotNull BossBar.Overlay overlay);
  }

  interface ExperienceBarController {

    void display(ExperienceBarContext context, ExperienceBarFormatter formatter);
  }

  record ExperienceBarContext(JobProgressionView progression, Player player, BigDecimal amount) {}
}
