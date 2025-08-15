package net.aincraft.api.container;

import java.math.BigDecimal;

public interface Boost {

  BoostType type();

  BigDecimal apply(BigDecimal amount);

  static Boost multiplicative(BoostType type, BigDecimal amount) {
    return new MultiplicativeBoostImpl(type,amount);
  }

  static Boost additive(BoostType type, BigDecimal amount) {
    return new AdditiveBoostImpl(type,amount);
  }

}
