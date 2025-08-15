package net.aincraft.api.service;

import java.util.Collection;
import java.util.function.Supplier;
import net.aincraft.api.Bridge;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Tracks damage contributions to specific mobs, typically for reward or scaling purposes.
 */
public interface MobDamageTracker {

  /**
   * Returns the global {@link MobDamageTracker} instance via the {@link Bridge}.
   */
  static MobDamageTracker mobDamageTracker() {
    return Bridge.bridge().mobDamageTracker();
  }

  /**
   * Registers a mob type (by {@link Key}) to be tracked when spawned.
   *
   * @param key the mob's type key (e.g. "minecraft:zombie")
   */
  void registerEntity(Key key);

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
   * Underlying data store for tracking which entity types are tracked and holding contributions.
   */
  interface MobDamageTrackerStore {

    /**
     * Registers a mob type to be tracked.
     *
     * @param key the type key (e.g. "minecraft:skeleton")
     */
    void registerTrackableEntity(Key key);

    /**
     * Checks if the given entity is eligible for tracking.
     *
     * @param entity the entity to check
     * @return true if trackable
     */
    boolean isTrackable(Entity entity);

    /**
     * Gets or creates the damage contribution record for an entity.
     *
     * @param entity the tracked entity
     * @param contributionSupplier fallback supplier if no record exists
     * @return the current or newly created {@link DamageContribution}
     */
    DamageContribution getContribution(Entity entity, Supplier<DamageContribution> contributionSupplier);

    /**
     * Removes and returns the damage contribution for the given entity.
     *
     * @param entity the entity to remove
     * @return the removed contribution, or null if none existed
     */
    DamageContribution removeContribution(Entity entity);

    /**
     * Checks if the entity has an active contribution record.
     *
     * @param entity the entity
     * @return true if a contribution record exists
     */
    boolean hasContribution(Entity entity);
  }

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
