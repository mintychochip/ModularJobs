package net.aincraft.payment;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.aincraft.container.Context.EntityContext;
import net.aincraft.util.KeyResolver;
import net.aincraft.service.MobDamageTracker.DamageContribution;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Entity;

final class MobDamageTrackerStoreImpl implements MobDamageTrackerStore {

  private final Set<Key> trackableEntities = new HashSet<>();
  private final KeyResolver keyResolver;
  //TODO: add configuration
  private final Map<UUID, DamageContribution> damageContributions = new HashMap<>();

  @Inject
  public MobDamageTrackerStoreImpl(KeyResolver keyResolver) {
    this.keyResolver = keyResolver;
    trackableEntities.add(Key.key("minecraft", "warden"));
    trackableEntities.add(Key.key("minecraft", "ender_dragon"));
    trackableEntities.add(Key.key("minecraft", "wither"));
    trackableEntities.add(Key.key("minecraft", "creeper"));
  }

  @Override
  public void registerTrackableEntity(Key key) {
    trackableEntities.add(key);
  }

  @Override
  public boolean isTrackable(Entity entity) {
    Key resolved = keyResolver.resolve(new EntityContext(entity));
    return trackableEntities.contains(resolved);
  }

  @Override
  public DamageContribution getContribution(Entity entity, Supplier<DamageContribution> contributionSupplier) {
    return damageContributions.computeIfAbsent(entity.getUniqueId(),ignored -> contributionSupplier.get());
  }

  @Override
  public DamageContribution removeContribution(Entity entity) {
    return damageContributions.remove(entity.getUniqueId());
  }

  @Override
  public boolean hasContribution(Entity entity) {
    return damageContributions.containsKey(entity.getUniqueId());
  }
}
