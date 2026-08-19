package net.aincraft.service;

import dev.conditions.paper.PersistentBags;
import dev.databag.DataBag;
import java.util.Optional;
import net.aincraft.boost.BoostDataCodec;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Stores and reads boost data on {@link ItemStack}s via the Bukkit persistent data container.
 *
 * <p>The PDC tag is a Kryo {@link DataBag} {@code BYTE_ARRAY}. Boost JSON lives in the bag
 * as a {@code byte[]} primitive (condition graphs stay {@link dev.conditions.ConditionSerializer}
 * bytes, not Kryo condition classes).
 */
public final class ItemBoostDataService {

  static final NamespacedKey ITEM_BOOST_DATA_KEY = NamespacedKey.fromString(
      "modular_jobs:item_boost_data");

  /** Bag key for the {@link SerializableBoostData} JSON payload. */
  public static final Key BOOST_PAYLOAD_KEY = Key.key("modularjobs", "boost_data");

  private final BoostDataCodec codec;

  public ItemBoostDataService(BoostDataCodec codec) {
    this.codec = codec;
  }

  /**
   * Serializes {@code data} into a primitive bag and embeds the bag bytes on {@code stack}.
   *
   * @param data boost data to attach
   * @param stack item to attach the data to
   */
  public void addData(SerializableBoostData data, ItemStack stack) {
    DataBag bag = DataBag.create().setBytes(BOOST_PAYLOAD_KEY, codec.write(data));
    PersistentBags.write(stack, ITEM_BOOST_DATA_KEY, bag);
  }

  /**
   * Reads the boost data embedded on {@code stack}, if present.
   *
   * @param stack item to inspect
   * @return the decoded boost data, or empty if the item carries none
   */
  public Optional<SerializableBoostData> getData(ItemStack stack) {
    var pdc = stack.getPersistentDataContainer();
    if (!pdc.has(ITEM_BOOST_DATA_KEY, PersistentDataType.BYTE_ARRAY)) {
      return Optional.empty();
    }
    byte[] blob = pdc.get(ITEM_BOOST_DATA_KEY, PersistentDataType.BYTE_ARRAY);
    if (blob == null || blob.length == 0) {
      return Optional.empty();
    }
    try {
      DataBag bag = DataBag.fromBytes(blob);
      Optional<byte[]> payload = bag.getBytes(BOOST_PAYLOAD_KEY);
      if (payload.isPresent()) {
        return Optional.of(codec.read(payload.get()));
      }
    } catch (RuntimeException ignored) {
      // fall through to legacy raw JSON blob
    }
    try {
      return Optional.of(codec.read(blob));
    } catch (RuntimeException ignored) {
      return Optional.empty();
    }
  }
}
