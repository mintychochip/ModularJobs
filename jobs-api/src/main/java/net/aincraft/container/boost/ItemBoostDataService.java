package net.aincraft.container.boost;

import java.util.Optional;
import org.bukkit.inventory.ItemStack;

public interface ItemBoostDataService {

  boolean addBoostData(ItemBoostData data, ItemStack itemStack);

  void removeBoostData(ItemStack itemStack);

  Optional<ItemBoostData> getBoostData(ItemStack itemStack);
}
