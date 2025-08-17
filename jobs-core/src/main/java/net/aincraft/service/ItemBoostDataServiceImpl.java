package net.aincraft.service;

import io.papermc.paper.persistence.PersistentDataContainerView;
import java.util.Optional;
import net.aincraft.container.boost.ItemBoostData;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.registry.RegistryKeys;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ItemBoostDataServiceImpl implements ItemBoostDataService {

  private static final NamespacedKey ITEM_BOOST_DATA_KEY = NamespacedKey.fromString(
      "modular_jobs:item_boost_data");

  private final CodecRegistry codecRegistry;

  public ItemBoostDataServiceImpl(CodecRegistry codecRegistry) {
    this.codecRegistry = codecRegistry;
  }

  @Override
  public boolean addBoostData(ItemBoostData data, ItemStack itemStack) {
    byte[] blob = codecRegistry.encode(data);
    return itemStack.editPersistentDataContainer(
        p -> p.set(ITEM_BOOST_DATA_KEY, PersistentDataType.BYTE_ARRAY, blob));
  }


  @Override
  public void removeBoostData(ItemStack itemStack) {

  }

  //TODO: identify the underlying behavior of the codec, so we can mark it as nullable/notnull
  @Override
  public Optional<ItemBoostData> getBoostData(ItemStack itemStack) {
    PersistentDataContainerView pdc = itemStack.getPersistentDataContainer();
    if (!pdc.has(ITEM_BOOST_DATA_KEY)) {
      return Optional.empty();
    }
    byte[] blob = pdc.get(ITEM_BOOST_DATA_KEY, PersistentDataType.BYTE_ARRAY);
    return Optional.of(codecRegistry.decode(blob, ItemBoostData.class));
  }
}
