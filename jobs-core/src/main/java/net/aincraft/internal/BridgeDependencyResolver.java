package net.aincraft.internal;

import java.util.Optional;
import net.aincraft.container.BoostSource;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
interface BridgeDependencyResolver {
  Optional<BoostSource> getMcMMOBoostSource();
}
