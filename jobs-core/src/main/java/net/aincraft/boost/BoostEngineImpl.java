package net.aincraft.boost;

import java.util.List;
import java.util.Optional;
import net.aincraft.container.ActionType;
import net.aincraft.container.Boost;
import net.aincraft.container.boost.ItemBoostData;
import net.aincraft.container.boost.ItemBoostData.PassiveBoostData;
import net.aincraft.container.boost.ItemBoostDataService;
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
  public List<Boost> evaluate(ActionType actionType, Player player) {
    PlayerInventory inventory = player.getInventory();
    for (ItemStack itemStack : inventory.getContents()) {
      if (itemStack == null) {
        continue;
      }
      Bukkit.broadcastMessage(itemStack.toString());
      Optional<ItemBoostData> data = boostDataService.getBoostData(itemStack);
      if (data.isEmpty()) {
        continue;
      }
      if (data.get() instanceof PassiveBoostData passiveBoostData) {
        int[] applicableSlots = passiveBoostData.getApplicableSlots();
      }
      Bukkit.broadcastMessage(data.get().toString());
    }
    return List.of();
  }
}
