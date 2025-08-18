package net.aincraft.payment;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

final class EntityValidationServiceImpl implements EntityValidationService {

  private static final String INVALIDATION_VALUE = "invalid";

  private final NamespacedKey invalidationKey;

  private EntityValidationServiceImpl(NamespacedKey invalidationKey) {
    this.invalidationKey = invalidationKey;
  }

  public static EntityValidationService create(Plugin plugin) {
    return new EntityValidationServiceImpl(new NamespacedKey(plugin, INVALIDATION_VALUE));
  }

  @Override
  public boolean isValid(Entity entity) {
    PersistentDataContainer pdc = entity.getPersistentDataContainer();
    return !pdc.has(invalidationKey);
  }

  @Override
  public void setValid(Entity entity, boolean state) {
    PersistentDataContainer pdc = entity.getPersistentDataContainer();
    if (state) {
      pdc.remove(invalidationKey);
      return;
    }
    pdc.set(invalidationKey, PersistentDataType.BOOLEAN,true);
  }
}
