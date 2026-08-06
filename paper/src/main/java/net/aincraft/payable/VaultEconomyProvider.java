package net.aincraft.payable;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.PayableAmount;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

/**
 * Vault economy bridge via reflection so Vault remains an optional soft-depend (no compile-time
 * Vault API dependency required).
 */
public final class VaultEconomyProvider implements EconomyProvider {

  private static final Logger LOGGER = Logger.getLogger(VaultEconomyProvider.class.getName());
  private static final String VAULT_ECONOMY = "net.milkbowl.vault.economy.Economy";

  private final Object vaultEconomy;
  private final Method depositPlayer;
  private final Method hasAccount;
  private final Method createPlayerAccount;

  private VaultEconomyProvider(Object vaultEconomy, Method depositPlayer, Method hasAccount,
      Method createPlayerAccount) {
    this.vaultEconomy = vaultEconomy;
    this.depositPlayer = depositPlayer;
    this.hasAccount = hasAccount;
    this.createPlayerAccount = createPlayerAccount;
  }

  /**
   * @return a provider when Vault plugin is enabled and an Economy service is registered; else null
   */
  public static @Nullable EconomyProvider tryCreate(@Nullable Plugin plugin) {
    if (Bukkit.getPluginManager() == null
        || !Bukkit.getPluginManager().isPluginEnabled("Vault")) {
      return null;
    }
    try {
      Class<?> economyClass = Class.forName(VAULT_ECONOMY);
      RegisteredServiceProvider<?> registration =
          Bukkit.getServicesManager().getRegistration(economyClass);
      if (registration == null) {
        LOGGER.warning("Vault is enabled but no Economy service is registered");
        return null;
      }
      Object economy = registration.getProvider();
      Method deposit = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
      Method hasAccount = economyClass.getMethod("hasAccount", OfflinePlayer.class);
      Method createAccount = economyClass.getMethod("createPlayerAccount", OfflinePlayer.class);
      if (plugin != null) {
        plugin.getSLF4JLogger().info("Using Vault economy provider: {}",
            economy.getClass().getName());
      }
      return new VaultEconomyProvider(economy, deposit, hasAccount, createAccount);
    } catch (ClassNotFoundException e) {
      LOGGER.warning("Vault plugin present but Vault API classes not on classpath");
      return null;
    } catch (NoSuchMethodException e) {
      LOGGER.log(Level.SEVERE, "Vault Economy API methods missing", e);
      return null;
    }
  }

  @Override
  public boolean isCurrencySupported() {
    return false;
  }

  @Override
  public boolean deposit(OfflinePlayer player, PayableAmount payableAmount) {
    BigDecimal amount = payableAmount.value();
    if (amount == null || amount.signum() <= 0) {
      return false;
    }
    try {
      Boolean has = (Boolean) hasAccount.invoke(vaultEconomy, player);
      if (has == null || !has) {
        createPlayerAccount.invoke(vaultEconomy, player);
      }
      Object response = depositPlayer.invoke(vaultEconomy, player, amount.doubleValue());
      // EconomyResponse has transactionSuccess()
      if (response != null) {
        Method success = response.getClass().getMethod("transactionSuccess");
        Object ok = success.invoke(response);
        return Boolean.TRUE.equals(ok);
      }
      return true;
    } catch (ReflectiveOperationException e) {
      LOGGER.log(Level.SEVERE, "Vault deposit failed for " + player.getUniqueId(), e);
      return false;
    }
  }
}
