package net.aincraft.service;

import net.aincraft.api.Bridge;
import net.aincraft.api.service.SpawnerService;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;

public final class SpawnerServiceImpl implements SpawnerService {

  private static final String SPAWNER_MOB_KEY = "modular-jobs-spawner";

  @Override
  public boolean isSpawnerEntity(Entity entity) {
    return entity.hasMetadata(SPAWNER_MOB_KEY);
  }

  @Override
  public void setSpawnerEntity(Entity entity, boolean state) {
    if (state) {
      entity.setMetadata(SPAWNER_MOB_KEY, new FixedMetadataValue(Bridge.bridge().plugin(), true));
      return;
    }
    entity.removeMetadata(SPAWNER_MOB_KEY, Bridge.bridge().plugin());
  }
}
