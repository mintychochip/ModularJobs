package net.aincraft.payment;

import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.inject.Inject;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

final class EntityValidationServiceImpl implements EntityValidationService {

  @NotNull
  private final Plugin plugin;

  @LazyInit
  private NamespacedKey invalidationKey = null;

  private NamespacedKey invalidationKey() {
    if (invalidationKey == null) {
      invalidationKey = new NamespacedKey(plugin, "invalid");
    }
    return invalidationKey;
  }

  @Inject
  private EntityValidationServiceImpl(@NotNull Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean isValid(Entity entity) {
    PersistentDataContainer pdc = entity.getPersistentDataContainer();
    return !pdc.has(invalidationKey());
  }

  @Override
  public void setValid(Entity entity, boolean state) {
    PersistentDataContainer pdc = entity.getPersistentDataContainer();
    if (state) {
      pdc.remove(invalidationKey());
      return;
    }
    pdc.set(invalidationKey(), PersistentDataType.BOOLEAN, true);
  }
}
