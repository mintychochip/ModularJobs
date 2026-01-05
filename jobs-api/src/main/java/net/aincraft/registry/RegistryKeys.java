package net.aincraft.registry;

import net.aincraft.Job;
import net.aincraft.container.ActionType;
import net.aincraft.container.BoostSource;
import net.aincraft.container.PayableType;
import net.kyori.adventure.key.Key;

public class RegistryKeys {

  private RegistryKeys() {
    throw new UnsupportedOperationException();
  }

  public static final RegistryKey<Job> JOBS = RegistryKey.key(Key.key("jobs:jobs"));
  public static final RegistryKey<PayableType> PAYABLE_TYPES = RegistryKey.key(
      Key.key("jobs:payable_types"));
  public static final RegistryKey<ActionType> ACTION_TYPES = RegistryKey.key(
      Key.key("jobs:action_types"));
  public static final RegistryKey<BoostSource> TRANSIENT_BOOST_SOURCES = RegistryKey.key(
      Key.key("jobs:boost_sources"));
}
