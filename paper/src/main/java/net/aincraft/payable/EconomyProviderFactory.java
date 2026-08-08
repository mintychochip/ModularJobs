package net.aincraft.payable;

import java.util.logging.Logger;
import net.aincraft.container.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Selects an {@link EconomyProvider} for payable wiring. Package-visible selection logic is unit
 * tested without a live Mint server.
 */
public final class EconomyProviderFactory {

  private static final Logger LOGGER = Logger.getLogger(EconomyProviderFactory.class.getName());

  private EconomyProviderFactory() {}

  /**
   * Returns a lazy Mint bridge whenever the Mint plugin is enabled (the ledger service may still be
   * booting; {@link MintEconomyProvider} resolves the {@code Mint} service and checks READY at
   * deposit time). Returns null when Mint is not present or disabled.
   */
  public static @Nullable EconomyProvider tryCreate(@NotNull Plugin plugin) {
    if (Bukkit.getPluginManager() == null
        || !Bukkit.getPluginManager().isPluginEnabled("Mint")) {
      return null;
    }
    return new MintEconomyProvider();
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
   * @throws IllegalStateException when required and no provider is available (Mint not installed /
   *     disabled)
   */
  public static @Nullable EconomyProvider createOrFail(@NotNull Plugin plugin) {
    EconomyProvider provider = tryCreate(plugin);
    if (provider != null) {
      return provider;
    }
    if (isRequired(plugin)) {
      throw new IllegalStateException(
          "No economy provider available (install the Mint plugin), "
              + "or set economy.required: false in config.yml for experience-only servers");
    }
    LOGGER.severe(
        "No economy provider available — modularjobs:economy payables will refuse deposits. "
            + "Install the Mint plugin, or leave economy.required false.");
    return null;
  }
}
