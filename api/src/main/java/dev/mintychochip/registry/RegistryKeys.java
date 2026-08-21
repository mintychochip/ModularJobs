package dev.mintychochip.registry;

import dev.mintychochip.Job;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.PayableType;
import net.kyori.adventure.key.Key;

/**
 * Holds the predefined {@link RegistryKey} constants used to address the plugin's
 * built-in registries.
 */
public class RegistryKeys {

  private RegistryKeys() {
    throw new UnsupportedOperationException();
  }

  /** Key of the registry of available {@link Job}s. */
  public static final RegistryKey<Job> JOBS = RegistryKey.key(Key.key("jobs:jobs"));
  /** Key of the registry of {@link PayableType}s. */
  public static final RegistryKey<PayableType> PAYABLE_TYPES = RegistryKey.key(
      Key.key("jobs:payable_types"));
  /** Key of the registry of {@link ActionType}s. */
  public static final RegistryKey<ActionType> ACTION_TYPES = RegistryKey.key(
      Key.key("jobs:action_types"));
  /** Key of the registry of transient {@link BoostSource}s. */
  public static final RegistryKey<BoostSource> TRANSIENT_BOOST_SOURCES = RegistryKey.key(
      Key.key("jobs:boost_sources"));
}
