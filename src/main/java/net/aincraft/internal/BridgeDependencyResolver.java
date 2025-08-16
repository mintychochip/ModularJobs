package net.aincraft.internal;

import java.util.Optional;
import net.aincraft.api.container.BoostSource;
import net.aincraft.api.service.BlockOwnershipService.BlockProtectionAdapter;
import net.aincraft.economy.EconomyProvider;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
interface BridgeDependencyResolver {

  @NotNull
  Optional<EconomyProvider> getEconomyProvider();

  Optional<BlockProtectionAdapter> getBlockProtectionAdapter();

  Optional<BoostSource> getMcMMOBoostSource();
}
