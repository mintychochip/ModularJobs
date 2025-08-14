package net.aincraft.api.container;

import net.aincraft.api.Bridge;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;

public class PayableTypes {

  private PayableTypes() {
    throw new UnsupportedOperationException();
  }

  public static final PayableType EXPERIENCE = RegistryContainer.registryContainer().getRegistry(
      RegistryKeys.PAYABLE_TYPES).getOrThrow(Key.key("jobs:experience"));

  public static final PayableType ECONOMY = type(context -> {
    Bridge.bridge().economy().deposit(context.getPlayer(), context.getPayable().getAmount());
  }, "economy");

  private static PayableType type(PayableHandler handler, String keyString) {
    return PayableType.create(handler, new NamespacedKey("jobs", keyString));
  }
}
