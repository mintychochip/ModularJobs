package net.aincraft.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Local themed MiniMessage messaging — replaces Mint.sendThemedMessage /
 * Mint.createThemedComponent. Tags: {@code primary}, {@code secondary},
 * {@code neutral}, {@code accent}, {@code error}, {@code success}, {@code info}.
 */
@NullMarked
public final class Messages {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
      .tags(TagResolver.builder()
          .resolver(StandardTags.defaults())
          .tag("primary", Tag.styling(NamedTextColor.GOLD))
          .tag("secondary", Tag.styling(NamedTextColor.YELLOW))
          .tag("neutral", Tag.styling(NamedTextColor.GRAY))
          .tag("accent", Tag.styling(NamedTextColor.AQUA))
          .tag("error", Tag.styling(NamedTextColor.RED))
          .tag("success", Tag.styling(NamedTextColor.GREEN))
          .tag("info", Tag.styling(NamedTextColor.BLUE))
          .build())
      .build();

  /** Prevents instantiation of this static utility class. */
  private Messages() {
  }

  /** Deserialize a themed MiniMessage string to a Component. */
  public static Component component(@Nullable String message) {
    if (message == null || message.isEmpty()) {
      return Component.empty();
    }
    return MINI_MESSAGE.deserialize(message).decoration(TextDecoration.ITALIC, false);
  }

  /** Send a themed MiniMessage string to a command sender. */
  public static void send(CommandSender sender, @Nullable String message) {
    sender.sendMessage(component(message));
  }

  /** Exposed for tests — same MiniMessage instance used at runtime. */
  static MiniMessage miniMessage() {
    return MINI_MESSAGE;
  }
}
