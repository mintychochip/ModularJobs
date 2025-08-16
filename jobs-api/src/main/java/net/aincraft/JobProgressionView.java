package net.aincraft;

import java.math.BigDecimal;
import org.bukkit.OfflinePlayer;

public interface JobProgressionView {

  double getExperienceForLevel(int level);

  Job getJob();

  OfflinePlayer getPlayer();

  BigDecimal getExperience();

  int getLevel() throws IllegalStateException;

}
