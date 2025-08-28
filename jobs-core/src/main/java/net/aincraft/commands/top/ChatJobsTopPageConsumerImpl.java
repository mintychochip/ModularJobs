package net.aincraft.commands.top;

import java.util.List;
import net.aincraft.JobProgression;
import net.aincraft.commands.Page;
import net.aincraft.commands.PageConsumer;
import net.aincraft.commands.components.PlayerComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public final class ChatJobsTopPageConsumerImpl implements PageConsumer<JobProgression> {

  private static final String ENTRY_FORMAT = "<rank>. <player>: <level>";

  private static Component player(@Nullable String playerName, String playerId) {
    playerName = playerName != null ? playerName : "Invalid";
    return Component.text(playerName)
        .hoverEvent(HoverEvent.showText(Component.text(playerId)));
  }

  @Override
  public void consume(Page<JobProgression> page, CommandSender sender) {
    Component body = Component.empty();
    List<JobProgression> data = page.data();
    int pageNumber = page.pageNumber();
    for (int i = 0; i < data.size(); i++) {
      JobProgression progression = data.get(i);
      OfflinePlayer progressionPlayer = progression.player();
      Component row = MiniMessage.miniMessage().deserialize(ENTRY_FORMAT, TagResolver.builder()
          .tag("rank", Tag.inserting(Component.text((i + 1) + (pageNumber - 1) * data.size())))
          .tag("player", Tag.inserting(PlayerComponent.of(progressionPlayer)))
          .tag("level", Tag.inserting(LevelComponent.of(progression)))
              .build());
      body = body.append(row).appendNewline();
    }
    sender.sendMessage(body);
  }
}
