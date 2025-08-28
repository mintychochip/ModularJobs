package net.aincraft.commands.top;

import java.util.List;
import net.aincraft.JobProgression;
import net.aincraft.commands.Page;
import net.aincraft.commands.PageConsumer;
import net.aincraft.commands.TextScoreboard;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public final class ScoreboardJobsTopConsumerImpl implements PageConsumer<JobProgression> {

  private final TextScoreboard scoreboard;

  public ScoreboardJobsTopConsumerImpl(TextScoreboard scoreboard) {
    this.scoreboard = scoreboard;
  }

  @Override
  public void consume(Page<JobProgression> page, CommandSender sender) {
    if (!(sender instanceof Player player)) {
      return;
    }
    List<JobProgression> data = page.data();
    int size = data.size();
    for (int i = 0; i < size; i++) {
      JobProgression progression = data.get(i);
      scoreboard.setLine();
    }
    scoreboard.setLine();
  }
}
