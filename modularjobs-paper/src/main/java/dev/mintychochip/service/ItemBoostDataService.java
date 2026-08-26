package dev.mintychochip.service;

import dev.mintychochip.boost.BoostDataCodec;
import dev.mintychochip.boost.BoostPayloadHandler;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData;
import dev.mintychochip.databag.DataBag;
import dev.mintychochip.databag.FormattedBytes;
import dev.mintychochip.databag.UnknownBagFormatException;
import dev.mintychochip.databag.paper.PersistentBags;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Stores and reads boost data on {@link ItemStack}s via the Bukkit persistent data container.
 *
 * <p>The PDC tag is a Kryo {@link DataBag} {@code BYTE_ARRAY}. Boost JSON lives in the bag as
 * formatted bytes ({@link #BOOST_PAYLOAD_FORMAT}) so later encodings can migrate. Condition graphs
 * stay {@link dev.mintychochip.databag.ConditionSerializer} bytes, not Kryo condition classes.
 */
public final class ItemBoostDataService {

  static final NamespacedKey ITEM_BOOST_DATA_KEY =
      NamespacedKey.fromString("modular_jobs:item_boost_data");

  /** Bag key for the {@link SerializableBoostData} JSON payload. */
  public static final Key BOOST_PAYLOAD_KEY = BoostPayloadHandler.KEY;

  /** Format id for {@link #BOOST_PAYLOAD_KEY} UTF-8 JSON (`SerializableBoostData`). */
  public static final int BOOST_PAYLOAD_FORMAT = BoostPayloadHandler.FORMAT;

  private final BoostDataCodec codec;

  /** Item boost data service. */
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
    DataBag bag = DataBag.create().set(BoostPayloadHandler.INSTANCE, codec.write(data));
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
    if (looksLikeJson(blob)) {
      return readRawJson(blob);
    }
    if (DataBag.isVersioned(blob)) {
      try {
        return readBagPayload(DataBag.fromBytes(blob));
      } catch (UnknownBagFormatException | IllegalStateException ignored) {
        return Optional.empty();
      }
    }
    try {
      Optional<SerializableBoostData> fromBag = readBagPayload(DataBag.fromBytes(blob));
      if (fromBag.isPresent()) {
        return fromBag;
      }
    } catch (IllegalArgumentException | IllegalStateException ignored) {
      // fall through to legacy raw JSON blob
    }
    return readRawJson(blob);
  }

  private Optional<SerializableBoostData> readRawJson(byte[] blob) {
    try {
      return Optional.of(codec.read(blob));
    } catch (IllegalArgumentException
        | IllegalStateException
        | java.time.format.DateTimeParseException ignored) {
      return Optional.empty();
    }
  }

  private static boolean looksLikeJson(byte[] blob) {
    for (byte value : blob) {
      if (!Character.isWhitespace(value)) {
        return value == '{' || value == '[';
      }
    }
    return false;
  }

  private Optional<SerializableBoostData> readBagPayload(DataBag bag) {
    Optional<byte[]> handled = bag.get(BoostPayloadHandler.INSTANCE);
    if (handled.isPresent()) {
      return Optional.of(codec.read(handled.get()));
    }
    Optional<FormattedBytes> formatted = bag.getFormatted(BOOST_PAYLOAD_KEY);
    if (formatted.isPresent()) {
      FormattedBytes payload = formatted.get();
      if (payload.format() != BOOST_PAYLOAD_FORMAT) {
        return Optional.empty();
      }
      return Optional.of(codec.read(payload.value()));
    }
    Optional<byte[]> raw = bag.getBytes(BOOST_PAYLOAD_KEY);
    if (raw.isPresent()) {
      return Optional.of(codec.read(raw.get()));
    }
    return Optional.empty();
  }
}
