package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.aincraft.JobProgression;
import net.aincraft.boost.AdditiveBoostImpl;
import net.aincraft.boost.MultiplicativeBoostImpl;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.RuledBoostSource;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService.ActiveBoostData;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import net.aincraft.service.JobService;
import net.aincraft.upgrade.UpgradeBoostDataService;
import dev.mintychochip.mint.Mint;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BoostsCommand implements JobsCommand {

  private final ItemBoostDataService itemBoostDataService;
  private final TimedBoostDataService timedBoostDataService;
  private final UpgradeBoostDataService upgradeBoostDataService;
  private final JobService jobService;

  @Inject
  public BoostsCommand(ItemBoostDataService itemBoostDataService,
      TimedBoostDataService timedBoostDataService,
      UpgradeBoostDataService upgradeBoostDataService,
      JobService jobService) {
    this.itemBoostDataService = itemBoostDataService;
    this.timedBoostDataService = timedBoostDataService;
    this.upgradeBoostDataService = upgradeBoostDataService;
    this.jobService = jobService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("boosts")
        .executes(context -> {
          CommandSourceStack source = context.getSource();

          if (!(source.getSender() instanceof Player player)) {
            Mint.sendThemedMessage(source.getSender(), "<error>This command can only be used by players.");
            return 0;
          }

          // Header
          Mint.sendThemedMessage(player, "<neutral>━━━━━━━━━ <primary>Active Boosts<neutral> ━━━━━━━━━");
          Mint.sendThemedMessage(player, "");

          // Timed Boosts
          List<ActiveBoostData> timedBoosts = timedBoostDataService.findApplicableBoosts(
              new PlayerTarget(player));

          if (!timedBoosts.isEmpty()) {
            Mint.sendThemedMessage(player, "<secondary>⏰ Timed Boosts:");
            for (ActiveBoostData boost : timedBoosts) {
              String timeRemaining = getTimeRemaining(boost);
              String boostEffects = formatBoostEffects(boost.boostSource());
              Mint.sendThemedMessage(player, "<neutral>  • <secondary>" + boost.boostSource().key().asString());
              Mint.sendThemedMessage(player, "<neutral>      <accent>" + boostEffects + " <neutral>- " + timeRemaining);
            }
            Mint.sendThemedMessage(player, "");
          }

          // Passive Item Boosts
          List<PassiveBoostInfo> passiveBoosts = getPassiveBoosts(player);

          if (!passiveBoosts.isEmpty()) {
            Mint.sendThemedMessage(player, "<secondary>🛡 Passive Boosts:");
            for (PassiveBoostInfo info : passiveBoosts) {
              String boostEffects = formatBoostEffects(info.boostSource);
              Mint.sendThemedMessage(player, "<neutral>  • <secondary>" + info.boostSource.key().asString() + " <neutral>(Slot " + info.slot + ")");
              Mint.sendThemedMessage(player, "<neutral>      <accent>" + boostEffects);
            }
            Mint.sendThemedMessage(player, "");
          }

          // Upgrade Tree Boosts (now uses the same BoostSource API)
          List<JobProgression> progressions = jobService.getProgressions(player);
          boolean hasUpgradeBoosts = false;

          for (JobProgression progression : progressions) {
            List<BoostSource> upgradeBoosts = upgradeBoostDataService.getBoostSources(
                player.getUniqueId(), progression.job().key());

            if (!upgradeBoosts.isEmpty()) {
              if (!hasUpgradeBoosts) {
                Mint.sendThemedMessage(player, "<secondary>⬆ Upgrade Boosts:");
                hasUpgradeBoosts = true;
              }

              String jobName = progression.job().key().value();
              for (BoostSource upgradeSource : upgradeBoosts) {
                String boostEffects = formatBoostEffects(upgradeSource);
                String desc = upgradeSource.description() != null ? upgradeSource.description() : upgradeSource.key().value();
                Mint.sendThemedMessage(player, "<neutral>  • <secondary>" + jobName + " <neutral>(" + desc + "): <accent>" + boostEffects);
              }
            }
          }

          if (hasUpgradeBoosts) {
            Mint.sendThemedMessage(player, "");
          }

          // No boosts message
          if (timedBoosts.isEmpty() && passiveBoosts.isEmpty() && !hasUpgradeBoosts) {
            Mint.sendThemedMessage(player, "<neutral>  You have no active boosts. ☹");
            Mint.sendThemedMessage(player, "");
          }

          // Footer
          Mint.sendThemedMessage(player, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━");

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
        BitSet slotSet = passiveData.slotSet();
        if (slotSet.get(slot)) {
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

  private String formatBoostEffects(BoostSource source) {
    if (source instanceof RuledBoostSource ruledSource) {
      List<Rule> rules = ruledSource.rules();
      if (rules.isEmpty()) {
        return "No effects";
      }

      // Collect all unique boost effects
      List<String> effects = rules.stream()
          .map(rule -> formatBoost(rule.boost()))
          .distinct()
          .collect(Collectors.toList());

      if (effects.size() == 1) {
        return effects.get(0);
      }

      return String.join(", ", effects);
    }

    // For non-ruled boost sources, try to get description
    String desc = source.description();
    return desc != null && !desc.isEmpty() ? desc : "Active";
  }

  private String formatBoost(Boost boost) {
    if (boost instanceof MultiplicativeBoostImpl multi) {
      return "x" + multi.amount().stripTrailingZeros().toPlainString();
    } else if (boost instanceof AdditiveBoostImpl add) {
      return "+" + add.amount().stripTrailingZeros().toPlainString();
    }
    return boost.getClass().getSimpleName();
  }
}
