package net.aincraft.api;

import org.bukkit.OfflinePlayer;

public interface JobProgressionView {

  double getExperienceForLevel(int level);

  Job getJob();

  OfflinePlayer getPlayer();

  double getExperience();

  int getLevel() throws IllegalStateException;

}
