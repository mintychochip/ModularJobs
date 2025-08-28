package net.aincraft.payment;

import java.util.Collection;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Tracks damage contributions to specific mobs, typically for reward or scaling purposes.
 */
interface MobDamageTracker {

  /**
   * Ends tracking for an entity and returns the final damage contribution record.
   *
   * @param entity the tracked entity
   * @return the final {@link DamageContribution}
   * @throws IllegalArgumentException if the entity was not tracked
   */
  DamageContribution endTracking(Entity entity) throws IllegalArgumentException;

  /**
   * Checks if the entity is currently being tracked.
   *
   * @param entity the entity to check
   * @return true if tracked, false otherwise
   */
  boolean isTracking(Entity entity);



  /**
   * Represents damage contribution data for an entity.
   */
  interface DamageContribution {

    /**
     * Returns the damage contributed by the given entity.
     *
     * @param entity the contributing entity
     * @param normalized whether to normalize the damage (e.g. to 0–1)
     * @return the raw or normalized damage contribution
     */
    double getContribution(Entity entity, boolean normalized);

    /**
     * Adds a damage expected for the given contributor.
     *
     * @param entity the contributor
     * @param damage the amount of damage dealt
     */
    void addContribution(Entity entity, double damage);

    /**
     * Returns all entities that have contributed damage.
     *
     * @return the set of contributors
     */
    @NotNull Collection<@NotNull Entity> getContributors();
  }
}
