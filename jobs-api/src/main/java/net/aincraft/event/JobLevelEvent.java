package net.aincraft.event;

import org.bukkit.entity.Player;

public class JobLevelEvent extends AbstractEvent {

  private final int level;
  private final Player player;

  public JobLevelEvent(int level, Player player) {
    this.level = level;
    this.player = player;
  }

  public Player getPlayer() {
    return player;
  }

  public int getLevel() {
    return level;
  }
}
