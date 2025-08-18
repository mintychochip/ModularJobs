package net.aincraft.payment;

import org.bukkit.entity.Entity;

/**
 * Service for tracking and modifying validation state for entities.
 *
 * <p>Typically used to mark entities spawned or handled by specific systems (e.g. spawners).
 */
interface EntityValidationService {

  /**
   * Checks if the given entity is marked as valid.
   *
   * @param entity the entity to check
   * @return true if valid, false otherwise
   */
  boolean isValid(Entity entity);

  /**
   * Sets the validity state for the given entity.
   *
   * @param entity the entity to modify
   * @param state  true to mark as valid, false to mark as invalid
   */
  void setValid(Entity entity, boolean state);
}
