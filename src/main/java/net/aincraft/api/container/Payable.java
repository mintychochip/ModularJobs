package net.aincraft.api.container;

import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface Payable {
  PayableType getType();
  PayableAmount getAmount();
}
