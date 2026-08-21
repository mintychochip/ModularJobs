package dev.mintychochip.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

/** Drives shipped {@link RegistryKeys} constants and {@link RegistryKey#key} factory. */
class RegistryKeysTest {

  @Test
  void wellKnownKeysHaveStableIdentifiers() {
    assertEquals(Key.key("jobs", "jobs"), RegistryKeys.JOBS.key());
    assertEquals(Key.key("jobs", "payable_types"), RegistryKeys.PAYABLE_TYPES.key());
    assertEquals(Key.key("jobs", "action_types"), RegistryKeys.ACTION_TYPES.key());
    assertEquals(Key.key("jobs", "boost_sources"), RegistryKeys.TRANSIENT_BOOST_SOURCES.key());
  }

  @Test
  void registryKeysAreDistinct() {
    assertNotEquals(RegistryKeys.JOBS.key(), RegistryKeys.ACTION_TYPES.key());
    assertNotEquals(RegistryKeys.PAYABLE_TYPES.key(), RegistryKeys.TRANSIENT_BOOST_SOURCES.key());
  }

  @Test
  void factoryCreatesKeyedInstance() {
    Key custom = Key.key("modularjobs", "custom_registry");
    RegistryKey<String> key = RegistryKey.key(custom);
    assertNotNull(key);
    assertEquals(custom, key.key());
  }

  @Test
  @SuppressWarnings({"PMD.AvoidAccessibilityAlteration", "PMD.PreserveStackTrace"})
  void utilityConstructorIsBlocked() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          var ctor = RegistryKeys.class.getDeclaredConstructor();
          ctor.setAccessible(true);
          try {
            ctor.newInstance();
          } catch (ReflectiveOperationException e) {
            if (e.getCause() instanceof UnsupportedOperationException uoe) {
              throw uoe;
            }
            throw new RuntimeException(e);
          }
        });
  }
}
