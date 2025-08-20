package net.aincraft.payment;

import com.google.common.cache.CacheLoader;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarController;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarFormatter;
import net.aincraft.container.PayableHandler;
import net.aincraft.payment.ExploitService.ExploitProtectionType;
import net.aincraft.service.ExploitProtectionStore;
import net.aincraft.service.MobDamageTracker;
import net.aincraft.util.LocationKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class PaymentModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(BoostEngine.class).to(BoostEngineImpl.class).in(Singleton.class);
    bind(ChunkExplorationStore.class).to(ChunkExplorationStoreImpl.class).in(Singleton.class);
    bind(MobDamageTrackerStore.class).to(MobDamageTrackerStoreImpl.class).in(Singleton.class);
    bind(MobDamageTracker.class).to(MobDamageTrackerImpl.class).in(Singleton.class);
    bind(ExperienceBarController.class).to(ExperienceBarControllerImpl.class).in(Singleton.class);
    bind(ExperienceBarFormatter.class).to(ExperienceBarFormatterImpl.class).in(Singleton.class);
    bind(JobsPaymentHandler.class).to(JobsPaymentHandlerImpl.class).in(Singleton.class);
    MapBinder<Key, PayableHandler> handlerMapBinder = MapBinder.newMapBinder(binder(),
        Key.class, PayableHandler.class);
    handlerMapBinder.addBinding(Key.key("modularjobs:experience")).to(
        BufferedExperienceHandlerImpl.class);
    handlerMapBinder.addBinding(Key.key("modularjobs:economy"))
        .to(EconomyPayableHandlerImpl.class);
    Multibinder<Listener> binder = Multibinder.newSetBinder(binder(), Listener.class);
    binder.addBinding().to(MobDamageTrackerController.class);
    binder.addBinding().to(JobPaymentListener.class);
    binder.addBinding().to(MobTagController.class);
  }

  @Provides
  @Singleton
  EntityValidationService validationService(Plugin plugin) {
    return EntityValidationServiceImpl.create(plugin);
  }

  @Provides
  @Singleton
  ExploitService exploitService() {
    Map<Key, ExploitProtectionStore<?>> providers = new HashMap<>();
    providers.put(ExploitProtectionType.WAX.key(),
        new MemoryExploitProtectionStoreImpl<>(
            Map.of(Material.COPPER_BLOCK, Duration.ofSeconds(5)), Block::getType,
            CacheLoader.from(
                block -> new LocationKey(block.getWorld().getName(), block.getX(), block.getY(),
                    block.getZ()))));
    providers.put(ExploitProtectionType.PLACED.key(),
        new MemoryExploitProtectionStoreImpl<>(Map.of(Material.STONE, Duration.ofSeconds(60)),
            Block::getType,
            CacheLoader.from(
                block -> new LocationKey(block.getWorld().getName(), block.getX(), block.getY(),
                    block.getZ()))));
    providers.put(ExploitProtectionType.DYE_ENTITY.key(),
        new MemoryExploitProtectionStoreImpl<>(
            Map.of(EntityType.WOLF, Duration.ofMinutes(5), EntityType.SHEEP, Duration.ofSeconds(5)),
            Entity::getType,
            CacheLoader.from(Entity::getUniqueId)));
    providers.put(ExploitProtectionType.MILK.key(),
        new MemoryExploitProtectionStoreImpl<>(
            Map.of(EntityType.COW, Duration.ofSeconds(5), EntityType.GOAT, Duration.ofSeconds(5)),
            Entity::getType,
            CacheLoader.from(Entity::getUniqueId)));
    return new ExploitServiceImpl(providers);
  }
}
