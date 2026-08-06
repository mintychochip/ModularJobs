package net.aincraft.container;

import net.aincraft.registry.RegistryContainer;
import net.aincraft.registry.RegistryKeys;
import org.bukkit.NamespacedKey;

public class PayableTypes {

  private PayableTypes() {
    throw new UnsupportedOperationException();
  }

  public static final PayableType EXPERIENCE = type("experience");

  public static final PayableType ECONOMY = type("economy");

  private static PayableType type(String keyString) {
    return RegistryContainer.registryContainer().getRegistry(
        RegistryKeys.PAYABLE_TYPES).getOrThrow(new NamespacedKey("modularjobs",keyString));
  }
}
