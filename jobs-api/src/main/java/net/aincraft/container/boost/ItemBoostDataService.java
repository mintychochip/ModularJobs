package net.aincraft.container.boost;

import java.util.Optional;
import org.bukkit.inventory.ItemStack;

public interface ItemBoostDataService {
  void addData(BoostData.SerializableBoostData data, ItemStack stack);
  Optional<BoostData.SerializableBoostData> getData(ItemStack stack);
}
