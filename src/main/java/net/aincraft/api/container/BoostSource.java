package net.aincraft.api.container;

import java.util.Optional;
import net.aincraft.api.container.BoostCondition.BoostContext;
import net.kyori.adventure.key.Keyed;

public interface BoostSource extends Keyed {
  Optional<Boost> getBoost(BoostContext context);
}
