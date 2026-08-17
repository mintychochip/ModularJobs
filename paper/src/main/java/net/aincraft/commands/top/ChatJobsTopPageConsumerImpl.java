package net.aincraft.commands.top;

import net.aincraft.util.Messages;
import java.util.List;
import net.aincraft.JobProgression;
import net.aincraft.commands.Page;
import net.aincraft.commands.components.PlayerComponent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Renders leaderboard entries as chat messages, including the viewer's rank,
 * highlighted rows for the viewer, and clickable previous/next navigation.
 */
public final class ChatJobsTopPageConsumerImpl implements JobsTopPageConsumer {

  private static final String ENTRY_FORMAT = "<rank>. <player>: <level>";

  @Override
  public void consume(Component jobName, Page<JobProgression> page, CommandSender sender,
      int maxPages, List<JobProgression> allEntries) {
    String jobNameText = PlainTextComponentSerializer.plainText().serialize(jobName);
    int pageNumber = page.pageNumber();

    // Header with page info
    Messages.send(sender, "");
    Messages.send(sender, "<neutral>━━━━━━ <primary>Jobs Top </primary><accent>"
        + jobNameText + "</accent> <neutral>━━━━━━");
    Messages.send(sender, "<neutral>Page " + pageNumber + " of " + maxPages);

    // Show viewer's rank if they're a player and in the leaderboard
    if (sender instanceof Player player) {
      int rank = findPlayerRank(player, allEntries);
      if (rank > 0) {
        Messages.send(sender, "<accent>Your Rank: <primary>#" + rank);
      }
    }

    // Show total players
    Messages.send(sender, "<neutral>Showing top <accent>" + allEntries.size()
        + "<neutral> players");
    Messages.send(sender, "");

    // Leaderboard entries
    Component body = Component.empty();
    List<JobProgression> data = page.data();
    int pageSize = page.size();
    for (int i = 0; i < data.size(); i++) {
      JobProgression progression = data.get(i);
      OfflinePlayer progressionPlayer = Bukkit.getOfflinePlayer(progression.playerId());
      boolean isViewer = sender instanceof Player player
          && progression.playerId().equals(player.getUniqueId());

      Component row = MiniMessage.miniMessage().deserialize(ENTRY_FORMAT, TagResolver.builder()
          .tag("rank", Tag.inserting(Component.text((i + 1) + (pageNumber - 1) * pageSize)))
          .tag("player", Tag.inserting(PlayerComponent.of(progressionPlayer)))
          .tag("level", Tag.inserting(LevelComponent.of(progression)))
          .build());

      // Highlight the viewer's entry
      if (isViewer) {
        row = Component.text("→ ", NamedTextColor.GOLD).append(row.color(NamedTextColor.YELLOW));
      }

      body = body.append(row).appendNewline();
    }
    sender.sendMessage(body);

    // Navigation footer
    Messages.send(sender, "");
    Component navigation = buildNavigation(jobNameText, pageNumber, maxPages);
    sender.sendMessage(navigation);
    Messages.send(sender, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Messages.send(sender, "");
  }

  /**
   * Builds the previous/next navigation footer with clickable {@code /jobs top}
   * commands, disabling the button at the movement boundary.
   */
  private Component buildNavigation(String jobName, int currentPage, int maxPages) {
    Component nav = Component.empty();

    // Previous button
    if (currentPage > 1) {
      Component prevButton = Component.text("[< Previous]")
          .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)
          .clickEvent(ClickEvent.runCommand("/jobs top " + jobName + " " + (currentPage - 1)))
          .hoverEvent(Component.text("Click to go to page " + (currentPage - 1)));
      nav = nav.append(prevButton);
    } else {
      Component prevButton = Component.text("[< Previous]")
          .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY);
      nav = nav.append(prevButton);
    }

    nav = nav.append(Component.text("  ").color(net.kyori.adventure.text.format.NamedTextColor.GRAY));

    // Next button
    if (currentPage < maxPages) {
      Component nextButton = Component.text("[Next >]")
          .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)
          .clickEvent(ClickEvent.runCommand("/jobs top " + jobName + " " + (currentPage + 1)))
          .hoverEvent(Component.text("Click to go to page " + (currentPage + 1)));
      nav = nav.append(nextButton);
    } else {
      Component nextButton = Component.text("[Next >]")
          .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY);
      nav = nav.append(nextButton);
    }

    return nav;
  }

  /**
   * Finds the 1-indexed rank of a player within the leaderboard.
   *
   * @return the player's rank, or {@code -1} if not present
   */
  private int findPlayerRank(Player player, List<JobProgression> allEntries) {
    for (int i = 0; i < allEntries.size(); i++) {
      JobProgression progression = allEntries.get(i);
      if (progression.playerId().equals(player.getUniqueId())) {
        return i + 1; // Rank is 1-indexed
      }
    }
    return -1; // Player not found in leaderboard
  }
}
