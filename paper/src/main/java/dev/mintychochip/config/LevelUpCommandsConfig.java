package dev.mintychochip.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Level-up commands configured in {@code config.yml} under {@code level-up-commands}.
 *
 * <p>Each entry is {@code {command: "...", min-level: N}}. Commands run from the console
 * when a player reaches a level at or above {@code min-level}.
 */
public record LevelUpCommandsConfig(@NotNull List<LevelUpCommand> commands) {

  /** A single configured console command with its minimum triggering level. */
  public record LevelUpCommand(@NotNull String command, int minLevel) {}

  /** No level-up commands configured. */
  public static LevelUpCommandsConfig defaults() {
    return new LevelUpCommandsConfig(List.of());
  }

  /** Parses from a raw map (used by tests and {@link #fromPlugin}). */
  public static LevelUpCommandsConfig fromMap(@NotNull Map<?, ?> source) {
    Object raw = source.get("level-up-commands");
    if (!(raw instanceof List<?> list)) {
      return defaults();
    }
    List<LevelUpCommand> commands = new ArrayList<>();
    for (Object item : list) {
      if (!(item instanceof Map<?, ?> entry)) {
        continue;
      }
      Object cmd = entry.get("command");
      if (!(cmd instanceof String command) || command.isBlank()) {
        continue;
      }
      int minLevel = 1;
      Object rawMin = entry.get("min-level");
      if (rawMin instanceof Number n) {
        minLevel = n.intValue();
      }
      commands.add(new LevelUpCommand(command, Math.max(1, minLevel)));
    }
    return new LevelUpCommandsConfig(List.copyOf(commands));
  }

  /** Loads from {@code config.yml}. */
  public static LevelUpCommandsConfig fromPlugin(@NotNull Plugin plugin) {
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("level-up-commands");
    if (section == null) {
      return defaults();
    }
    return fromMap(section.getValues(false));
  }
}
