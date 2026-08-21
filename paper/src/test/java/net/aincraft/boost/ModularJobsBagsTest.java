package net.aincraft.boost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.databag.DataBag;
import dev.mintychochip.databag.DataHandlers;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ModularJobsBagsTest {

  @Test
  void extrasCarryJobKeyAndLevel() {
    DataBag bag = ModularJobsBags.extras("modularjobs:miner", 12);
    assertEquals(Optional.of("modularjobs:miner"), bag.getString(ModularJobsBags.JOB));
    assertEquals(OptionalInt.of(12), bag.getInt(ModularJobsBags.JOB_LEVEL));
  }

  @Test
  void extrasWithoutJobAreEmpty() {
    DataBag bag = ModularJobsBags.extras(null, 99);
    assertFalse(bag.has(ModularJobsBags.JOB));
    assertTrue(bag.getInt(ModularJobsBags.JOB_LEVEL).isEmpty());
  }

  @Test
  void registerExposesBoostPayloadHandler() {
    ModularJobsBags.register();
    assertEquals(
        Optional.of(BoostPayloadHandler.INSTANCE),
        DataHandlers.get(BoostPayloadHandler.KEY));
  }
}
