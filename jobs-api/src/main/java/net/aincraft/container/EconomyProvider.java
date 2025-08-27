package net.aincraft.container;

import com.google.common.base.Preconditions;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public interface EconomyProvider {

  boolean isCurrencySupported();

  boolean deposit(OfflinePlayer player, PayableAmount payableAmount);
}
