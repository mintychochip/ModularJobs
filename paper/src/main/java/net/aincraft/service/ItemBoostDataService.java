package net.aincraft.service;

import io.papermc.paper.persistence.PersistentDataContainerView;
import java.util.Optional;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.serialization.KryoCodecRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Stores and reads boost data on {@link ItemStack}s via the Bukkit persistent data container.
 *
 * <p>Data is serialized with a {@link KryoCodecRegistry} into a byte array under the namespaced key
 * {@code modular_jobs:item_boost_data}, so boosts survive the item being moved, saved, or reloaded.
 */
public final class ItemBoostDataService {

  private static final NamespacedKey ITEM_BOOST_DATA_KEY = NamespacedKey.fromString(
      "modular_jobs:item_boost_data");

  private final KryoCodecRegistry codecRegistry;

  public ItemBoostDataService(KryoCodecRegistry codecRegistry) {
    this.codecRegistry = codecRegistry;
  }

  /**
   * Serializes {@code data} and embeds it on {@code stack}'s persistent data container.
   *
   * @param data boost data to attach
   * @param stack item to attach the data to
   */
  public void addData(SerializableBoostData data, ItemStack stack) {
    byte[] blob = codecRegistry.encode(data);
    stack.editPersistentDataContainer(pdc -> {
      pdc.set(ITEM_BOOST_DATA_KEY, PersistentDataType.BYTE_ARRAY, blob);
    });
  }

  /**
   * Reads the boost data embedded on {@code stack}, if present.
   *
   * @param stack item to inspect
   * @return the decoded boost data, or empty if the item carries none
   */
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
