package net.aincraft.listener;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import java.util.List;
import net.aincraft.hooks.JobPetsHook;
import net.aincraft.repository.ConnectionSource;
import net.aincraft.service.PetUpgradeService;
import net.aincraft.service.PetUpgradeServiceImpl;
import org.bukkit.event.Listener;

public final class ListenerModule extends AbstractModule {

  @Override
  protected void configure() {
    // Bind services
    bind(JobPetsHook.class).in(Singleton.class);

    // Register listeners
    Multibinder<Listener> binder = Multibinder.newSetBinder(binder(), Listener.class);
    binder.addBinding().to(JobLevelUpListener.class);
    binder.addBinding().to(PetChangeListener.class);
    binder.addBinding().to(PlayerLoginListener.class);
  }

  @Provides
  @Singleton
  PetUpgradeService providePetUpgradeService(ConnectionSource connectionSource) {
    PetUpgradeServiceImpl service = new PetUpgradeServiceImpl(connectionSource);
    // Register mining pets (excluding silverfish which is the default)
    service.registerJobPets("modularjobs:miner", List.of(
        "allay",
        "bat",
        "goat",
        "copper_golem"
    ));
    return service;
  }
}
