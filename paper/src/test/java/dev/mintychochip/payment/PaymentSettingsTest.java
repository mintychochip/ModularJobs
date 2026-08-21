package dev.mintychochip.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaymentSettingsTest {

  @Test
  void defaultsMatchPriorHardcodedValues() {
    PaymentSettings settings = PaymentSettings.defaults();
    assertTrue(settings.payInCreative());
    assertFalse(settings.payWhileRiding());
    assertTrue(settings.disabledWorlds().isEmpty());
    assertEquals(0.5, settings.killContributionCutoff(), 1e-9);
    assertEquals(25.0, settings.furnaceMaxDistance(), 1e-9);
    assertEquals(625.0, settings.furnaceMaxDistanceSquared(), 1e-9);
  }

  @Test
  void worldDisabledIsCaseInsensitive() {
    PaymentSettings settings = new PaymentSettings(
        true, false, java.util.Set.of("world_nether"), 0.5, 25.0);
    assertTrue(settings.isWorldDisabled("world_nether"));
    assertTrue(settings.isWorldDisabled("WORLD_NETHER"));
    assertFalse(settings.isWorldDisabled("world"));
  }
}
