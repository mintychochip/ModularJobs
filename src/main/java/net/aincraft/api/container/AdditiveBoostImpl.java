package net.aincraft.api.container;

import java.math.BigDecimal;

record AdditiveBoostImpl(BoostType type, BigDecimal amount) implements Boost {

  @Override
  public BigDecimal apply(BigDecimal amount) {
    return amount.add(this.amount);
  }
}
