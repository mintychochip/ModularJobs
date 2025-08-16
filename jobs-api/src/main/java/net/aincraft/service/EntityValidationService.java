package net.aincraft.service;

import net.aincraft.Bridge;
import org.bukkit.entity.Entity;

/**
 * Service for tracking and modifying validation state for entities.
 *
 * <p>Typically used to mark entities spawned or handled by specific systems (e.g. spawners).
 */
public interface EntityValidationService {

  /**
   * Returns the global {@link EntityValidationService} instance from the {@link Bridge}.
   */
  static EntityValidationService entityValidationService() {
    return Bridge.bridge().spawnerService();
  }

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
