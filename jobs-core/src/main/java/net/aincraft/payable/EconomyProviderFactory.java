package net.aincraft.payable;

import java.util.logging.Logger;
import net.aincraft.container.EconomyProvider;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Selects an {@link EconomyProvider} for payable wiring. Package-visible selection logic is unit
 * tested without a live Vault server.
 */
public final class EconomyProviderFactory {

  private static final Logger LOGGER = Logger.getLogger(EconomyProviderFactory.class.getName());

  private EconomyProviderFactory() {}

  /**
   * Try optional bridges in order (Vault first). Returns null when none available.
   */
  public static @Nullable EconomyProvider tryCreate(@NotNull Plugin plugin) {
    EconomyProvider vault = VaultEconomyProvider.tryCreate(plugin);
    if (vault != null) {
      return vault;
    }
    return null;
  }

  /**
   * Whether enable must fail when no provider is available.
   * Config key: {@code economy.required} (default {@code true}).
   */
  public static boolean isRequired(@NotNull Plugin plugin) {
    return plugin.getConfig().getBoolean("economy.required", true);
  }

  /**
   * Resolve provider or hard-fail when economy is required.
   *
   * @throws IllegalStateException when required and no provider is available
   */
  public static @Nullable EconomyProvider createOrFail(@NotNull Plugin plugin) {
    EconomyProvider provider = tryCreate(plugin);
    if (provider != null) {
      return provider;
    }
    if (isRequired(plugin)) {
      throw new IllegalStateException(
          "No economy provider available (install Vault + an economy plugin), "
              + "or set economy.required: false in config.yml for experience-only servers");
    }
    LOGGER.severe(
        "No economy provider available — modularjobs:economy payables will refuse deposits. "
            + "Install Vault + an economy plugin, or leave economy.required false.");
    return null;
  }
}
