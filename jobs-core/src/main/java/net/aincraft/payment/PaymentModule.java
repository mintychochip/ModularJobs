package net.aincraft.payment;

import com.google.common.cache.CacheLoader;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jdk.jfr.Name;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.payment.ExploitService.ExploitProtectionType;
import net.aincraft.service.ExploitProtectionStore;
import net.aincraft.util.LocationKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class PaymentModule extends AbstractModule {

  private static final String TRACKABLE_ENTITIES = "trackable-entities";

  @Override
  protected void configure() {
    bind(BoostEngine.class).to(BoostEngineImpl.class).in(Singleton.class);
    bind(ChunkExplorationStore.class).to(ChunkExplorationStoreImpl.class).in(Singleton.class);
    bind(MobDamageTrackerStore.class).to(MobDamageTrackerStoreImpl.class).in(Singleton.class);
    bind(EntityValidationService.class).to(EntityValidationServiceImpl.class).in(Singleton.class);
    bind(MobDamageTracker.class).to(MobDamageTrackerImpl.class).in(Singleton.class);
    bind(JobsPaymentHandler.class).to(JobsPaymentHandlerImpl.class).in(Singleton.class);
    Multibinder<Listener> binder = Multibinder.newSetBinder(binder(), Listener.class);
    binder.addBinding().to(MobDamageTrackerController.class);
    binder.addBinding().to(JobPaymentListener.class);
    binder.addBinding().to(MobTagController.class);
    binder.addBinding().to(ExploitStoreController.class);
    binder.addBinding().to(JobLevelUpListener.class);
  }

//  @Provides
//  @Singleton
//  @Named(TRACKABLE_ENTITIES)
//  Set<Key> trackableEntities(@Named(TRACKABLE_ENTITIES) final ConfigurationSection configuration) {
//    if (!configuration.contains(TRACKABLE_ENTITIES)) {
//      //TODO: add logging message, with plugin logger
//      return Set.of();
//    }
//    return configuration.getStringList(TRACKABLE_ENTITIES).stream().map(NamespacedKey::fromString)
//        .collect(
//            Collectors.toSet());
//  }

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
