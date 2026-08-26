package dev.mintychochip.payable;

import dev.mintychochip.container.EconomyProvider;
import dev.mintychochip.container.PayableAmount;
import java.math.BigDecimal;
import java.util.UUID;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Safe fallback for servers that do not install a currency provider.
 *
 * <p>Positive currency rewards are accepted and intentionally discarded. This keeps the payable
 * pipeline usable for experience-only or development servers without pretending that a balance was
 * credited.
 */
public final class BlackholeEconomyProvider implements EconomyProvider {

  /**
   * Creates the fallback provider, logging one warning that positive currency rewards will be
   * discarded.
   */
  public BlackholeEconomyProvider(@NotNull Plugin plugin) {
    plugin
        .getLogger()
        .warning(
            "No economy provider is available; positive modularjobs:economy rewards will be"
                + " discarded. Install Mint or set economy.missing-provider: fail if currency"
                + " rewards are mandatory.");
  }

  @Override
  public boolean isCurrencySupported() {
    return true;
  }

  @Override
  public boolean deposit(UUID playerId, PayableAmount payableAmount) {
    if (payableAmount == null) {
      return false;
    }
    BigDecimal amount = payableAmount.value();
    return amount != null && amount.signum() > 0;
  }
}
