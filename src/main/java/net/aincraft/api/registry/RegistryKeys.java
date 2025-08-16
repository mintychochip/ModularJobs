package net.aincraft.api.registry;

import net.aincraft.api.Job;
import net.aincraft.api.action.ActionType;
import net.aincraft.api.container.BoostSource;
import net.aincraft.api.container.PayableType;
import net.aincraft.api.container.boost.Condition.Codec;
import net.kyori.adventure.key.Key;

public class RegistryKeys {

  private RegistryKeys() {
    throw new UnsupportedOperationException();
  }

  public static final RegistryKey<Codec> CONDITION_CODEC = RegistryKey.key(Key.key("jobs:codecs"));
  public static final RegistryKey<Job> JOBS = RegistryKey.key(Key.key("jobs:jobs"));
  public static final RegistryKey<PayableType> PAYABLE_TYPES = RegistryKey.key(
      Key.key("jobs:payable_types"));
  public static final RegistryKey<ActionType> ACTION_TYPES = RegistryKey.key(
      Key.key("jobs:action_types"));
  public static final RegistryKey<BoostSource> TRANSIENT_BOOST_SOURCES = RegistryKey.key(
      Key.key("jobs:boost_sources"));
}
