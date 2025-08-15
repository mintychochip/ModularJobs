package net.aincraft.api.container;

import java.math.BigDecimal;

record MultiplicativeBoostImpl(BoostType type, BigDecimal amount) implements Boost {

  @Override
  public BigDecimal apply(BigDecimal amount) {
    return amount.multiply(amount);
  }
}
