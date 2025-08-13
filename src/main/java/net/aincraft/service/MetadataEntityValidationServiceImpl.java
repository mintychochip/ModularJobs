package net.aincraft.service;

import net.aincraft.api.service.EntityValidationService;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public final class MetadataEntityValidationServiceImpl implements EntityValidationService {

  private static final String TAG = "metadata-entity-validation-service-impl";
  private final Plugin plugin;

  public MetadataEntityValidationServiceImpl(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean isValid(Entity entity) {
    return !entity.hasMetadata(TAG);
  }

  @Override
  public void setValid(Entity entity, boolean state) {
    if (state) {
      entity.removeMetadata(TAG,plugin);
      return;
    }
    entity.setMetadata(TAG,new FixedMetadataValue(plugin,true));
  }
}
