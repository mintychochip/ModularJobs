package dev.mintychochip.service;

import java.util.function.Consumer;
import dev.mintychochip.config.LevelUpCommandsConfig;
import dev.mintychochip.config.LevelUpCommandsConfig.LevelUpCommand;
import org.jetbrains.annotations.NotNull;

/**
 * Executes configured level-up commands, substituting {@code {player}}, {@code {level}},
 * and {@code {job}} placeholders.
 *
 * <p>Commands whose min-level exceeds the new level are skipped.</p>
 */
public final class LevelUpCommandExecutor {

  private final LevelUpCommandsConfig config;
  private final Consumer<String> dispatcher;

  /**
   * Creates an executor for configured level-up commands.
   *
   * @param config     configured level-up command list
   * @param dispatcher receives the final command string (typically console dispatch)
   */
  public LevelUpCommandExecutor(
      @NotNull LevelUpCommandsConfig config,
      @NotNull Consumer<String> dispatcher) {
    this.config = config;
    this.dispatcher = dispatcher;
  }

  /** Runs every configured command whose min-level is satisfied for the new level. */
  public void execute(@NotNull String playerName, @NotNull String jobName, int newLevel) {
    for (LevelUpCommand c : config.commands()) {
      if (newLevel < c.minLevel()) {
        continue;
      }
      String command = c.command()
          .replace("{player}", playerName)
          .replace("{level}", Integer.toString(newLevel))
          .replace("{job}", jobName);
      dispatcher.accept(command);
    }
  }
}
