package net.aincraft.boost;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import net.aincraft.boost.config.BoostSourceLoader;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.registry.Registry;
import net.aincraft.registry.SimpleRegistryImpl;
import org.bukkit.plugin.Plugin;

/**
 * Guice module for boost source registry and loading.
 */
public final class BoostSourceModule extends AbstractModule {

  @Override
  protected void configure() {
    // Nothing to bind here, everything is provided
  }

  @Provides
  @Singleton
  public Registry<BoostSource> boostSourceRegistry() {
    return new SimpleRegistryImpl<>();
  }

  @Provides
  @Singleton
  public BoostSourceLoader boostSourceLoader(
      Plugin plugin,
      Gson gson,
      ConditionFactory conditionFactory,
      BoostFactory boostFactory,
      Registry<BoostSource> boostSourceRegistry
  ) {
    BoostSourceLoader loader = new BoostSourceLoader(
        plugin,
        gson,
        conditionFactory,
        boostFactory,
        boostSourceRegistry
    );

    // Load boost sources on creation
    loader.load();

    return loader;
  }
}
