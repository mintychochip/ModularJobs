package dev.mintychochip.payment;

import com.google.errorprone.annotations.concurrent.LazyInit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Stores an invalidation marker on entities using persistent data. */
public final class EntityValidationService {

  @NotNull private final Plugin plugin;

  @LazyInit private NamespacedKey invalidationKey = null;

  /** Lazily creates the plugin-scoped key used for invalidation markers. */
  private NamespacedKey invalidationKey() {
    if (invalidationKey == null) {
      invalidationKey = new NamespacedKey(plugin, "invalid");
    }
    return invalidationKey;
  }

  /** Creates a validator backed by the supplied plugin namespace. */
  public EntityValidationService(@NotNull Plugin plugin) {
    this.plugin = plugin;
  }

  /** Returns whether the entity has not been explicitly invalidated. */
  public boolean isValid(Entity entity) {
    PersistentDataContainer pdc = entity.getPersistentDataContainer();
    return !pdc.has(invalidationKey());
  }

  /** Marks an entity valid or invalid by adding or removing its marker. */
  public void setValid(Entity entity, boolean valid) {
    PersistentDataContainer pdc = entity.getPersistentDataContainer();
    if (valid) {
      pdc.remove(invalidationKey());
      return;
    }
    pdc.set(invalidationKey(), PersistentDataType.BOOLEAN, true);
  }
}
