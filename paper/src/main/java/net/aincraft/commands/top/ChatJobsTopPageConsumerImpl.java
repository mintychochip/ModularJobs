package net.aincraft.commands.top;

import net.aincraft.util.Messages;
import java.util.List;
import net.aincraft.JobProgression;
import net.aincraft.commands.Page;
import net.aincraft.commands.components.PlayerComponent;
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

    Messages.send(sender, "");
    Messages.send(sender, "<neutral>━━━━━━ <primary>Jobs Top </primary><accent>"
        + jobNameText + "</accent> <neutral>━━━━━━");
    Messages.send(sender, "<neutral>Page " + pageNumber + " of " + maxPages);

    if (sender instanceof Player player) {
      int playerRank = findPlayerRank(player, allEntries);
      if (playerRank > 0) {
        Messages.send(sender, "<neutral>Your rank: <accent>#" + playerRank);
      }
    }

    Messages.send(sender, "<neutral>Showing top <accent>" + allEntries.size()
        + "<neutral> players");
    Messages.send(sender, "");

    Component body = Component.empty();
    List<JobProgression> data = page.data();
    int pageSize = page.size();
    for (int i = 0; i < data.size(); i++) {
      JobProgression progression = data.get(i);
      OfflinePlayer progressionPlayer = Bukkit.getOfflinePlayer(progression.playerId());
      boolean isViewer = sender instanceof Player player
          && progression.playerId().equals(player.getUniqueId());

      Component row = MiniMessage.miniMessage().deserialize(ENTRY_FORMAT, TagResolver.builder()
          .tag("rank", Tag.inserting(Component.text(i + 1 + (pageNumber - 1) * pageSize)))
          .tag("player", Tag.inserting(PlayerComponent.of(progressionPlayer)))
          .tag("level", Tag.inserting(LevelComponent.of(progression)))
          .build());

      if (isViewer) {
        row = Component.text("→ ", NamedTextColor.GOLD).append(row.color(NamedTextColor.YELLOW));
      }

      body = body.append(row).appendNewline();
    }
    sender.sendMessage(body);

    Messages.send(sender, "");
    Component navigation = buildNavigation(jobNameText, pageNumber, maxPages);
    sender.sendMessage(navigation);
    Messages.send(sender, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Messages.send(sender, "");
  }

  private Component buildNavigation(String jobName, int currentPage, int maxPages) {
    Component nav = Component.empty();

    if (currentPage > 1) {
      Component prevButton = Component.text("[< Previous]")
          .color(NamedTextColor.GREEN)
          .clickEvent(ClickEvent.runCommand("/jobs top " + jobName + " " + (currentPage - 1)))
          .hoverEvent(Component.text("Click to go to page " + (currentPage - 1)));
      nav = nav.append(prevButton);
    } else {
      nav = nav.append(Component.text("[< Previous]").color(NamedTextColor.DARK_GRAY));
    }

    nav = nav.append(Component.text("  ").color(NamedTextColor.GRAY));

    if (currentPage < maxPages) {
      Component nextButton = Component.text("[Next >]")
          .color(NamedTextColor.GREEN)
          .clickEvent(ClickEvent.runCommand("/jobs top " + jobName + " " + (currentPage + 1)))
          .hoverEvent(Component.text("Click to go to page " + (currentPage + 1)));
      nav = nav.append(nextButton);
    } else {
      nav = nav.append(Component.text("[Next >]").color(NamedTextColor.DARK_GRAY));
    }

    return nav;
  }

  private int findPlayerRank(Player player, List<JobProgression> allEntries) {
    for (int i = 0; i < allEntries.size(); i++) {
      if (allEntries.get(i).playerId().equals(player.getUniqueId())) {
        return i + 1;
      }
    }
    return -1;
  }
}
