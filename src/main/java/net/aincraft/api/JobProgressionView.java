package net.aincraft.api;

import org.bukkit.OfflinePlayer;

public interface JobProgressionView {

  Job getJob();

  OfflinePlayer getPlayer();

  long getExperience();
}
