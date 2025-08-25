package net.aincraft.commands.top;

import java.util.List;
import net.aincraft.JobProgression;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public final class ChatJobsTopPageConsumerImpl implements PageConsumer<List<JobProgression>> {

  private static final String FORMAT = "<rank>. <player>: <level>";

  private static Component player(@Nullable String playerName, String playerId) {
    playerName = playerName != null ? playerName : "Invalid";
    return Component.text(playerName)
        .hoverEvent(HoverEvent.showText(Component.text(playerId)));
  }

  @Override
  public void consume(Page<List<JobProgression>> page, CommandSender sender) {
    Component body = Component.empty();
    List<JobProgression> progressions = page.content();
    int pageNumber = page.pageNumber();
    for (int i = 0; i < page.pageSize(); i++) {
      JobProgression progression = progressions.get(i);
      OfflinePlayer progressionPlayer = progression.getPlayer();
      Component row = MiniMessage.miniMessage().deserialize(FORMAT, TagResolver.builder()
          .tag("rank", Tag.inserting(Component.text((i + 1) + (pageNumber - 1) * page.pageSize())))
          .tag("player", Tag.inserting(
              player(progressionPlayer.getName(), progressionPlayer.getUniqueId().toString())))
          .tag("level", Tag.inserting(Component.text(progression.getLevel())))
          .build());
      body = body.append(row).appendNewline();
    }
    sender.sendMessage(body);
  }
}
