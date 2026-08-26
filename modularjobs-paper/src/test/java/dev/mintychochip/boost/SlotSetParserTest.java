package dev.mintychochip.boost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import org.junit.jupiter.api.Test;

/** Drives shipped {@link SlotSetParser#parse} named sets, ranges, and lists. */
class SlotSetParserTest {

  @Test
  void parseHotbarIsSlots0Through8() {
    BitSet bits = SlotSetParser.parse("hotbar");
    for (int i = 0; i <= 8; i++) {
      assertTrue(bits.get(i), "hotbar must include slot " + i);
    }
    assertFalse(bits.get(9));
    assertEquals(9, bits.cardinality());
  }

  @Test
  void parseInventoryIsSlots9Through35() {
    BitSet bits = SlotSetParser.parse("inventory");
    assertFalse(bits.get(8));
    for (int i = 9; i <= 35; i++) {
      assertTrue(bits.get(i), "inventory must include slot " + i);
    }
    assertFalse(bits.get(36));
    assertEquals(27, bits.cardinality());
  }

  @Test
  void parseArmorAndPieces() {
    BitSet armor = SlotSetParser.parse("armor");
    for (int i = 36; i <= 39; i++) {
      assertTrue(armor.get(i));
    }
    assertEquals(4, armor.cardinality());

    assertTrue(SlotSetParser.parse("helmet").get(39));
    assertTrue(SlotSetParser.parse("chestplate").get(38));
    assertTrue(SlotSetParser.parse("leggings").get(37));
    assertTrue(SlotSetParser.parse("boots").get(36));
    assertTrue(SlotSetParser.parse("offhand").get(40));
  }

  @Test
  void parseAllIs0Through40() {
    BitSet all = SlotSetParser.parse("all");
    assertEquals(41, all.cardinality());
    assertTrue(all.get(0));
    assertTrue(all.get(40));
  }

  @Test
  void parseMainhandIsHotbarRange() {
    BitSet main = SlotSetParser.parse("mainhand");
    for (int i = 0; i <= 8; i++) {
      assertTrue(main.get(i));
    }
    assertEquals(9, main.cardinality());
  }

  @Test
  void parseSingleSlotAndRange() {
    BitSet single = SlotSetParser.parse("5");
    assertTrue(single.get(5));
    assertEquals(1, single.cardinality());

    BitSet range = SlotSetParser.parse("0-8");
    assertEquals(9, range.cardinality());
    assertTrue(range.get(0));
    assertTrue(range.get(8));
  }

  @Test
  void parseCommaSeparatedAndMixedRanges() {
    BitSet bits = SlotSetParser.parse("0,9,36");
    assertTrue(bits.get(0));
    assertTrue(bits.get(9));
    assertTrue(bits.get(36));
    assertEquals(3, bits.cardinality());

    BitSet mixed = SlotSetParser.parse("0-2,10,20-21");
    assertTrue(mixed.get(0));
    assertTrue(mixed.get(1));
    assertTrue(mixed.get(2));
    assertTrue(mixed.get(10));
    assertTrue(mixed.get(20));
    assertTrue(mixed.get(21));
    assertEquals(6, mixed.cardinality());
  }

  @Test
  void parseIsCaseInsensitiveForNamedSets() {
    BitSet a = SlotSetParser.parse("HOTBAR");
    BitSet b = SlotSetParser.parse("HotBar");
    assertEquals(a, b);
    assertEquals(9, a.cardinality());
  }

  @Test
  void parseRejectsNullBlankAndInvalid() {
    assertThrows(IllegalArgumentException.class, () -> SlotSetParser.parse(null));
    assertThrows(IllegalArgumentException.class, () -> SlotSetParser.parse(""));
    assertThrows(IllegalArgumentException.class, () -> SlotSetParser.parse("   "));
    assertThrows(IllegalArgumentException.class, () -> SlotSetParser.parse("not-a-slot"));
    assertThrows(IllegalArgumentException.class, () -> SlotSetParser.parse("99"));
    assertThrows(IllegalArgumentException.class, () -> SlotSetParser.parse("8-0"));
  }
}
