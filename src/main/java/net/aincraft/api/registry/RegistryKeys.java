package net.aincraft.api.registry;

import net.aincraft.api.Job;
import net.aincraft.api.container.PayableHandler;
import net.kyori.adventure.key.Key;

public class RegistryKeys {

  private RegistryKeys() {
    throw new UnsupportedOperationException();
  }

  public static final RegistryKey<Job> JOBS = RegistryKey.key(Key.key("jobs:jobs"));
  public static final RegistryKey<PayableHandler> PAYABLE_HANDLERS = RegistryKey.key(
      Key.key("jobs:payable_types"));
}
