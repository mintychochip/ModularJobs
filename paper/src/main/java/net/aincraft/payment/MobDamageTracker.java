package net.aincraft.payment;

import net.kyori.adventure.key.Key;
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
   *
   * @throws IllegalArgumentException when no tracking exists for the entity
   */
  public DamageContribution endTracking(Entity entity) throws IllegalArgumentException {
    return store.removeContribution(entity);
  }

  /** @return true when damage on {@code entity} is currently being tracked */
  public boolean isTracking(Entity entity) {
    return store.hasContribution(entity);
  }
}