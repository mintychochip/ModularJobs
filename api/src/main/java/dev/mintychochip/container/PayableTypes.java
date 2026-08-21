package dev.mintychochip.container;

import dev.mintychochip.registry.RegistryContainer;
import dev.mintychochip.registry.RegistryKeys;
import net.kyori.adventure.key.Key;

/**
 * Well-known {@link PayableType} instances resolved from the payable-type
 * registry.
 *
 * <p>This utility class is not instantiable. The constants reference the
 * registry entries keyed under the {@code modularjobs} namespace; accessing
 * them requires the payable-type registry to have been populated at runtime.</p>
 */
public class PayableTypes {

  /**
   * Private constructor to prevent instantiation.
   */
  private PayableTypes() {
    throw new UnsupportedOperationException();
  }

  /**
   * The experience-based payable type.
   */
  public static final PayableType EXPERIENCE = type("experience");

  /**
   * The economy (currency) based payable type.
   */
  public static final PayableType ECONOMY = type("economy");

  /**
   * Looks up a payable type in the registry by its {@code modularjobs} key.
   *
   * @param keyString the key name without namespace
   * @return the registered payable type
   */
  private static PayableType type(String keyString) {
    return RegistryContainer.registryContainer().getRegistry(
        RegistryKeys.PAYABLE_TYPES).getOrThrow(Key.key("modularjobs", keyString));
  }
}
