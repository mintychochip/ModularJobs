package net.aincraft.payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.aincraft.JobProgression;
import net.aincraft.container.ActionType;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.BoostSource;
import net.aincraft.container.Context;
import net.aincraft.container.Payable;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import net.aincraft.service.ItemBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService.ActiveBoostData;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import dev.conditions.paper.PaperConditionContexts;
import dev.databag.DataBag;
import net.aincraft.boost.ModularJobsBags;
import net.aincraft.upgrade.UpgradeBoostDataService;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Aggregates passive item, timed, and upgrade-tree boost sources, evaluates them
 * against a {@link BoostContext}, and returns one {@link Boost} per source key.
 */
public final class BoostEngine {

  private final ItemBoostDataService boostDataService;
  private final TimedBoostDataService timedBoostDataService;
  private final UpgradeBoostDataService upgradeBoostDataService;

  /**
   * Composes the three boost-data services (item, timed, upgrade-tree) into the aggregation
   * engine.
   */
  public BoostEngine(ItemBoostDataService boostDataService,
      TimedBoostDataService timedBoostDataService,
      UpgradeBoostDataService upgradeBoostDataService) {
    this.boostDataService = boostDataService;
    this.timedBoostDataService = timedBoostDataService;
    this.upgradeBoostDataService = upgradeBoostDataService;
  }

  /**
   * Evaluates the full boost set for a player's action: aggregates the player's item-sourced,
   * timed, and upgrade-tree boost sources, then applies them for {@code type} against
   * {@code progression} and {@code payable}.
   *
   * @return one boost per source key; empty when the player is offline
   */
  public Map<Key, Boost> evaluate(OfflinePlayer player, ActionType type, Context context,
      JobProgression progression, Payable payable) {
    if (!player.isOnline()) {
      return Map.of();
    }
    Player onlinePlayer = player.getPlayer();
    if (onlinePlayer == null) {
      return Map.of();
    }

    String jobKey = progression == null || progression.job() == null
        ? null
        : progression.job().key().asString();
    DataBag extras = ModularJobsBags.extras(
        jobKey, progression == null ? 0 : progression.level());
    BoostContext boostContext = new BoostContext(
        type,
        progression,
        onlinePlayer.getUniqueId(),
        onlinePlayer.getWorld().getName(),
        payable,
        PaperConditionContexts.from(
            onlinePlayer,
            jobKey == null ? Set.of() : Set.of(jobKey),
            extras));
    List<BoostSource> itemSources = aggregateItemSources(onlinePlayer);
    List<ActiveBoostData> timedBoosts = timedBoostDataService.findApplicableBoosts(
        new PlayerTarget(onlinePlayer.getUniqueId()));
    List<BoostSource> upgradeSources = upgradeBoostDataService.getBoostSources(
        onlinePlayer.getUniqueId(),
        progression.job().key()
    );
    return evaluateSources(boostContext, itemSources, timedBoosts, upgradeSources);
  }

  /**
   * Pure evaluation path used by payment and unit tests: given already-resolved sources,
   * evaluate each against {@code context} and flatten to one boost per source key.
   */
  public Map<Key, Boost> evaluateSources(
      BoostContext context,
      List<BoostSource> itemSources,
      List<ActiveBoostData> timedBoosts,
      List<BoostSource> upgradeSources) {
    Map<Key, List<Boost>> boostsBySource = new HashMap<>();

    for (BoostSource source : itemSources) {
      collect(boostsBySource, source, context);
    }
    for (ActiveBoostData activeBoost : timedBoosts) {
      collect(boostsBySource, activeBoost.boostSource(), context);
    }
    for (BoostSource source : upgradeSources) {
      collect(boostsBySource, source, context);
    }

    return flatten(boostsBySource);
  }

  /**
   * Apply evaluated boosts to a base amount (same loop as payment handling).
   */
  public static BigDecimal applyBoosts(BigDecimal baseAmount, Map<Key, Boost> boosts) {
    BigDecimal boostedAmount = baseAmount;
    for (Boost boost : boosts.values()) {
      boostedAmount = boost.boost(boostedAmount);
    }
    return boostedAmount;
  }

  private static void collect(
      Map<Key, List<Boost>> boostsBySource, BoostSource source, BoostContext context) {
    List<Boost> evaluated = source.evaluate(context);
    if (!evaluated.isEmpty()) {
      boostsBySource.put(source.key(), evaluated);
    }
  }

  private static Map<Key, Boost> flatten(Map<Key, List<Boost>> boostsBySource) {
    Map<Key, Boost> result = new HashMap<>();
    for (Map.Entry<Key, List<Boost>> entry : boostsBySource.entrySet()) {
      List<Boost> sourceBoosts = entry.getValue();
      if (sourceBoosts.size() == 1) {
        result.put(entry.getKey(), sourceBoosts.get(0));
      } else {
        result.put(entry.getKey(), amount -> {
          BigDecimal current = amount;
          for (Boost b : sourceBoosts) {
            current = b.boost(current);
          }
          return current;
        });
      }
    }
    return result;
  }

  private List<BoostSource> aggregateItemSources(Player player) {
    List<BoostSource> sources = new ArrayList<>();
    Set<Key> boostSourceKeys = new HashSet<>();
    PlayerInventory inventory = player.getInventory();
    for (int i = 0; i < inventory.getSize(); ++i) {
      ItemStack itemStack = inventory.getItem(i);
      if (itemStack == null) {
        continue;
      }
      Optional<SerializableBoostData> data = boostDataService.getData(itemStack);
      if (data.isEmpty()) {
        continue;
      }
      SerializableBoostData serializableBoostData = data.get();
      if (serializableBoostData instanceof PassiveBoostData passiveBoostData) {
        BitSet slotSet = passiveBoostData.slotSet();
        BoostSource boostSource = passiveBoostData.boostSource();
        if (slotSet.get(i) && !boostSourceKeys.contains(boostSource.key())) {
          sources.add(boostSource);
          boostSourceKeys.add(boostSource.key());
        }
      }
    }
    return sources;
  }
}
