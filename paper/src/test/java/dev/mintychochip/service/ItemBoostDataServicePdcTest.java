package dev.mintychochip.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.databag.gson.GsonConditionSerializer;
import dev.mintychochip.databag.paper.PersistentBags;
import dev.mintychochip.databag.DataBag;
import dev.mintychochip.databag.FormattedBytes;
import dev.mintychochip.boost.BoostPayloadHandler;
import java.math.BigDecimal;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import dev.mintychochip.boost.BoostDataCodec;
import dev.mintychochip.boost.BoostFactoryImpl;
import dev.mintychochip.boost.MultiplicativeBoostImpl;
import dev.mintychochip.boost.RuledBoostSourceImpl;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import dev.mintychochip.container.boost.RuledBoostSource.Rule;
import dev.mintychochip.test.MockBukkitSupport;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives the shipped item embed path: bag Kryo bytes sit on PDC {@code BYTE_ARRAY}.
 */
class ItemBoostDataServicePdcTest {

  private ItemBoostDataService service;
  private BoostDataCodec codec;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    codec = new BoostDataCodec(GsonConditionSerializer.gson(), BoostFactoryImpl.INSTANCE);
    service = new ItemBoostDataService(codec);
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void pdcByteArrayDecodesToSameBoostPayload() {
    SerializableBoostData data = sampleData();
    ItemStack stack = new ItemStack(Material.DIAMOND_HELMET);
    service.addData(data, stack);

    byte[] tag = stack.getPersistentDataContainer()
        .get(ItemBoostDataService.ITEM_BOOST_DATA_KEY, PersistentDataType.BYTE_ARRAY);
    assertNotNull(tag);
    assertTrue(tag.length > 0);

    DataBag bag = DataBag.fromBytes(tag);
    FormattedBytes payload = bag.getFormatted(ItemBoostDataService.BOOST_PAYLOAD_KEY).orElseThrow();
    assertEquals(ItemBoostDataService.BOOST_PAYLOAD_FORMAT, payload.format());
    assertArrayEquals(codec.write(data), payload.value());
    assertArrayEquals(codec.write(data), bag.get(BoostPayloadHandler.INSTANCE).orElseThrow());

    Optional<SerializableBoostData> roundTrip = service.getData(stack);
    assertTrue(roundTrip.isPresent());
    assertArrayEquals(payload.value(), codec.write(roundTrip.get()));
  }

  @Test
  void stillReadsUnformattedBagBytes() {
    SerializableBoostData data = sampleData();
    ItemStack stack = new ItemStack(Material.DIAMOND_HELMET);
    DataBag legacy = DataBag.create()
        .setBytes(ItemBoostDataService.BOOST_PAYLOAD_KEY, codec.write(data));
    PersistentBags.write(stack, ItemBoostDataService.ITEM_BOOST_DATA_KEY, legacy);

    Optional<SerializableBoostData> roundTrip = service.getData(stack);
    assertTrue(roundTrip.isPresent());
    assertArrayEquals(codec.write(data), codec.write(roundTrip.get()));
  }

  @Test
  void stillReadsRawJsonBlob() {
    SerializableBoostData data = sampleData();
    ItemStack stack = new ItemStack(Material.DIAMOND_HELMET);
    byte[] json = codec.write(data);
    stack.editPersistentDataContainer(pdc ->
        pdc.set(ItemBoostDataService.ITEM_BOOST_DATA_KEY, PersistentDataType.BYTE_ARRAY, json));

    Optional<SerializableBoostData> roundTrip = service.getData(stack);
    assertTrue(roundTrip.isPresent());
    assertArrayEquals(json, codec.write(roundTrip.get()));
  }

  private static SerializableBoostData sampleData() {
    return new PassiveBoostData(
        new RuledBoostSourceImpl(
            List.of(new Rule(
                BoostFactoryImpl.INSTANCE.sneaking(true),
                10,
                new MultiplicativeBoostImpl(new BigDecimal("1.25")))),
            Key.key("modularjobs", "mining_helmet"),
            "helmet"),
        new BitSet());
  }
}
