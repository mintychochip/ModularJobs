package net.aincraft;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.service.JobService;
import org.bukkit.plugin.Plugin;

record BridgeImpl(
    Plugin plugin,
    RegistryContainer registryContainer,
    JobService jobService,
    @Nullable EconomyProvider economyProvider,
    ConditionFactory conditionFactory,
    BoostFactory boostFactory,
    TimedBoostDataService timedBoostDataService) implements Bridge {

  @Override
  public Optional<EconomyProvider> economy() {
    return Optional.ofNullable(economyProvider);
  }
}
