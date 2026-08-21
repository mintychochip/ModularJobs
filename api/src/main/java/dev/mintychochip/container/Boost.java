package dev.mintychochip.container;

import java.math.BigDecimal;
import dev.mintychochip.Bridge;
import dev.mintychochip.container.boost.factories.BoostFactory;

public interface Boost {

  BigDecimal boost(BigDecimal amount);

  /**
   * Lazy factory access — avoids class-init dependency on Bridge/Bukkit.
   */
  static BoostFactory factory() {
    return Bridge.bridge().boostFactory();
  }
}
