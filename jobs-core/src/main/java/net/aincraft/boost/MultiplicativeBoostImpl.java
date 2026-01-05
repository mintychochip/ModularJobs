package net.aincraft.boost;

import java.math.BigDecimal;
import net.aincraft.container.Boost;

public record MultiplicativeBoostImpl(BigDecimal amount) implements Boost {

  @Override
  public BigDecimal boost(BigDecimal amount) {
    return amount.multiply(this.amount);
  }

}
