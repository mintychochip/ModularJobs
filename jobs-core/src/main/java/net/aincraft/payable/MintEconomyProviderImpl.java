package net.aincraft.payable;

import dev.mintychochip.mint.Service;
import dev.mintychochip.mint.ServiceHolder;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.PayableAmount;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

final class MintEconomyProviderImpl implements EconomyProvider {

  private static final Logger LOGGER = Logger.getLogger(MintEconomyProviderImpl.class.getName());
  private final ServiceHolder<Service> mintService;
  private boolean checkedService = false;

  MintEconomyProviderImpl(ServiceHolder<Service> mintService) {
    this.mintService = mintService;
  }

  @Override
  public boolean isCurrencySupported() {
    return false;
  }

  @Override
  public boolean deposit(OfflinePlayer player, PayableAmount payableAmount) {
    BigDecimal amount = payableAmount.value();
    LOGGER.info("Attempting to deposit " + amount + " to player " + player.getName());
    try {
      // Just deposit directly - let Choco handle account creation like /eco give does
      BigDecimal result = mintService.get().deposit(player.getUniqueId(), amount).join();
      LOGGER.info("Deposit result: " + result);

      if (result != null && result.compareTo(BigDecimal.ZERO) > 0) {
        // Verify the deposit actually worked
        BigDecimal actualBalance = mintService.get().getBalanceByHolder(player.getUniqueId()).join();
        LOGGER.info("Verified balance for " + player.getName() + ": " + actualBalance);
        return true;
      } else {
        LOGGER.warning("Deposit failed - returned: " + result);
        return false;
      }
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      LOGGER.log(Level.SEVERE, "Failed to deposit to player " + player.getName() + ": " + e.getMessage(), cause);
      return false;
    }
  }
}
