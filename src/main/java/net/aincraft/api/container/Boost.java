package net.aincraft.api.container;

import java.math.BigDecimal;
import net.aincraft.api.Bridge;
import net.aincraft.api.container.boost.factories.BoostFactory;

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
