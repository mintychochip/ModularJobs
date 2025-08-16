package net.aincraft.service;

import java.util.Optional;
import net.aincraft.container.Codec;
import net.aincraft.container.boost.ItemBoostData;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.registry.RegistryKeys;
import net.aincraft.registry.RegistryView;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ItemBoostDataServiceImpl implements ItemBoostDataService {

  private static final NamespacedKey ITEM_BOOST_DATA_KEY = NamespacedKey.fromString(
      "modular_jobs:item_boost_data");

  @Override
  public void addBoostData(ItemBoostData data, ItemStack itemStack) {
    if (RegistryContainer.registryContainer()
        .getRegistry(RegistryKeys.CODEC) instanceof CodecRegistry codec) {
      byte[] encodedBoostData = codec.encode(data);
      itemStack.editPersistentDataContainer(pdc -> {
        pdc.set(ITEM_BOOST_DATA_KEY, PersistentDataType.BYTE_ARRAY, encodedBoostData);
      });
      Bukkit.broadcastMessage(encodedBoostData.toString());
    }
  }

  @Override
  public void removeBoostData(ItemStack itemStack) {

  }

  @Override
  public Optional<ItemBoostData> getBoostData(ItemStack itemStack) {
    return Optional.empty();
  }
}
