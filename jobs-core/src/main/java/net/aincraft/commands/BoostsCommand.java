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
import dev.mintychochip.mint.Mint;
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
            Mint.sendMessage(source.getSender(), "<error>This command can only be used by players.");
            return 0;
          }

          // Header
          Mint.sendMessage(player, "<neutral>━━━━━━━━━ <primary>Active Boosts<neutral> ━━━━━━━━━");
          Mint.sendMessage(player, "");

          // Timed Boosts
          List<ActiveBoostData> timedBoosts = timedBoostDataService.findApplicableBoosts(
              new PlayerTarget(player));

          if (!timedBoosts.isEmpty()) {
            Mint.sendMessage(player, "<secondary>⏰ Timed Boosts:");
            for (ActiveBoostData boost : timedBoosts) {
              String timeRemaining = getTimeRemaining(boost);
              Mint.sendMessage(player, "<neutral>  • <secondary>" + boost.boostSource().key().asString() + "<accent> - " + timeRemaining);
            }
            Mint.sendMessage(player, "");
          }

          // Passive Item Boosts
          List<PassiveBoostInfo> passiveBoosts = getPassiveBoosts(player);

          if (!passiveBoosts.isEmpty()) {
            Mint.sendMessage(player, "<secondary>🛡 Passive Boosts:");
            for (PassiveBoostInfo info : passiveBoosts) {
              Mint.sendMessage(player, "<neutral>  • <secondary>" + info.boostSource.key().asString() + "<neutral> (Slot " + info.slot + ")");
            }
            Mint.sendMessage(player, "");
          }

          // No boosts message
          if (timedBoosts.isEmpty() && passiveBoosts.isEmpty()) {
            Mint.sendMessage(player, "<neutral>  You have no active boosts. ☹");
            Mint.sendMessage(player, "");
          }

          // Footer
          Mint.sendMessage(player, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━");

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
