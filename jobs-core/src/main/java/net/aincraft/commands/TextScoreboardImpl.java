package net.aincraft.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

final class TextScoreboardImpl implements TextScoreboard {

  private final Scoreboard scoreboard;
  private final Team[] teams = new Team[15];

  private TextScoreboardImpl(Scoreboard scoreboard) {
    this.scoreboard = scoreboard;
  }

  public static TextScoreboard create(Component displayName) {
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    Objective objective = scoreboard.registerNewObjective("internal", Criteria.DUMMY, displayName);
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    return new TextScoreboardImpl(scoreboard);
  }

  @Override
  public void setLine(int index, Component line) {
    Team team = teams[index];
    if (team == null) {
    }
  }

  @Override
  public void setCurrent(Player player) {

  }
}
