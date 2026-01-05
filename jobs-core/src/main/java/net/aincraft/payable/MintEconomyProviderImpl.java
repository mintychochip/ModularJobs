package net.aincraft.payable;

import dev.mintychochip.mint.Service;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.PayableAmount;
import org.bukkit.OfflinePlayer;

final class MintEconomyProviderImpl implements EconomyProvider {

  private final Service service;

  MintEconomyProviderImpl(Service service) {
    this.service = service;
  }

  @Override
  public boolean isCurrencySupported() {
    return false;
  }

  @Override
  public boolean deposit(OfflinePlayer player, PayableAmount payableAmount) {
    if (player.getUniqueId() == null) {
      return false;
    }
    try {
      service.deposit(player.getUniqueId(), payableAmount.value()).join();
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
