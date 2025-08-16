package net.aincraft.listener.util;

import net.aincraft.service.EntityValidationService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityTransformEvent;

/**
 * Listener responsible for tracking entities spawned or transformed by spawner-related mechanics
 * (e.g., natural spawners or spawn eggs).
 * <p>
 * It tags these entities using the {@link EntityValidationService}, so that future plugin logic can
 * identify them as "spawner" entities — useful for applying special behavior or filtering.
 * </p>
 *
 * <p>
 * Specifically:
 * <ul>
 *   <li>Marks entities spawned by {@link SpawnReason#SPAWNER_EGG} or {@link SpawnReason#SPAWNER}</li>
 *   <li>Propagates the "spawner" tag to transformed entities when appropriate</li>
 * </ul>
 * </p>
 *
 * @author mintychochip
 */
public class MobTagController implements Listener {

  /**
   * Handles entity spawn events and marks any entity spawned from a spawner or a spawn egg as a
   * "spawner entity" using {@link EntityValidationService}.
   *
   * @param event the {@link CreatureSpawnEvent} representing the spawn
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onSpawn(final CreatureSpawnEvent event) {
    switch (event.getSpawnReason()) {
      case BREEDING:
      case SPAWNER:
      case SPAWNER_EGG:
      case BUCKET:
        EntityValidationService.entityValidationService()
            .setValid(event.getEntity(),false);
    }
  }

  /**
   * Handles entity transformations and ensures the "spawner entity" tag is retained on the
   * resulting entity/entities if the original entity was tagged.
   *
   * <p>
   * Only specific transformation types are eligible for propagation:
   * <ul>
   *   <li>{@code DROWNED} — if from a {@code ZOMBIE}</li>
   *   <li>{@code FROZEN} — if from a {@code SKELETON}</li>
   *   <li>{@code SPLIT} — if from a {@code SLIME} or {@code MAGMA_CUBE}</li>
   * </ul>
   * Other transformation reasons are ignored.
   * </p>
   *
   * @param event the {@link EntityTransformEvent} representing the transformation
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onEntityTransform(final EntityTransformEvent event) {
    EntityValidationService entityValidationService = EntityValidationService.entityValidationService();

    if (!entityValidationService.isValid(event.getEntity())) {
      return;
    }

    Entity transformedEntity = event.getTransformedEntity();
    EntityType type = event.getEntityType();

    switch (event.getTransformReason()) {
      case DROWNED:
        if (type != EntityType.ZOMBIE) {
          return;
        }
        entityValidationService.setValid(transformedEntity,false);
        break;
      case FROZEN:
        if (type != EntityType.SKELETON) {
          return;
        }
        entityValidationService.setValid(transformedEntity,false);
        break;
      case SPLIT:
        if (!(type == EntityType.SLIME || type == EntityType.MAGMA_CUBE)) {
          return;
        }
        for (Entity entity : event.getTransformedEntities()) {
          entityValidationService.setValid(entity,false);
        }
        break;
      case CURED:
      case INFECTION:
      case LIGHTNING:
      case PIGLIN_ZOMBIFIED:
      case SHEARED:
      case UNKNOWN:
      default:
        break;
    }
  }
}
