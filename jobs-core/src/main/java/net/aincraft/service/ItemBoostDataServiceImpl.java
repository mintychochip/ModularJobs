package net.aincraft.service;

import io.papermc.paper.persistence.PersistentDataContainerView;
import java.util.Optional;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class ItemBoostDataServiceImpl implements ItemBoostDataService {

  private static final NamespacedKey ITEM_BOOST_DATA_KEY = NamespacedKey.fromString(
      "modular_jobs:item_boost_data");

  private final CodecRegistry codecRegistry;

  public ItemBoostDataServiceImpl(CodecRegistry codecRegistry) {
    this.codecRegistry = codecRegistry;
  }

  @Override
  public void addData(SerializableBoostData data, ItemStack stack) {
    byte[] blob = codecRegistry.encode(data);
    stack.editPersistentDataContainer(pdc -> {
      pdc.set(ITEM_BOOST_DATA_KEY,PersistentDataType.BYTE_ARRAY,blob);
    });
  }

  @Override
  public Optional<SerializableBoostData> getData(ItemStack stack) {
    PersistentDataContainerView pdc = stack.getPersistentDataContainer();
    if (!pdc.has(ITEM_BOOST_DATA_KEY)) {
      return Optional.empty();
    }
    byte[] blob = pdc.get(ITEM_BOOST_DATA_KEY, PersistentDataType.BYTE_ARRAY);
    SerializableBoostData decode = codecRegistry.decode(blob, SerializableBoostData.class);
    return Optional.ofNullable(decode);
  }
}
