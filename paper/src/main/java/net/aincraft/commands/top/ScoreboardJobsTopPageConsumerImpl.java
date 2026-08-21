package net.aincraft.commands.top;

import java.util.List;
import net.aincraft.JobProgression;
import net.aincraft.commands.Page;
import net.aincraft.commands.TextScoreboard;
import net.aincraft.commands.components.PlayerComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Renders leaderboard entries onto a player's side scoreboard. Only renders
 * when the sender is a player; other senders (console, command blocks) are
 * silently ignored.
 */
public final class ScoreboardJobsTopPageConsumerImpl implements JobsTopPageConsumer {

  private static final String ENTRY_FORMAT = "<rank>. <player>: <level>";

  private final TextScoreboard scoreBoard;

  /**
   * Creates a scoreboard consumer that writes rows to the given surface.
   *
   * @param scoreBoard scoreboard surface the rows are written to
   */
  public ScoreboardJobsTopPageConsumerImpl(TextScoreboard scoreBoard) {
    this.scoreBoard = scoreBoard;
  }

  @Override
  public void consume(Component jobName, Page<JobProgression> page, CommandSender sender, int maxPages,
      List<JobProgression> allEntries) {
    if (!(sender instanceof Player player)) {
      return;
    }
    List<JobProgression> data = page.data();
    int pageNumber = page.pageNumber();
    int pageSize = page.size();
    for (int i = 0; i < data.size(); i++) {
      JobProgression progression = data.get(i);
      OfflinePlayer progressionPlayer = Bukkit.getOfflinePlayer(progression.playerId());
      Component row = MiniMessage.miniMessage().deserialize(ENTRY_FORMAT, TagResolver.builder()
          .tag("rank", Tag.inserting(Component.text(i + 1 + (pageNumber - 1) * pageSize)))
          .tag("player", Tag.inserting(PlayerComponent.of(progressionPlayer)))
          .tag("level", Tag.inserting(LevelComponent.of(progression)))
          .build());
      scoreBoard.setLine(i, row, Component.empty());
    }
    scoreBoard.setCurrent(player);
  }
}
