package net.aincraft.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.conditions.gson.GsonConditionSerializer;
import dev.databag.DataBag;
import java.math.BigDecimal;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import net.aincraft.boost.BoostDataCodec;
import net.aincraft.boost.BoostFactoryImpl;
import net.aincraft.boost.MultiplicativeBoostImpl;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.test.MockBukkitSupport;
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
    SerializableBoostData data = new PassiveBoostData(
        new RuledBoostSourceImpl(
            List.of(new Rule(
                BoostFactoryImpl.INSTANCE.sneaking(true),
                10,
                new MultiplicativeBoostImpl(new BigDecimal("1.25")))),
            Key.key("modularjobs", "mining_helmet"),
            "helmet"),
        new BitSet());
    ItemStack stack = new ItemStack(Material.DIAMOND_HELMET);
    service.addData(data, stack);

    byte[] tag = stack.getPersistentDataContainer()
        .get(ItemBoostDataService.ITEM_BOOST_DATA_KEY, PersistentDataType.BYTE_ARRAY);
    assertNotNull(tag);
    assertTrue(tag.length > 0);

    DataBag bag = DataBag.fromBytes(tag);
    byte[] payload = bag.getBytes(ItemBoostDataService.BOOST_PAYLOAD_KEY).orElseThrow();
    assertArrayEquals(codec.write(data), payload);

    Optional<SerializableBoostData> roundTrip = service.getData(stack);
    assertTrue(roundTrip.isPresent());
    assertArrayEquals(payload, codec.write(roundTrip.get()));
  }
}
