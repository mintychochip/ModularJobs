package net.aincraft.payment;

import com.google.inject.Inject;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityTransformEvent;

/**
 * Responsible for tracking entities spawned or transformed by spawner-related mechanics
 * (e.g., natural spawners or spawn eggs).
 * <p>
 * It tags these entities using the {@link EntityValidationService}, so that future plugin logic can
 * identify them as "spawner" entities — useful for applying special behavior or filtering.
 * </p>
 * @author mintychochip
 */
final class MobTagController implements Listener {

  private final EntityValidationService entityValidationService;

  @Inject
  MobTagController(EntityValidationService entityValidationService) {
    this.entityValidationService = entityValidationService;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onSpawn(final CreatureSpawnEvent event) {
    switch (event.getSpawnReason()) {
      case BREEDING:
      case SPAWNER:
      case SPAWNER_EGG:
      case BUCKET:
        entityValidationService
            .setValid(event.getEntity(), false);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onEntityTransform(final EntityTransformEvent event) {
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
        entityValidationService.setValid(transformedEntity, false);
        break;
      case FROZEN:
        if (type != EntityType.SKELETON) {
          return;
        }
        entityValidationService.setValid(transformedEntity, false);
        break;
      case SPLIT:
        if (!(type == EntityType.SLIME || type == EntityType.MAGMA_CUBE)) {
          return;
        }
        for (Entity entity : event.getTransformedEntities()) {
          entityValidationService.setValid(entity, false);
        }
        break;
      default:
        break;
    }
  }
}
