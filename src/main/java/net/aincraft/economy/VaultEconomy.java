package net.aincraft.economy;

import java.math.BigDecimal;
import org.bukkit.OfflinePlayer;

public final class VaultEconomy implements Economy {

  private final net.milkbowl.vault.economy.Economy vaultEconomy;

  public VaultEconomy(net.milkbowl.vault.economy.Economy vaultEconomy) {
    this.vaultEconomy = vaultEconomy;
  }

  @Override
  public boolean deposit(OfflinePlayer player, BigDecimal amount) {
    return true;
  }

  @Override
  public BigDecimal getBalance(OfflinePlayer player) {
    return null;
  }

  @Override
  public boolean withdraw(OfflinePlayer player, BigDecimal money) {
    return false;
  }
}
