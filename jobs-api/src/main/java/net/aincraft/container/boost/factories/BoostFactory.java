package net.aincraft.container.boost.factories;

import java.math.BigDecimal;
import net.aincraft.Bridge;
import net.aincraft.container.Boost;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface BoostFactory {

  static BoostFactory boostFactory() {
    return Bridge.bridge().boostFactory();
  }

  Boost additive(BigDecimal amount);

  Boost multiplicative(BigDecimal amount);
}
