package net.aincraft.container;

import java.util.UUID;

public interface EconomyProvider {

  boolean isCurrencySupported();

  boolean deposit(UUID playerId, PayableAmount payableAmount);
}
