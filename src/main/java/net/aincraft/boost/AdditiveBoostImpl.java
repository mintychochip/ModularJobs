package net.aincraft.boost;

import java.math.BigDecimal;
import net.aincraft.api.container.Boost;

record AdditiveBoostImpl(BigDecimal amount) implements Boost {

  @Override
  public BigDecimal boost(BigDecimal amount) {
    return amount.add(this.amount);
  }
}
