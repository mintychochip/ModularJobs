package net.aincraft.payment;

import net.kyori.adventure.key.Key;
import org.bukkit.entity.Entity;

public final class MobDamageTracker {

  private final MobDamageTrackerStore store;

  public MobDamageTracker(MobDamageTrackerStore store) {
    this.store = store;
  }

  public DamageContribution endTracking(Entity entity) throws IllegalArgumentException {
    return store.removeContribution(entity);
  }

  public boolean isTracking(Entity entity) {
    return store.hasContribution(entity);
  }
}