package net.aincraft.bridge;

import java.util.Optional;
import java.util.UUID;
import net.aincraft.api.container.BoostSource;
import net.aincraft.api.container.Provider;
import net.aincraft.economy.EconomyProvider;
import org.bukkit.block.Block;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
interface BridgeDependencyResolver {

  @NotNull
  Optional<EconomyProvider> getEconomyProvider();

  Optional<Provider<Block, UUID>> getBlockOwnershipProvider();

  Optional<BoostSource> getMcMMOBoostSource();
}
