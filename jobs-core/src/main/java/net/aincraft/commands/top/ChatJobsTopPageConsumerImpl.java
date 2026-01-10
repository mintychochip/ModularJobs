package net.aincraft.commands.top;

import dev.mintychochip.mint.Mint;
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
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ChatJobsTopPageConsumerImpl implements JobsTopPageConsumer {

  private static final String ENTRY_FORMAT = "<rank>. <player>: <level>";

  @Override
  public void consume(Component jobName, Page<JobProgression> page, CommandSender sender,
      int maxPages, List<JobProgression> allEntries) {
    String jobNameText = PlainTextComponentSerializer.plainText().serialize(jobName);
    int pageNumber = page.pageNumber();

    // Header with page info
    Mint.sendThemedMessage(sender, "");
    Mint.sendThemedMessage(sender, "<neutral>━━━━━━ <primary>Jobs Top </primary><accent>"
        + jobNameText + "</accent> <neutral>━━━━━━");
    Mint.sendThemedMessage(sender, "<neutral>Page " + pageNumber + " of " + maxPages);

    // Show viewer's rank if they're a player and in the leaderboard
    if (sender instanceof Player player) {
      int rank = findPlayerRank(player, allEntries);
      if (rank > 0) {
        Mint.sendThemedMessage(sender, "<accent>Your Rank: <primary>#" + rank);
      }
    }

    // Show total players
    Mint.sendThemedMessage(sender, "<neutral>Showing top <accent>" + allEntries.size()
        + "<neutral> players");
    Mint.sendThemedMessage(sender, "");

    // Leaderboard entries
    Component body = Component.empty();
    List<JobProgression> data = page.data();
    int pageSize = page.size();
    for (int i = 0; i < data.size(); i++) {
      JobProgression progression = data.get(i);
      boolean isViewer = sender instanceof Player player
          && progression.player().getUniqueId().equals(player.getUniqueId());

      Component row = MiniMessage.miniMessage().deserialize(ENTRY_FORMAT, TagResolver.builder()
          .tag("rank", Tag.inserting(Component.text((i + 1) + (pageNumber - 1) * pageSize)))
          .tag("player", Tag.inserting(PlayerComponent.of(progression.player())))
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
    Mint.sendThemedMessage(sender, "");
    Component navigation = buildNavigation(jobNameText, pageNumber, maxPages);
    sender.sendMessage(navigation);
    Mint.sendThemedMessage(sender, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Mint.sendThemedMessage(sender, "");
  }

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

  private int findPlayerRank(Player player, List<JobProgression> allEntries) {
    for (int i = 0; i < allEntries.size(); i++) {
      JobProgression progression = allEntries.get(i);
      if (progression.player().getUniqueId().equals(player.getUniqueId())) {
        return i + 1; // Rank is 1-indexed
      }
    }
    return -1; // Player not found in leaderboard
  }
}
