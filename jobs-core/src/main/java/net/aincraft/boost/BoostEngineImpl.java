package net.aincraft.boost;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.aincraft.JobProgressionView;
import net.aincraft.container.ActionType;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.SlotSet;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.RuledBoostSource;
import net.aincraft.service.ProgressionService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class BoostEngineImpl implements BoostEngine {

  private final ItemBoostDataService boostDataService;

  public BoostEngineImpl(ItemBoostDataService boostDataService) {
    this.boostDataService = boostDataService;
  }

  @Override
  public List<Boost> evaluate(ActionType actionType, JobProgressionView progression,
      Player player) {
    PlayerInventory inventory = player.getInventory();
    List<Boost> boosts = new ArrayList<>();
    for (int i = 0; i < inventory.getSize(); ++i) {
      ItemStack itemStack = inventory.getItem(i);
      if (itemStack == null) {
        continue;
      }
      Optional<SerializableBoostData> data = boostDataService.getData(itemStack);
      if (data.isEmpty()) {
        continue;
      }
      SerializableBoostData boostData = data.get();
      if (boostData instanceof PassiveBoostData passiveBoostData) {
        Optional<SlotSet> slots = passiveBoostData.getApplicableSlots();
        if (slots.isEmpty()) {
          continue;
        }
        SlotSet set = slots.get();
        if (!set.contains(i)) {
          continue;
        }
        RuledBoostSource boostSource = boostData.getBoostSource();
        boosts.addAll(boostSource.evaluate(
            new BoostContext(actionType, progression,
                player)));
      }
    }
    return boosts;
  }
}
