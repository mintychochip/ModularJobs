package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.aincraft.container.BoostSource;
import net.aincraft.container.SlotSet;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService.ActiveBoostData;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BoostsCommand implements JobsCommand {

  private final ItemBoostDataService itemBoostDataService;
  private final TimedBoostDataService timedBoostDataService;

  @Inject
  public BoostsCommand(ItemBoostDataService itemBoostDataService,
      TimedBoostDataService timedBoostDataService) {
    this.itemBoostDataService = itemBoostDataService;
    this.timedBoostDataService = timedBoostDataService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("boosts")
        .executes(context -> {
          CommandSourceStack source = context.getSource();

          if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text("This command can only be used by players.")
                .color(NamedTextColor.RED));
            return 0;
          }

          // Header
          player.sendMessage(Component.text("━━━━━━━━━ ", NamedTextColor.GRAY)
              .append(Component.text("Active Boosts", NamedTextColor.GOLD, TextDecoration.BOLD))
              .append(Component.text(" ━━━━━━━━━", NamedTextColor.GRAY)));
          player.sendMessage(Component.empty());

          // Timed Boosts
          List<ActiveBoostData> timedBoosts = timedBoostDataService.findApplicableBoosts(
              new PlayerTarget(player));

          if (!timedBoosts.isEmpty()) {
            player.sendMessage(Component.text("⏰ Timed Boosts:", NamedTextColor.YELLOW));
            for (ActiveBoostData boost : timedBoosts) {
              String timeRemaining = getTimeRemaining(boost);
              player.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
                  .append(Component.text(boost.boostSource().key().asString(), NamedTextColor.WHITE))
                  .append(Component.text(" - " + timeRemaining, NamedTextColor.AQUA)));
            }
            player.sendMessage(Component.empty());
          }

          // Passive Item Boosts
          List<PassiveBoostInfo> passiveBoosts = getPassiveBoosts(player);

          if (!passiveBoosts.isEmpty()) {
            player.sendMessage(Component.text("\uD83D\uDEE1 Passive Boosts:", NamedTextColor.YELLOW));
            for (PassiveBoostInfo info : passiveBoosts) {
              player.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
                  .append(Component.text(info.boostSource.key().asString(), NamedTextColor.WHITE))
                  .append(Component.text(" (Slot " + info.slot + ")", NamedTextColor.DARK_GRAY)));
            }
            player.sendMessage(Component.empty());
          }

          // No boosts message
          if (timedBoosts.isEmpty() && passiveBoosts.isEmpty()) {
            player.sendMessage(Component.text("  You have no active boosts.", NamedTextColor.GRAY)
                .append(Component.text(" ☹", NamedTextColor.DARK_GRAY)));
            player.sendMessage(Component.empty());
          }

          // Footer
          player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GRAY));

          return Command.SINGLE_SUCCESS;
        });
  }

  private String getTimeRemaining(ActiveBoostData boost) {
    if (boost.duration() == null) {
      return "Permanent";
    }

    if (boost.isExpired()) {
      return "Expired";
    }

    long expiresAt = boost.started().getTime() + boost.duration().toMillis();
    long remaining = expiresAt - System.currentTimeMillis();

    long hours = remaining / (1000 * 60 * 60);
    long minutes = (remaining / (1000 * 60)) % 60;
    long seconds = (remaining / 1000) % 60;

    if (hours > 0) {
      return String.format("%dh %dm", hours, minutes);
    } else if (minutes > 0) {
      return String.format("%dm %ds", minutes, seconds);
    } else {
      return String.format("%ds", seconds);
    }
  }

  private List<PassiveBoostInfo> getPassiveBoosts(Player player) {
    List<PassiveBoostInfo> passiveBoosts = new ArrayList<>();
    Set<String> seenBoostKeys = new HashSet<>();
    PlayerInventory inventory = player.getInventory();

    for (int slot = 0; slot < inventory.getSize(); slot++) {
      ItemStack item = inventory.getItem(slot);
      if (item == null) {
        continue;
      }

      Optional<SerializableBoostData> dataOpt = itemBoostDataService.getData(item);
      if (dataOpt.isEmpty()) {
        continue;
      }

      if (dataOpt.get() instanceof PassiveBoostData passiveData) {
        SlotSet slotSet = passiveData.slotSet();
        if (slotSet.contains(slot)) {
          BoostSource source = passiveData.boostSource();
          String key = source.key().asString();

          // Avoid duplicates
          if (!seenBoostKeys.contains(key)) {
            passiveBoosts.add(new PassiveBoostInfo(source, slot));
            seenBoostKeys.add(key);
          }
        }
      }
    }

    return passiveBoosts;
  }

  private record PassiveBoostInfo(BoostSource boostSource, int slot) {
  }
}
