package net.aincraft.api.container;

import java.util.Optional;
import java.util.function.Predicate;
import net.aincraft.api.container.BoostCondition.BoostContext;
import net.kyori.adventure.key.Keyed;

public interface BoostSource extends Keyed {
  interface Rule {
    Predicate<BoostContext> getCondition();
    int getPriority();
    Boost getBoost();
  }
  Optional<Boost> getBoost(BoostContext context);
}
