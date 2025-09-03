package net.aincraft.commands;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

final class TextScoreboardImpl implements TextScoreboard {

  private static final int MAX_LINES = 15;

  private final Scoreboard scoreboard;
  private final Objective objective;
  private final Team[] teams = new Team[MAX_LINES];
  private final String[] entries = new String[MAX_LINES];

  private TextScoreboardImpl(Scoreboard scoreboard, Objective objective) {
    this.scoreboard = scoreboard;
    this.objective = objective;
  }

  public static TextScoreboard create(Component displayName) {
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    if (manager == null) {
      throw new IllegalStateException("ScoreboardManager is not available yet.");
    }
    Scoreboard scoreboard = manager.getNewScoreboard();
    Objective objective = scoreboard.registerNewObjective("internal", Criteria.DUMMY, displayName);
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    objective.numberFormat(NumberFormat.blank());
    return new TextScoreboardImpl(scoreboard, objective);
  }


  @Override
  public void setLine(int index, ComponentLike prefix, ComponentLike suffix) {
    String entry = entries[index];
    if (entry == null) {
      entry = ChatColor.values()[index].toString();
      entries[index] = entry;
    }
    Team team = teams[index];
    if (team == null) {
      team = scoreboard.registerNewTeam(entry);
      team.addEntry(entry);
      teams[index] = team;
      objective.getScore(entry).setScore(MAX_LINES - index);
    }
    team.prefix(prefix.asComponent());
    team.suffix(suffix.asComponent());
  }

  @Override
  public void show(Player player, Duration duration) {
    player.setScoreboard(scoreboard);
  }

  @Override
  public void setCurrent(Player player) {
    if (player == null) {
      return;
    }
    player.setScoreboard(this.scoreboard);
  }
}
