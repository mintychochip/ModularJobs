package dev.mintychochip.payable;

import java.util.Locale;
import dev.mintychochip.container.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Selects the optional economy provider used by payable wiring. */
public final class EconomyProviderFactory {

  private static final String MINT_PLUGIN = "Mint";
  private static final String REQUIRED_KEY = "economy.required";
  private static final String MISSING_PROVIDER_KEY = "economy.missing-provider";

  private EconomyProviderFactory() {}

  /**
   * Returns a lazy Mint bridge when the optional plugin and API are available.
   *
   * <p>The bridge resolves Mint's service and READY state at deposit time because Mint registers
   * its service asynchronously during startup. Returns null when the optional integration is not
   * available.
   */
  public static @Nullable EconomyProvider tryCreate(@NotNull Plugin plugin) {
    if (Bukkit.getPluginManager() == null
        || !Bukkit.getPluginManager().isPluginEnabled(MINT_PLUGIN)
        || !MintEconomyProvider.isApiAvailable()) {
      return null;
    }
    return new MintEconomyProvider();
  }

  /**
   * Whether the legacy required flag is enabled.
   *
   * <p>The generated default is false. When true and no explicit
   * {@code economy.missing-provider} policy is configured, the factory preserves the old
   * fail-fast behavior.
   */
  public static boolean isRequired(@NotNull Plugin plugin) {
    return plugin.getConfig().getBoolean(REQUIRED_KEY, false);
  }

  /**
   * Resolves the configured provider, defaulting to a non-throwing blackhole fallback.
   *
   * @throws IllegalStateException when the configured policy is {@code fail} and Mint is absent
   */
  public static @NotNull EconomyProvider createOrFail(@NotNull Plugin plugin) {
    EconomyProvider provider = tryCreate(plugin);
    if (provider != null) {
      return provider;
    }

    return switch (missingProviderPolicy(plugin)) {
      case BLACKHOLE -> new BlackholeEconomyProvider(plugin);
      case FAIL -> throw new IllegalStateException(
          "No economy provider is available. Install Mint, or set "
              + MISSING_PROVIDER_KEY + ": blackhole in config.yml.");
    };
  }

  private static MissingProviderPolicy missingProviderPolicy(@NotNull Plugin plugin) {
    String configured = plugin.getConfig().contains(MISSING_PROVIDER_KEY, true)
        ? plugin.getConfig().getString(MISSING_PROVIDER_KEY)
        : null;
    if (configured != null) {
      return switch (configured.trim().toLowerCase(Locale.ROOT)) {
        case "blackhole" -> MissingProviderPolicy.BLACKHOLE;
        case "fail" -> MissingProviderPolicy.FAIL;
        default -> throw new IllegalArgumentException(
            "Unknown " + MISSING_PROVIDER_KEY + " value '" + configured
                + "'; expected blackhole or fail.");
      };
    }
    return isRequired(plugin)
        ? MissingProviderPolicy.FAIL
        : MissingProviderPolicy.BLACKHOLE;
  }

  private enum MissingProviderPolicy {
    BLACKHOLE,
    FAIL
  }
}
