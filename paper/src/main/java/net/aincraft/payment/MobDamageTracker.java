package net.aincraft.payment;

import org.bukkit.entity.Entity;

/**
 * Facade over {@link MobDamageTrackerStore} for tracking damage dealt to an entity and
 * retrieving the final contribution breakdown when tracking ends.
 */
public final class MobDamageTracker {

  private final MobDamageTrackerStore store;

  /** Creates a tracker backed by the given contribution store. */
  public MobDamageTracker(MobDamageTrackerStore store) {
    this.store = store;
  }

  /**
   * Stops tracking {@code entity} and returns its accumulated {@link DamageContribution}.
   */
  public DamageContribution endTracking(Entity entity) {
    return store.removeContribution(entity);
  }

  /**
   * Reports whether damage on {@code entity} is currently being tracked.
   *
   * @return true when damage on {@code entity} is currently being tracked
   */
  public boolean isTracking(Entity entity) {
    return store.hasContribution(entity);
  }
}
