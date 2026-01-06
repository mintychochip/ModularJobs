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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public final class ChatJobsTopPageConsumerImpl implements JobsTopPageConsumer {

  private static final String ENTRY_FORMAT = "<rank>. <player>: <level>";

  @Override
  public void consume(Component jobName, Page<JobProgression> page, CommandSender sender,
      int maxPages) {
    String jobNameText = PlainTextComponentSerializer.plainText().serialize(jobName);
    Mint.sendMessage(sender, "<primary>Jobs Top </primary><accent>" + jobNameText + "</accent>");
    Component body = Component.empty();
    List<JobProgression> data = page.data();
    int pageNumber = page.pageNumber();
    int pageSize = page.size();
    for (int i = 0; i < data.size(); i++) {
      JobProgression progression = data.get(i);
      Component row = MiniMessage.miniMessage().deserialize(ENTRY_FORMAT, TagResolver.builder()
          .tag("rank", Tag.inserting(Component.text((i + 1) + (pageNumber - 1) * pageSize)))
          .tag("player", Tag.inserting(PlayerComponent.of(progression.player())))
          .tag("level", Tag.inserting(LevelComponent.of(progression)))
          .build());
      body = body.append(row).appendNewline();
    }
    sender.sendMessage(body);
  }
}
