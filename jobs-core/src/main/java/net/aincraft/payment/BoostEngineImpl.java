package net.aincraft.payment;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.aincraft.JobProgressionView;
import net.aincraft.container.ActionType;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.BoostSource;
import net.aincraft.container.SlotSet;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import net.aincraft.container.boost.TimedBoostDataService.ActiveBoostData;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.registry.RegistryKeys;
import net.aincraft.registry.RegistryView;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
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
  public List<Boost> evaluate(ActionType actionType, JobProgressionView progression,
      Player player) {
    /**
     * implementation notes:
     * make a better api for registering longterm passive plugin boost sources, e.g. McMMO
     * 1. Count boost sources applicable from items *check*
     * 2. Count boost sources applicable from the player *check*
     * 3. Sources from global *check*
     * 4. Sources from passive plugin sources, e.g. mcmmo
     */
    BoostContext context = new BoostContext(actionType, progression, player);
    List<BoostSource> boostSources = aggregateItemSources(player);
    List<ActiveBoostData> applicableBoosts = timedBoostDataService.findApplicableBoosts(
        new PlayerTarget(player));
    boostSources.addAll(applicableBoosts.stream().map(ActiveBoostData::boostSource).toList());
    RegistryView<BoostSource> sources = RegistryContainer.registryContainer()
        .getRegistry(RegistryKeys.TRANSIENT_BOOST_SOURCES);
    sources.stream().forEach(source -> boostSources.add(source));
    Bukkit.broadcastMessage(boostSources.toString());
    return List.of();
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
