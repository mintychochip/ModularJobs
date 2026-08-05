package net.aincraft.payment;

import net.kyori.adventure.key.Key;
import org.bukkit.entity.Entity;

final class MobDamageTrackerImpl implements MobDamageTracker {

  private final MobDamageTrackerStore store;

  public MobDamageTrackerImpl(MobDamageTrackerStore store) {
    this.store = store;
  }

  @Override
  public DamageContribution endTracking(Entity entity) throws IllegalArgumentException {
    return store.removeContribution(entity);
  }

  @Override
  public boolean isTracking(Entity entity) {
    return store.hasContribution(entity);
  }
}