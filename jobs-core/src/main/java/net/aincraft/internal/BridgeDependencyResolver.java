package net.aincraft.internal;

import java.util.Optional;
import net.aincraft.container.BoostSource;
import net.aincraft.container.EconomyProvider;
import net.aincraft.service.BlockOwnershipService.BlockProtectionAdapter;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
interface BridgeDependencyResolver {

  @NotNull
  Optional<EconomyProvider> getEconomyProvider();

  Optional<BlockProtectionAdapter> getBlockProtectionAdapter();

  Optional<BoostSource> getMcMMOBoostSource();
}
