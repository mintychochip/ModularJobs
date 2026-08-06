package net.aincraft.container.boost.factories;

import java.math.BigDecimal;
import net.aincraft.container.Boost;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface BoostFactory {

  Boost additive(BigDecimal amount);

  Boost multiplicative(BigDecimal amount);
}
