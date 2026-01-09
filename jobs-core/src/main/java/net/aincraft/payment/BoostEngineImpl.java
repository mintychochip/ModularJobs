package net.aincraft.payment;

import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
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
import net.aincraft.container.SlotSet;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService.ActiveBoostData;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import net.aincraft.upgrade.UpgradeBoostDataService;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

final class BoostEngineImpl implements BoostEngine {

  private final ItemBoostDataService boostDataService;
  private final TimedBoostDataService timedBoostDataService;
  private final UpgradeBoostDataService upgradeBoostDataService;

  @Inject
  public BoostEngineImpl(ItemBoostDataService boostDataService,
      TimedBoostDataService timedBoostDataService,
      UpgradeBoostDataService upgradeBoostDataService) {
    this.boostDataService = boostDataService;
    this.timedBoostDataService = timedBoostDataService;
    this.upgradeBoostDataService = upgradeBoostDataService;
  }

  @Override
  public Map<Key, Boost> evaluate(OfflinePlayer player, ActionType type, Context context,
      JobProgression progression, Payable payable) {
    Map<Key, List<Boost>> boostsBySource = new HashMap<>();

    if (!player.isOnline()) {
      return Map.of();
    }
    Player onlinePlayer = player.getPlayer();
    if (onlinePlayer == null) {
      return Map.of();
    }

    BoostContext boostContext = new BoostContext(type, progression, onlinePlayer, payable);

    // Aggregate passive item sources
    List<BoostSource> itemSources = aggregateItemSources(onlinePlayer);
    for (BoostSource source : itemSources) {
      List<Boost> evaluated = source.evaluate(boostContext);
      if (!evaluated.isEmpty()) {
        boostsBySource.put(source.key(), evaluated);
      }
    }

    // Aggregate timed boost sources
    List<ActiveBoostData> timedBoosts = timedBoostDataService.findApplicableBoosts(
        new PlayerTarget(onlinePlayer));
    for (ActiveBoostData activeBoost : timedBoosts) {
      BoostSource source = activeBoost.boostSource();
      List<Boost> evaluated = source.evaluate(boostContext);
      if (!evaluated.isEmpty()) {
        boostsBySource.put(source.key(), evaluated);
      }
    }

    // Aggregate upgrade tree boost sources (now uses the same BoostSource API)
    List<BoostSource> upgradeSources = upgradeBoostDataService.getBoostSources(
        onlinePlayer.getUniqueId(),
        progression.job().key()
    );
    for (BoostSource source : upgradeSources) {
      List<Boost> evaluated = source.evaluate(boostContext);
      if (!evaluated.isEmpty()) {
        boostsBySource.put(source.key(), evaluated);
      }
    }

    // Flatten: for each source, combine boosts into a single composite boost
    Map<Key, Boost> result = new HashMap<>();
    for (Map.Entry<Key, List<Boost>> entry : boostsBySource.entrySet()) {
      List<Boost> sourceBoosts = entry.getValue();
      if (sourceBoosts.size() == 1) {
        result.put(entry.getKey(), sourceBoosts.get(0));
      } else {
        // Combine multiple boosts from same source into composite
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
        SlotSet slotSet = passiveBoostData.slotSet();
        BoostSource boostSource = passiveBoostData.boostSource();
        if (slotSet.contains(i) && !boostSourceKeys.contains(boostSource.key())) {
          sources.add(boostSource);
          boostSourceKeys.add(boostSource.key());
        }
      }
    }
    return sources;
  }

}
