package dev.mintychochip.container;

import dev.mintychochip.Bridge;
import dev.mintychochip.container.boost.factories.BoostFactory;
import java.math.BigDecimal;

/** Boost. */
@FunctionalInterface
public interface Boost {

  /** Boost. */
  BigDecimal boost(BigDecimal amount);

  /** Lazy factory access — avoids class-init dependency on Bridge/Bukkit. */
  static BoostFactory factory() {
    return Bridge.bridge().boostFactory();
  }
}
