package net.aincraft.payment;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobProgressionView;
import net.aincraft.JobTask;
import net.aincraft.PayableCurve;
import net.aincraft.PayableCurve.Parameters;
import net.aincraft.container.ActionType;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.BoostSource;
import net.aincraft.container.Context;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableType;
import net.aincraft.container.SlotSet;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService.ActiveBoostData;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

final class BoostEngineImpl implements BoostEngine {

  private final ItemBoostDataService boostDataService;
  private final TimedBoostDataService timedBoostDataService;

  @Inject
  public BoostEngineImpl(ItemBoostDataService boostDataService,
      TimedBoostDataService timedBoostDataService) {
    this.boostDataService = boostDataService;
    this.timedBoostDataService = timedBoostDataService;
  }

  @Override
  public Map<Key, Boost> evaluate(OfflinePlayer player, ActionType type, Context context,
      JobProgression progression, Payable payable) {
    Map<Key,Boost> boosts = new HashMap<>();
    Job job = progression.job();
    PayableAmount amount = payable.amount();
    PayableType payableType = payable.type();
    return Map.of();
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
