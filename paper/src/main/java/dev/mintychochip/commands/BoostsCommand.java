package dev.mintychochip.commands;

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
import dev.mintychochip.JobProgression;
import dev.mintychochip.boost.AdditiveBoostImpl;
import dev.mintychochip.boost.MultiplicativeBoostImpl;
import dev.mintychochip.container.Boost;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import dev.mintychochip.service.ItemBoostDataService;
import dev.mintychochip.container.boost.RuledBoostSource;
import dev.mintychochip.container.boost.RuledBoostSource.Rule;
import dev.mintychochip.container.boost.TimedBoostDataService;
import dev.mintychochip.container.boost.TimedBoostDataService.ActiveBoostData;
import dev.mintychochip.container.boost.TimedBoostDataService.Target.PlayerTarget;
import dev.mintychochip.service.JobService;
import dev.mintychochip.upgrade.UpgradeBoostDataService;
import dev.mintychochip.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * {@code /jobs boosts} command: displays the invoking player's active timed boosts,
 * passive item-slot boosts, and upgrade-tree boost sources, formatting each effect
 * for chat output.
 */
public class BoostsCommand implements JobsCommand {

  private final ItemBoostDataService itemBoostDataService;
  private final TimedBoostDataService timedBoostDataService;
  private final UpgradeBoostDataService upgradeBoostDataService;
  private final JobService jobService;

  /**
   * Creates the boosts command with the services that supply timed, item-passive, upgrade,
   * and job-progression data.
   */
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
            Messages.send(source.getSender(), "<error>This command can only be used by players.");
            return 0;
          }

          // Header
          Messages.send(player, "<neutral>━━━━━━━━━ <primary>Active Boosts<neutral> ━━━━━━━━━");
          Messages.send(player, "");

          // Timed Boosts
          List<ActiveBoostData> timedBoosts = timedBoostDataService.findApplicableBoosts(
              new PlayerTarget(player.getUniqueId()));

          if (!timedBoosts.isEmpty()) {
            Messages.send(player, "<secondary>⏰ Timed Boosts:");
            for (ActiveBoostData boost : timedBoosts) {
              String timeRemaining = getTimeRemaining(boost);
              String boostEffects = formatBoostEffects(boost.boostSource());
              Messages.send(player, "<neutral>  • <secondary>" + boost.boostSource().key().asString());
              Messages.send(player, "<neutral>      <accent>" + boostEffects + " <neutral>- " + timeRemaining);
            }
            Messages.send(player, "");
          }

          // Passive Item Boosts
          List<PassiveBoostInfo> passiveBoosts = getPassiveBoosts(player);

          if (!passiveBoosts.isEmpty()) {
            Messages.send(player, "<secondary>🛡 Passive Boosts:");
            for (PassiveBoostInfo info : passiveBoosts) {
              String boostEffects = formatBoostEffects(info.boostSource);
              Messages.send(player, "<neutral>  • <secondary>" + info.boostSource.key().asString() + " <neutral>(Slot " + info.slot + ")");
              Messages.send(player, "<neutral>      <accent>" + boostEffects);
            }
            Messages.send(player, "");
          }

          // Upgrade Tree Boosts (now uses the same BoostSource API)
          List<JobProgression> progressions = jobService.getProgressions(player.getUniqueId());
          boolean hasUpgradeBoosts = false;

          for (JobProgression progression : progressions) {
            List<BoostSource> upgradeBoosts = upgradeBoostDataService.getBoostSources(
                player.getUniqueId(), progression.job().key());

            if (!upgradeBoosts.isEmpty()) {
              if (!hasUpgradeBoosts) {
                Messages.send(player, "<secondary>⬆ Upgrade Boosts:");
                hasUpgradeBoosts = true;
              }

              String jobName = progression.job().key().value();
              for (BoostSource upgradeSource : upgradeBoosts) {
                String boostEffects = formatBoostEffects(upgradeSource);
                String desc = upgradeSource.description() != null ? upgradeSource.description() : upgradeSource.key().value();
                Messages.send(player, "<neutral>  • <secondary>" + jobName + " <neutral>(" + desc + "): <accent>" + boostEffects);
              }
            }
          }

          if (hasUpgradeBoosts) {
            Messages.send(player, "");
          }

          // No boosts message
          if (timedBoosts.isEmpty() && passiveBoosts.isEmpty() && !hasUpgradeBoosts) {
            Messages.send(player, "<neutral>  You have no active boosts. ☹");
            Messages.send(player, "");
          }

          // Footer
          Messages.send(player, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━");

          return Command.SINGLE_SUCCESS;
        });
  }

  /**
   * Formats the remaining lifetime of an active timed boost into a compact human-readable
   * duration, or {@code "Permanent"}/{@code "Expired"} when applicable.
   */
  private String getTimeRemaining(ActiveBoostData boost) {
    if (boost.duration() == null) {
      return "Permanent";
    }

    if (boost.isExpired()) {
      return "Expired";
    }

    long expiresAt = boost.started().toEpochMilli() + boost.duration().toMillis();
    long remaining = expiresAt - System.currentTimeMillis();

    long hours = remaining / (1000 * 60 * 60);
    long minutes = remaining / (1000 * 60) % 60;
    long seconds = remaining / 1000 % 60;

    if (hours > 0) {
      return String.format("%dh %dm", hours, minutes);
    } else if (minutes > 0) {
      return String.format("%dm %ds", minutes, seconds);
    } else {
      return String.format("%ds", seconds);
    }
  }

  /**
   * Collects the passive boost sources bound to the player's inventory slots, deduplicated
   * by boost-source key.
   *
   * @param player the player whose inventory is scanned
   * @return the distinct passive boosts with their originating slot
   */
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

  /** A passive boost source together with the inventory slot it is bound to. */
  private record PassiveBoostInfo(BoostSource boostSource, int slot) {
  }

  /** Renders the effects of a boost source as a comma-joined string for chat display. */
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

  /**
   * Formats a single boost as its multiplicative ({@code x…}) or additive ({@code +…})
   * shorthand, falling back to the simple class name for unknown boost types.
   */
  private String formatBoost(Boost boost) {
    if (boost instanceof MultiplicativeBoostImpl multi) {
      return "x" + multi.amount().stripTrailingZeros().toPlainString();
    } else if (boost instanceof AdditiveBoostImpl add) {
      return "+" + add.amount().stripTrailingZeros().toPlainString();
    }
    return boost.getClass().getSimpleName();
  }
}
