package net.aincraft.util;

import com.google.inject.Inject;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

final class KeyFactoryImpl implements KeyFactory {

  private final Plugin plugin;

  @Inject
  KeyFactoryImpl(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public Key create(String raw) throws IllegalArgumentException {
    String[] split = raw.split(":");
    int length = split.length;
    if (!(length == 2 || length == 1)) {
      throw new IllegalArgumentException("failed to create a jobKey, the format was incorrect");
    }
    return length == 2 ? NamespacedKey.fromString(raw) : new NamespacedKey(plugin, split[0]);
  }
}
