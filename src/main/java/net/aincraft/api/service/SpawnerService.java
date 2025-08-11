package net.aincraft.api.service;

import net.aincraft.api.Bridge;
import org.bukkit.entity.Entity;

public interface SpawnerService {

  static SpawnerService spawnerService() {
    return Bridge.bridge().spawnerService();
  }

  boolean isSpawnerEntity(Entity entity);

  void setSpawnerEntity(Entity entity, boolean state);
}
