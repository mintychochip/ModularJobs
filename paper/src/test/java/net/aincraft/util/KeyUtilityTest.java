package net.aincraft.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

/**
 * Pure equality / formatting contracts for key utility records used as map keys.
 */
class KeyUtilityTest {

  /** Verifies {@link LocationKey} equality, hash code, and formatting. */
  @Test
  void locationKeyToStringAndEquality() {
    LocationKey a = new LocationKey("world", 10, 64, -20);
    LocationKey b = new LocationKey("world", 10, 64, -20);
    LocationKey c = new LocationKey("world_nether", 10, 64, -20);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
    assertEquals("world:10,64,-20", a.toString());
  }

  /** Verifies {@link PlayerJobCompositeKey} record equality semantics. */
  @Test
  void playerJobCompositeKeyEquality() {
    UUID player = UUID.fromString("11111111-1111-1111-1111-111111111111");
    Key miner = Key.key("modularjobs", "miner");
    Key fisher = Key.key("modularjobs", "fisherman");

    PlayerJobCompositeKey a = new PlayerJobCompositeKey(player, miner);
    PlayerJobCompositeKey b = new PlayerJobCompositeKey(player, miner);
    PlayerJobCompositeKey otherJob = new PlayerJobCompositeKey(player, fisher);
    final PlayerJobCompositeKey otherPlayer = new PlayerJobCompositeKey(UUID.randomUUID(), miner);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, otherJob);
    assertNotEquals(a, otherPlayer);
    assertEquals(player, a.playerId());
    assertEquals(miner, a.jobKey());
  }
}
