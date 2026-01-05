package net.aincraft.util;

import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class KeyUtils {

  private KeyUtils() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Parses a key string into a Key instance.
   * Supports both "namespace:value" and "value" formats.
   * If no namespace is provided, uses the plugin's namespace.
   */
  public static @NotNull Key parseKey(@NotNull Plugin plugin, @NotNull String raw) {
    String[] split = raw.split(":");
    int length = split.length;
    if (!(length == 2 || length == 1)) {
      throw new IllegalArgumentException("Invalid key format: " + raw);
    }
    return length == 2 ? NamespacedKey.fromString(raw) : new NamespacedKey(plugin, split[0]);
  }
}
