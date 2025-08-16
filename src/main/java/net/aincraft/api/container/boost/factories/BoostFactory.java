package net.aincraft.api.container.boost.factories;

import java.math.BigDecimal;
import net.aincraft.api.Bridge;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostType;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface BoostFactory {

  static BoostFactory boostFactory() {
    return Bridge.bridge().boostFactory();
  }

  Boost additive(BigDecimal amount);

  Boost multiplicative(BigDecimal amount);
}
