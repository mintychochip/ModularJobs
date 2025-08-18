package net.aincraft.economy;

import java.math.BigDecimal;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.PayableAmount;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

final class VaultEconomyProviderImpl implements EconomyProvider {

  private final Economy vault;

  VaultEconomyProviderImpl(Economy vault) {
    this.vault = vault;
  }

  @Override
  public boolean deposit(OfflinePlayer player, PayableAmount currencyPayableAmount) {
    BigDecimal amount = currencyPayableAmount.amount();
    EconomyResponse economyResponse = vault.depositPlayer(player, amount.longValue());
    return economyResponse.transactionSuccess();
  }
}
