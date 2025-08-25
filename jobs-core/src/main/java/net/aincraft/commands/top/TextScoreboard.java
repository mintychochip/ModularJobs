package net.aincraft.commands.top;

import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TextScoreboard {

  private final Scoreboard scoreboard;
  private final Team[] teams = new Team[15];

  public TextScoreboard(Scoreboard scoreboard) {
    this.scoreboard = scoreboard;
    Objective objective = scoreboard.getObjective("textboard");
    if (objective == null) {
      objective = scoreboard.registerNewObjective("textboard", Criteria.DUMMY, Component.text(" "));
      objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }
  }

  public void setLine(int index) {

  }
}
