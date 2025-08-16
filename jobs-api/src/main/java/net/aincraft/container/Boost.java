package net.aincraft.container;

import java.math.BigDecimal;
import net.aincraft.Bridge;
import net.aincraft.container.boost.factories.BoostFactory;

public interface Boost {

  BoostFactory FACTORY = Bridge.bridge().boostFactory();

  BigDecimal boost(BigDecimal amount);

  static Boost multiplicative(BoostType type, BigDecimal amount) {
    return FACTORY.multiplicative(amount);
  }

  static Boost additive(BoostType type, BigDecimal amount) {
    return FACTORY.additive(amount);
  }

}
