package net.aincraft.payment;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.aincraft.container.Context.EntityContext;
import net.aincraft.payment.MobDamageTracker.DamageContribution;
import net.aincraft.util.KeyResolver;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Entity;

final class MobDamageTrackerStoreImpl implements MobDamageTrackerStore {

  private final Set<Key> trackableEntities;
  private final KeyResolver keyResolver;
  private final Map<UUID, DamageContribution> damageContributions = new HashMap<>();

  @Inject
  public MobDamageTrackerStoreImpl(Set<Key> trackableEntities, KeyResolver keyResolver) {
    this.trackableEntities = trackableEntities;
    this.keyResolver = keyResolver;
  }

  @Override
  public boolean isTrackable(Entity entity) {
    Key resolved = keyResolver.resolve(new EntityContext(entity));
    return trackableEntities.contains(resolved);
  }

  @Override
  public DamageContribution getContribution(Entity entity,
      Supplier<DamageContribution> contributionSupplier) {
    return damageContributions.computeIfAbsent(entity.getUniqueId(),
        __ -> contributionSupplier.get());
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
