package net.aincraft.container;

import java.math.BigDecimal;
import net.aincraft.Bridge;
import net.aincraft.container.boost.factories.BoostFactory;

public interface Boost {

  BigDecimal boost(BigDecimal amount);

  /**
   * Lazy factory access — avoids class-init dependency on Bridge/Bukkit.
   */
  static BoostFactory factory() {
    return Bridge.bridge().boostFactory();
  }
}
