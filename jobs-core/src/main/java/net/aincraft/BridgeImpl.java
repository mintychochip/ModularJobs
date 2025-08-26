package net.aincraft;

import com.google.inject.Inject;
import java.util.Optional;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.container.boost.factories.PolicyFactory;
import net.aincraft.registry.RegistryContainer;
import org.bukkit.plugin.Plugin;

record BridgeImpl(Plugin plugin,
                  RegistryContainer registryContainer) implements Bridge {

  @Inject
  BridgeImpl {
  }

  @Override
  public ConditionFactory conditionFactory() {
    return null;
  }

  @Override
  public BoostFactory boostFactory() {
    return null;
  }

  @Override
  public PolicyFactory policyFactory() {
    return null;
  }

  @Override
  public TimedBoostDataService timedBoostDataService() {
    return null;
  }

  @Override
  public Optional<EconomyProvider> economy() {
    return Optional.empty();
  }
}
