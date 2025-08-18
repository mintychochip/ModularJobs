package net.aincraft.container.boost;

import java.util.Optional;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import org.bukkit.inventory.ItemStack;

public interface ItemBoostDataService {

  void addData(SerializableBoostData data, ItemStack stack);

  Optional<SerializableBoostData> getData(ItemStack stack)
      throws IllegalArgumentException;
}
