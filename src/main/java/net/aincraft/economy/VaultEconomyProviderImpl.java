package net.aincraft.economy;

import java.math.BigDecimal;
import net.aincraft.api.container.PayableAmount;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

public final class VaultEconomyProviderImpl implements EconomyProvider {

  private final Economy vault;

  public VaultEconomyProviderImpl(Economy vault) {
    this.vault = vault;
  }

  @Override
  public boolean deposit(OfflinePlayer player, PayableAmount currencyPayableAmount) {
    BigDecimal amount = currencyPayableAmount.amount();
    EconomyResponse economyResponse = vault.depositPlayer(player, amount.longValue());
    return economyResponse.transactionSuccess();
  }
}
