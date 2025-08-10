package net.aincraft.config;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface ConfigurationFactory {
  YamlConfiguration yaml(Plugin plugin, String path);
}
