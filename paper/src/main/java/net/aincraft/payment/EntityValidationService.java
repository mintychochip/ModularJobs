package net.aincraft.payment;

import com.google.errorprone.annotations.concurrent.LazyInit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class EntityValidationService {

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

  public EntityValidationService(@NotNull Plugin plugin) {
    this.plugin = plugin;
  }

  public boolean isValid(Entity entity) {
    PersistentDataContainer pdc = entity.getPersistentDataContainer();
    return !pdc.has(invalidationKey());
  }

  public void setValid(Entity entity, boolean valid) {
    PersistentDataContainer pdc = entity.getPersistentDataContainer();
    if (valid) {
      pdc.remove(invalidationKey());
      return;
    }
    pdc.set(invalidationKey(), PersistentDataType.BOOLEAN, true);
  }
}
